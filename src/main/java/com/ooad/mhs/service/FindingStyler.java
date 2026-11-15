package com.ooad.mhs.service;

import com.ooad.mhs.domain.ConditionTrace;
import com.ooad.mhs.domain.Finding;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.util.ActionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides how a finding looks on the report: its tone (colour), icon and action label.
 *
 * Moved out of ReportService (Single Responsibility): ReportService assembles the report,
 * this class only answers "how serious does this finding look?".
 *
 * Rules:
 * 1. Safety-linked (its basis contains a positive risk signal) or a crisis rule → crisis.
 * 2. Otherwise by the rule's action type (how serious the recommendation is).
 * 3. Information (DISPLAY_INFO) findings: green only if reassuring, otherwise neutral.
 * Priority is NOT used: it orders findings, it does not say how serious they are.
 */
@Component
public class FindingStyler {

    /** How a finding is shown: CSS tone (class tone-*), icon, and a text label so meaning never relies on colour alone. */
    public record Style(String tone, String icon, String action) {}

    /** Tone of safety-related findings; they always stay with the user's results. */
    public static final String CRISIS_TONE = "crisis";

    /** Display order of finding tones, most serious first. */
    private static final List<String> TONE_ORDER =
        List.of(CRISIS_TONE, "urgent", "recommend", "suggest", "monitor", "note", "info");

    /**
     * Facts that signal a safety risk when their value is not 0.
     * A finding whose basis contains one of these (or a conclusion of a
     * CRISIS_PROTOCOL rule) is styled as crisis, whatever its action type.
     */
    private static final Set<String> RISK_FACTS = Set.of(
        "crisis_detected", "cssrs_passive_ideation", "cssrs_any_positive", "phq9_q9",
        "cssrs_q1", "cssrs_q2", "cssrs_q3", "cssrs_q4", "cssrs_q5", "cssrs_q6");

    /**
     * Information findings are green only when their result is recognisably reassuring.
     * Matched on the rule's conclusion key (e.g. depression_minimal, stress_low,
     * wellbeing_good, ptsd_negative, cssrs_no_risk, functioning_subclinical).
     * Anything else (symptom signals, follow-up gates, notes, rules added later
     * by an admin) defaults to neutral, so the report never shows false reassurance.
     */
    private static final Pattern REASSURING_KEY = Pattern.compile(
        "^(good_mental_health|.*_(minimal|low|good|negative|no_risk|subclinical))$");

    /**
     * True if this matched condition is a positive safety signal.
     *
     * @param crisisConclusions conclusion keys set by CRISIS_PROTOCOL rules
     */
    public boolean isSafetySignal(ConditionTrace t, Set<String> crisisConclusions) {
        boolean positive = !"0".equals(t.actualValue());
        return positive && (RISK_FACTS.contains(t.fieldKey()) || crisisConclusions.contains(t.fieldKey()));
    }

    /**
     * @param safetyLinked true if any matched condition is a safety signal
     * @param rule         the rule that produced the finding (null if it is no longer active)
     */
    public Style style(Finding f, boolean safetyLinked, Rule rule) {
        ActionType a = f.actionType();
        String action = switch (a) {
            case CRISIS_PROTOCOL -> "Crisis support";
            case URGENT_ASSESSMENT -> "Urgent assessment";
            case RECOMMEND_ASSESSMENT -> "Assessment recommended";
            case SUGGEST_PROFESSIONAL -> "Consider professional support";
            case MONITOR -> "Monitor";
            case DISPLAY_INFO -> "Information";
        };
        if (a == ActionType.CRISIS_PROTOCOL || safetyLinked) {
            if (a != ActionType.CRISIS_PROTOCOL) action += " · safety-related";
            return new Style(CRISIS_TONE, "⚠", action);
        }
        return switch (a) {
            case URGENT_ASSESSMENT -> new Style("urgent", "⚠", action);
            case RECOMMEND_ASSESSMENT -> new Style("recommend", "!", action);
            case SUGGEST_PROFESSIONAL -> new Style("suggest", "!", action);
            case MONITOR -> new Style("monitor", "•", action);
            default -> infoStyle(rule);
        };
    }

    /** Position in the "most serious first" order (lower = more serious). */
    public int seriousness(String tone) {
        return TONE_ORDER.indexOf(tone);
    }

    /** DISPLAY_INFO findings: green ✓ only when reassuring; symptom signals, gates and notes stay neutral. */
    private static Style infoStyle(Rule rule) {
        String key = rule != null && rule.conclusionKey() != null ? rule.conclusionKey() : "";
        String value = rule != null ? rule.conclusionValue() : null;
        boolean reassuring = REASSURING_KEY.matcher(key).matches()
            || (RuleEngine.OVERALL_SEVERITY_KEY.equals(key) && "minimal".equals(value));
        if (reassuring) return new Style("info", "✓", "No concern");
        if (key.endsWith("_clinical") || key.equals("ptsd_single")) {
            return new Style("note", "•", "Symptom noted");
        }
        if (key.endsWith("_unlocked")) return new Style("note", "→", "Follow-up checks");
        return new Style("note", "i", "Information");
    }
}
