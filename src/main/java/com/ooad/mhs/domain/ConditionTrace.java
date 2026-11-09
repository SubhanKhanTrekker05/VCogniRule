package com.ooad.mhs.domain;

import com.ooad.mhs.util.Operator;

/**
 * Represents one matched condition in a Finding's trace.
 * 
 * The trace is the "explanation facility" of the expert system.
 * When a rule fires, each condition that was checked gets a
 * ConditionTrace showing what was expected vs what was actual.
 * 
 * This is what makes the system transparent — the user can see
 * WHY a rule fired, not just THAT it fired.
 * 
 * Example trace rendered in the report:
 *   • PHQ-9 Total Score: 14 (threshold: ≥ 10)
 *   • Symptom Duration: 21 days (threshold: ≥ 14)
 * 
 * Adopted from the IAI project's trace patterns (VSecretServerAdvisor):
 * - Fired rules show all matched conditions (green styling)
 * - "May have" rules show borderline conditions (yellow styling)
 * - Not-fired rules show first failed condition (gray, collapsible)
 */
public record ConditionTrace(
    String fieldKey,        // e.g., "phq9_total"
    String actualValue,     // e.g., "14" — what the user's responses produced
    String requiredValue,   // e.g., "≥ 10" — what the rule required
    Operator operator,      // GTE, BETWEEN, EQ, etc.
    boolean matched         // true = condition passed, false = condition failed
) {}