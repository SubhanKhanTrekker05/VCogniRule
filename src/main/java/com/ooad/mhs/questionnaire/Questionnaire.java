package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;

import java.util.Map;
import java.util.Objects;

/**
 * One screening questionnaire (PHQ-9, GAD-7, C-SSRS, ...).
 *
 * Abstract base class of the ten questionnaires. Each subclass knows how to score its
 * own answers: it calculates the totals, subscales and flags ("derived fields") that the
 * rule engine reads, and the score line shown on the report.
 *
 * What it does NOT do: decide severity (mild, moderate, severe ...). Severity comes from
 * the rules in the knowledge base (e.g. PHQ9-MODERATE: phq9_total BETWEEN 10 AND 14), so
 * an admin can change it without touching Java code.
 *
 * Design patterns:
 * - Template Method: {@link #contributeTo} fixes the order of the steps
 *   (scores first, then flags); subclasses fill in the steps.
 * - Polymorphism: DerivedFieldCalculator calls {@code contributeTo} on every questionnaire
 *   without knowing which one it is.
 */
public abstract class Questionnaire {

    /** The tab / instrument this questionnaire scores. */
    private final Section section;

    protected Questionnaire(Section section) {
        this.section = Objects.requireNonNull(section, "section");
    }

    public final Section getSection() {
        return section;
    }

    /**
     * Template method: adds this questionnaire's derived fields to the working facts.
     * Called only when the user completed the section.
     *
     * @param facts the user's converted answers; derived fields are added to this map
     */
    public final void contributeTo(Map<String, Integer> facts) {
        computeScores(facts);
        computeFlags(facts);
    }

    /** Step 1: totals and subscale scores, e.g. phq9_total. */
    protected abstract void computeScores(Map<String, Integer> facts);

    /**
     * Step 2 (optional hook): yes/no flags read by combined rules, e.g. depression_any.
     * Does nothing unless a subclass overrides it.
     */
    protected void computeFlags(Map<String, Integer> facts) {
    }

    /**
     * The score line shown in the report's "Your answers" list, e.g. "Total score 14 / 27".
     *
     * @param facts   working facts (after {@link #contributeTo})
     * @param answers the user's raw answers as submitted (needed to count graded Yes answers)
     */
    public abstract String summary(Map<String, Integer> facts, Map<String, String> answers);

    /** How to read the score. Subclasses override this when higher is not simply "more symptoms". */
    public String scoreNote() {
        return "Higher means more symptoms.";
    }

    /**
     * Answer value 2 on the 0-3 frequency scale (PHQ-9, GAD-7) = "More than half the days".
     * An item counts as a clear symptom from this value upwards.
     */
    protected static final int MORE_THAN_HALF_THE_DAYS = 2;

    /** Total score from which PHQ-9 and GAD-7 show at least MILD symptoms (Kroenke 2001, Spitzer 2006). */
    protected static final int MILD_CUTOFF = 5;

    /** Total score from which PHQ-9 and GAD-7 show at least MODERATE symptoms. */
    protected static final int MODERATE_CUTOFF = 10;

    // ------------------------------------------------------------------ helpers for subclasses

    /** The answer for a key, or 0 if missing. */
    protected static int value(Map<String, Integer> facts, String key) {
        return facts.getOrDefault(key, 0);
    }

    /** Sum of prefix+from ... prefix+to, missing answers counted as 0 (e.g. phq9_q1..phq9_q9). */
    protected static int sum(Map<String, Integer> facts, String prefix, int from, int to) {
        int total = 0;
        for (int i = from; i <= to; i++) total += value(facts, prefix + i);
        return total;
    }

    /** How many of prefix+from ... prefix+to are at least {@code threshold}. */
    protected static int countAtLeast(Map<String, Integer> facts, String prefix, int from, int to, int threshold) {
        int count = 0;
        for (int i = from; i <= to; i++) {
            if (value(facts, prefix + i) >= threshold) count++;
        }
        return count;
    }

    /** 1 for true, 0 for false (the rule engine works with integers only). */
    protected static int flag(boolean condition) {
        return condition ? 1 : 0;
    }

    /** Counts Yes answers: any answered value other than "0" (covers graded Safety Check answers). */
    protected static int countYes(Map<String, String> answers, String prefix, int count) {
        int yes = 0;
        for (int i = 1; i <= count; i++) {
            String v = answers.get(prefix + i);
            if (v != null && !"0".equals(v)) yes++;
        }
        return yes;
    }
}
