package com.ooad.mhs.domain;

import java.util.List;

/**
 * Represents a group of conditions within a rule.
 * 
 * Design:
 * - Conditions WITHIN a group are AND'd (all must match for the group to pass)
 * - Groups BETWEEN each other are OR'd (if ANY group passes, the rule fires)
 * 
 * In our current knowledge base, all 150 rules have exactly 1 group each.
 * The logic column is always 'AND' because conditions within the group
 * are always combined with AND.
 * 
 * OR logic is handled through derived fields (e.g., crisis_detected,
 * cssrs_passive_ideation) computed by DerivedFieldCalculator before
 * rule evaluation — not through multiple groups.
 * 
 * Stored in: rule_condition_group table (150 rows)
 * Conditions loaded from: rule_condition table (205 rows)
 */
public record ConditionGroup(
    Long id,
    Long ruleId,                    // FK to rule
    String logic,                   // always "AND" in our design
    List<Condition> conditions      // the conditions within this group (1-4 per group)
) {}