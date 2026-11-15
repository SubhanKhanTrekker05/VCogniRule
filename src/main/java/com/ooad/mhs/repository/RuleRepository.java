package com.ooad.mhs.repository;

import com.ooad.mhs.domain.Rule;

import java.util.List;
import java.util.Optional;

/**
 * Stores and loads the rules of the knowledge base (rules, their condition groups and conditions).
 *
 * Repository pattern + Dependency Inversion: services depend on this interface, not on the
 * database code. The SQLite implementation is {@link JdbcRuleRepository}; another database,
 * or an in-memory fake in tests, could be used without changing any service.
 */
public interface RuleRepository {

    /** The active rules, as used by the rule engine (kept in memory, refreshed after every change). */
    List<Rule> getActiveRules();

    /** Every rule, active or not (admin dashboard). */
    List<Rule> findAllIncludingInactive();

    /** One rule by database id, active or not. */
    Optional<Rule> findById(Long id);

    /** Adds a new, active rule (without conditions). */
    void save(String ruleId, String name, int level, String category, int priority,
              String actionType, String explanation, String sourceRef,
              String conclusionKey, String conclusionValue);

    /** Changes the details of an existing rule. */
    void updateRule(Long id, String name, int level, String category, int priority,
                    String actionType, String explanation, String sourceRef,
                    String conclusionKey, String conclusionValue);

    /** Deletes a rule. */
    void deleteById(Long id);

    /** Switches a rule on or off without deleting it. */
    void toggleActive(Long id, boolean active);

    /** Adds a condition to a rule's (first) condition group. */
    void addCondition(Long ruleId, String fieldKey, String operator,
                      Integer valueInt, String valueText, Integer valueMin, Integer valueMax);

    /** Changes an existing condition. */
    void updateCondition(Long conditionId, String fieldKey, String operator,
                         Integer valueInt, String valueText, Integer valueMin, Integer valueMax);

    /** Deletes a condition. */
    void deleteCondition(Long conditionId);

    /**
     * Factory reset: replaces all rules, groups and conditions with the original knowledge base.
     *
     * @return number of rules after the reset (150)
     */
    int resetKnowledgeBase();
}
