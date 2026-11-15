package com.ooad.mhs.service;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.repository.AdminRepository;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.repository.RuleRepository;
import com.ooad.mhs.util.Category;

import java.util.*;

/**
 * In-memory fake repositories for unit tests.
 *
 * Possible because the services depend on repository INTERFACES (Dependency Inversion):
 * the same service code runs against these fakes instead of SQLite.
 */
final class Fakes {

    private Fakes() {
    }

    /** Questions of PHQ-9 (9), C-SSRS (6) and the 4 typed PSQI questions. */
    static final class FakeQuestionRepository implements QuestionRepository {
        private final List<Question> questions = new ArrayList<>();

        FakeQuestionRepository() {
            long id = 1;
            for (int i = 1; i <= 9; i++) questions.add(q(id++, "phq9_q" + i, "PHQ-9", "LIKERT_03"));
            for (int i = 1; i <= 6; i++) questions.add(q(id++, "cssrs_q" + i, "C-SSRS", "BINARY"));
            questions.add(q(id++, "psqi_q1", "PSQI", "TEXT"));
            questions.add(q(id++, "psqi_q2", "PSQI", "NUMBER_INT"));
            questions.add(q(id++, "psqi_q3", "PSQI", "TEXT"));
            questions.add(q(id++, "psqi_q4", "PSQI", "NUMBER_DEC"));
            questions.add(new Question(id, "phq9_total", "PHQ-9 Total Score", Category.MOOD, 200, "CALCULATED", "NUMBER_INT"));
        }

        private static Question q(long id, String key, String instrument, String scale) {
            return new Question(id, key, "Question " + key, Category.MOOD, (int) id, instrument, scale);
        }

        @Override
        public List<Question> findAll() {
            return questions;
        }

        @Override
        public List<Question> findAllReal() {
            return questions.stream().filter(q -> !"CALCULATED".equals(q.instrument())).toList();
        }
    }

    /** Rules kept in a list; one condition group per rule. */
    static final class FakeRuleRepository implements RuleRepository {
        final List<Rule> rules = new ArrayList<>();
        private long nextId = 100;

        @Override
        public List<Rule> getActiveRules() {
            return rules.stream().filter(Rule::isActive).toList();
        }

        @Override
        public List<Rule> findAllIncludingInactive() {
            return List.copyOf(rules);
        }

        @Override
        public Optional<Rule> findById(Long id) {
            return rules.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        @Override
        public void save(RuleDetails d) {
            long id = nextId++;
            rules.add(new Rule(id, d.ruleId(), d.name(), d.level(), d.category(), d.priority(), d.actionType(),
                d.explanation(), d.sourceRef(), d.conclusionKey(), d.conclusionValue(), true,
                List.of(new ConditionGroup(id, id, "AND", new ArrayList<>()))));
        }

        @Override
        public void updateRule(Long id, RuleDetails d) {
            Rule old = findById(id).orElseThrow();
            rules.set(rules.indexOf(old), new Rule(id, old.ruleId(), d.name(), d.level(), d.category(), d.priority(),
                d.actionType(), d.explanation(), d.sourceRef(), d.conclusionKey(), d.conclusionValue(),
                old.isActive(), old.groups()));
        }

        @Override
        public void deleteById(Long id) {
            rules.removeIf(r -> r.id().equals(id));
        }

        @Override
        public void toggleActive(Long id, boolean active) {
        }

        @Override
        public void addCondition(Long ruleId, ConditionDetails c) {
            Rule r = findById(ruleId).orElseThrow();
            r.groups().get(0).conditions().add(new Condition(nextId++, ruleId, c.fieldKey(), c.operator(),
                c.valueInt(), c.valueText(), c.valueMin(), c.valueMax()));
        }

        @Override
        public void updateCondition(Long conditionId, ConditionDetails c) {
        }

        @Override
        public void deleteCondition(Long conditionId) {
            rules.forEach(r -> r.groups().forEach(g -> g.conditions().removeIf(x -> x.id().equals(conditionId))));
        }

        @Override
        public int resetKnowledgeBase() {
            return rules.size();
        }
    }

    /** Admin accounts in a map: username → BCrypt hash. */
    static final class FakeAdminRepository implements AdminRepository {
        final Map<String, String> admins = new HashMap<>();

        @Override
        public Optional<Admin> findByUsername(String username) {
            return Optional.ofNullable(admins.get(username)).map(h -> new Admin(1L, username, h));
        }

        @Override
        public long count() {
            return admins.size();
        }

        @Override
        public void save(String username, String passwordHash) {
            admins.putIfAbsent(username, passwordHash);
        }
    }
}
