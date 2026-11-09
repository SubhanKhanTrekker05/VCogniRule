package com.ooad.mhs.domain;

import com.ooad.mhs.util.Category;

/**
 * Represents one screening question stored in the question table.
 * 
 * A Question can be:
 * - A real question shown to the user (instrument = PHQ-9, GAD-7, etc.)
 * - A derived field computed by DerivedFieldCalculator (instrument = CALCULATED)
 * 
 * Real questions appear in the screening form as collapsible sections.
 * Derived fields are internal — computed before rule evaluation, never shown to the user.
 * 
 * Stored in: question table (151 rows = 84 real + 34 derived + 33 conclusion keys)
 */
public record Question(
    Long id,
    String fieldKey,       // e.g., "phq9_q1" — referenced by rule conditions
    String text,           // e.g., "Little interest or pleasure in doing things"
    Category category,     // MOOD, ANXIETY, SLEEP, etc.
    Integer displayOrder,  // 1-84 for real questions, 200+ for derived
    String instrument,     // PHQ-9, GAD-7, CALCULATED
    String scaleType,      // LIKERT_03, BINARY, TEXT, NUMBER_INT, etc.
    Integer defaultValue   // 0 for symptom scales, 5 for WHO-5
) {}