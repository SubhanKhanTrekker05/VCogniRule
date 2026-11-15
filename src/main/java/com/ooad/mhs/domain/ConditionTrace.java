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
 * Only rules that FIRED get a trace, and it lists the conditions that matched.
 * ReportService turns each trace line into plain language for the report's
 * "Why this appears" list, for example:
 *   Depression score (PHQ-9 total, 0–27)   Your result: 14   Rule needs: between 10 and 14
 */
public record ConditionTrace(
    String fieldKey,        // e.g., "phq9_total"
    String actualValue,     // e.g., "14" — what the user's responses produced
    String requiredValue,   // e.g., "10 - 14" or ">= 10" — what the rule required (OperatorStrategy.describe)
    Operator operator       // GTE, BETWEEN, EQ, etc.
) {}