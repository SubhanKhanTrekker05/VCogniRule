package com.ooad.mhs.domain;

import com.ooad.mhs.util.Operator;

/**
 * Represents one condition within a rule condition group.
 * 
 * A condition checks one field against a required value using an operator.
 * Multiple conditions within a group are AND'd together (all must match).
 * 
 * Example: fieldKey="phq9_total", operator=GTE, valueInt=20
 * Means: "Is the PHQ-9 total score >= 20?"
 * 
 * Different operators use different value columns:
 * - EQ, NE, GT, GTE, LT, LTE → use valueInt
 * - BETWEEN → use valueMin + valueMax
 * - IN → use valueText (comma-separated list)
 * 
 * Stored in: rule_condition table (205 rows)
 */
public record Condition(
    Long id,
    Long groupId,           // FK to rule_condition_group
    String fieldKey,        // e.g., "phq9_total", "cssrs_q3"
    Operator operator,      // EQ, GTE, LTE, BETWEEN, IN, etc.
    Integer valueInt,       // for EQ, NE, GT, GTE, LT, LTE
    String valueText,       // for IN operator (comma-separated)
    Integer valueMin,       // for BETWEEN (lower bound, inclusive)
    Integer valueMax        // for BETWEEN (upper bound, inclusive)
) {}