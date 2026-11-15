package com.ooad.mhs.repository;

import com.ooad.mhs.domain.ConditionDetails;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.domain.RuleDetails;

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
    void save(RuleDetails details);

    /** Changes the details of an existing rule (its rule ID stays the same). */
    void updateRule(Long id, RuleDetails details);

    /** Deletes a rule. */
    void deleteById(Long id);

    /** Switches a rule on or off without deleting it. */
    void toggleActive(Long id, boolean active);

    /** Adds a condition to a rule's (first) condition group. */
    void addCondition(Long ruleId, ConditionDetails condition);

    /** Changes an existing condition. */
    void updateCondition(Long conditionId, ConditionDetails condition);

    /** Deletes a condition. */
    void deleteCondition(Long conditionId);

    /**
     * Factory reset: replaces all rules, groups and conditions with the original knowledge base.
     *
     * @return number of rules after the reset (150)
     */
    int resetKnowledgeBase();
}
