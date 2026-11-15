package com.ooad.mhs.repository;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.util.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQLite (JDBC) implementation of {@link RuleRepository}.
 *
 * Rules are stored in three tables: rule → rule_condition_group → rule_condition.
 * The active rules are assembled into Rule objects once and kept in memory
 * (cachedRules) for the rule engine. After a change, {@link #reloadActiveRules()} is called by
 * the observer RuleCacheRefresher (Observer pattern), not by this class.
 */
@Repository
public class JdbcRuleRepository implements RuleRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcRuleRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private List<Rule> cachedRules;

    private record RawRule(
        Long id, String ruleId, String name, Integer level,
        Category category, Integer priority, ActionType actionType,
        String explanation, String sourceRef, String conclusionKey,
        String conclusionValue, boolean isActive
    ) {}

    private static final RowMapper<RawRule> RULE_MAPPER = (rs, rowNum) -> new RawRule(
        rs.getLong("id"), rs.getString("rule_id"), rs.getString("name"),
        rs.getInt("level"), Category.valueOf(rs.getString("category")),
        rs.getInt("priority"), ActionType.valueOf(rs.getString("action_type")),
        rs.getString("explanation"), rs.getString("source_ref"),
        rs.getString("conclusion_key"), rs.getString("conclusion_value"),
        rs.getInt("is_active") == 1
    );

    private record RawGroup(Long id, Long ruleId, String logic) {}

    private static final RowMapper<RawGroup> GROUP_MAPPER = (rs, rowNum) -> new RawGroup(
        rs.getLong("id"), rs.getLong("rule_id"), rs.getString("logic")
    );

    private static final RowMapper<Condition> CONDITION_MAPPER = (rs, rowNum) -> new Condition(
        rs.getLong("id"), rs.getLong("group_id"), rs.getString("field_key"),
        Operator.valueOf(rs.getString("operator")),
        (Integer) rs.getObject("value_int"), rs.getString("value_text"),
        (Integer) rs.getObject("value_min"), (Integer) rs.getObject("value_max")
    );

    public JdbcRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void loadRulesIntoMemory() {
        cachedRules = loadRulesFromDb(true);
        log.info("Loaded {} active rules", cachedRules.size());
    }

    @Override
    public List<Rule> getActiveRules() { return cachedRules; }

    @Override
    public void reloadActiveRules() {
        cachedRules = loadRulesFromDb(true);
    }

    @Override
    public List<Rule> findAllIncludingInactive() { return loadRulesFromDb(false); }

    /**
     * Finds a rule by its database id, whether it is active or not.
     * (The cache holds only active rules, so searching it made inactive rules
     * impossible to open in the admin panel.)
     */
    @Override
    public Optional<Rule> findById(Long id) {
        return loadRulesFromDb(false).stream().filter(r -> r.id().equals(id)).findFirst();
    }

    // === CRUD ===
    @Override
    public void save(RuleDetails d) {
        jdbcTemplate.update(
            "INSERT INTO rule (rule_id, name, level, category, priority, action_type, explanation, source_ref, conclusion_key, conclusion_value, is_active) VALUES (?,?,?,?,?,?,?,?,?,?,1)",
            d.ruleId(), d.name(), d.level(), d.category().name(), d.priority(), d.actionType().name(),
            d.explanation(), d.sourceRef(), d.conclusionKey(), d.conclusionValue());
    }

    @Override
    public void updateRule(Long id, RuleDetails d) {
        jdbcTemplate.update(
            "UPDATE rule SET name=?, level=?, category=?, priority=?, action_type=?, explanation=?, source_ref=?, conclusion_key=?, conclusion_value=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
            d.name(), d.level(), d.category().name(), d.priority(), d.actionType().name(),
            d.explanation(), d.sourceRef(), d.conclusionKey(), d.conclusionValue(), id);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM rule WHERE id=?", id);
    }

    @Override
    public void toggleActive(Long id, boolean active) {
        jdbcTemplate.update("UPDATE rule SET is_active=? WHERE id=?", active ? 1 : 0, id);
    }

    // === Condition CRUD ===
    private Long getOrCreateGroup(Long ruleId) {
        List<Long> ids = jdbcTemplate.query(
            "SELECT id FROM rule_condition_group WHERE rule_id=? ORDER BY id LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"), ruleId);
        if (!ids.isEmpty()) return ids.get(0);
        jdbcTemplate.update("INSERT INTO rule_condition_group (rule_id, logic) VALUES (?, 'AND')", ruleId);
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    @Override
    public void addCondition(Long ruleId, ConditionDetails c) {
        Long groupId = getOrCreateGroup(ruleId);
        jdbcTemplate.update(
            // OR IGNORE: an identical condition already exists (unique index uq_rule_condition)
            "INSERT OR IGNORE INTO rule_condition (group_id, field_key, operator, value_int, value_text, value_min, value_max) VALUES (?,?,?,?,?,?,?)",
            groupId, c.fieldKey(), c.operator().name(), c.valueInt(), c.valueText(), c.valueMin(), c.valueMax());
    }

    @Override
    public void updateCondition(Long conditionId, ConditionDetails c) {
        jdbcTemplate.update(
            // OR IGNORE: skip an edit that would duplicate another condition of the same rule
            "UPDATE OR IGNORE rule_condition SET field_key=?, operator=?, value_int=?, value_text=?, value_min=?, value_max=? WHERE id=?",
            c.fieldKey(), c.operator().name(), c.valueInt(), c.valueText(), c.valueMin(), c.valueMax(), conditionId);
    }

    @Override
    public void deleteCondition(Long conditionId) {
        jdbcTemplate.update("DELETE FROM rule_condition WHERE id=?", conditionId);
    }
        // === Factory Reset ===
    /**
     * Factory reset: wipes all rules + groups + conditions, re-seeds from rules_master.sql.
     *
     * Order of operations:
     *   1. DELETE rule_condition        (grandchildren - the leaf conditions)
     *   2. DELETE rule_condition_group  (children - the AND groups)
     *   3. DELETE rule                  (parent - the 150 rules themselves)
     *   4. Re-execute rules_master.sql  (re-seeds all 150 rules + 150 groups + 209 conditions)
     *   (the in-memory cache is then reloaded by the observer RuleCacheRefresher)
     *
     * NOTE: SQLite FK enforcement is OFF by default (the JDBC URL has no ?foreign_keys=on
     *       parameter), so the ON DELETE CASCADE declared in schema.sql does NOT fire
     *       automatically. The explicit ordered DELETEs above make the reset work
     *       regardless of the FK pragma state - safer and more portable.
     *
     * This is the "ultimate Ctrl+Z" for viva demos: every CRUD change made via the
     * admin panel (added rules, edited explanations, toggled active flags, deleted
     * rules) is undone in one shot.
     *
     * @return number of rules after reset (should always be 150)
     */
    @Override
    public int resetKnowledgeBase() {
        jdbcTemplate.update("DELETE FROM rule_condition");
        jdbcTemplate.update("DELETE FROM rule_condition_group");
        jdbcTemplate.update("DELETE FROM rule");

        // Run rules_master.sql statement by statement. A single jdbcTemplate.execute(...)
        // of the whole file only ran its FIRST statement on SQLite (the rule INSERT),
        // silently skipping the groups and conditions, so a reset left every rule
        // without conditions.
        ResourceDatabasePopulator populator =
            new ResourceDatabasePopulator(new ClassPathResource("rules_master.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(Objects.requireNonNull(jdbcTemplate.getDataSource()));


        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rule", Integer.class);
        return count != null ? count : 0;
    }

    // === Assembly ===
    private List<Rule> loadRulesFromDb(boolean activeOnly) {
        String sql = activeOnly
            ? "SELECT * FROM rule WHERE is_active = 1 ORDER BY level ASC, priority DESC"
            : "SELECT * FROM rule ORDER BY level ASC, priority DESC";
        List<RawRule> rawRules = jdbcTemplate.query(sql, RULE_MAPPER);
        List<RawGroup> rawGroups = jdbcTemplate.query("SELECT * FROM rule_condition_group ORDER BY id", GROUP_MAPPER);
        List<Condition> conditions = jdbcTemplate.query("SELECT * FROM rule_condition ORDER BY group_id", CONDITION_MAPPER);
        Map<Long, List<Condition>> byGroup = conditions.stream().collect(Collectors.groupingBy(Condition::groupId));
        Map<Long, List<ConditionGroup>> groupsByRule = new HashMap<>();
        for (RawGroup rg : rawGroups) {
            groupsByRule.computeIfAbsent(rg.ruleId(), k -> new ArrayList<>()).add(
                new ConditionGroup(rg.id(), rg.ruleId(), rg.logic(), byGroup.getOrDefault(rg.id(), List.of())));
        }
        return rawRules.stream().map(r -> new Rule(r.id(), r.ruleId(), r.name(), r.level(),
            r.category(), r.priority(), r.actionType(), r.explanation(), r.sourceRef(),
            r.conclusionKey(), r.conclusionValue(), r.isActive(),
            groupsByRule.getOrDefault(r.id(), List.of()))).toList();
    }
}