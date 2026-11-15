package com.ooad.mhs.service;

import org.springframework.stereotype.Component;

/**
 * Decides the overall result shown in the report banner.
 *
 * Moved out of ReportService (Single Responsibility). The rule base has five overall
 * rules (Level 4). When none fires, there is no basis for a severity, so the result is
 * never silently "minimal". A crisis finding always outranks a reassuring overall label.
 */
@Component
public class OverallResultResolver {

    /**
     * @param key   CSS/state key: a severity (critical, severe, moderate, mild, minimal)
     *              or a status (attention, undetermined, unclassified)
     * @param label text shown in the banner
     * @param note  explanation under the banner, or null
     */
    public record OverallResult(String key, String label, String note) {}

    /**
     * @param severity           severity set by the Level 4 rules, or null if none fired
     * @param hasCrisis          true if a CRISIS_PROTOCOL rule fired
     * @param depressionComplete true if the PHQ-9 section was completed
     * @param anyPartial         true if any section was only partly answered
     */
    public OverallResult resolve(String severity, boolean hasCrisis, boolean depressionComplete, boolean anyPartial) {
        if (hasCrisis && (severity == null || severity.equals("mild") || severity.equals("minimal")
                || severity.equals("moderate"))) {
            String note = "One or more answers suggest a safety concern. This takes priority over every other result.";
            if (severity != null) {
                note += " Apart from the safety concern, the rules rated the overall pattern as "
                    + severity.toUpperCase() + ".";
            }
            return new OverallResult("attention", "Needs attention", note);
        }
        if (severity != null) {
            // A partly answered section was left out: say so, so "MINIMAL" is not read as covering it
            String note = anyPartial
                ? "Based only on the sections you completed. Partly answered sections are listed below."
                : null;
            return new OverallResult(severity, severity, note);
        }
        if (!depressionComplete) {
            return new OverallResult("undetermined", "Not determined",
                "The overall result needs the Depression & Mood section (see below).");
        }
        return new OverallResult("unclassified", "Not classified",
            "Your answers did not match one of the overall patterns. Please read the individual findings below.");
    }
}
