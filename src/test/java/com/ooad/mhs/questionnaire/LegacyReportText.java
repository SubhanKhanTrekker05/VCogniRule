/*
 * TEST-ONLY COPY of the score-text methods of service/ReportService.java (lines 415-449)
 * exactly as they were at commit e7095d2, before the Questionnaire refactor (M1).
 * Only made package-private static in this holder class. Do not edit the logic below.
 */
package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;

import java.util.Map;

final class LegacyReportText {

    private LegacyReportText() {
    }

    static String scoreText(Section section, Map<String, Integer> f, Map<String, String> answers) {
        return switch (section) {
            case PHQ9 -> "Total score " + f.get("phq9_total") + " / 27";
            case GAD7 -> "Total score " + f.get("gad7_total") + " / 21";
            case PHQ15 -> "Total score " + f.get("phq15_total") + " / 30";
            case CSSRS -> countYes(answers, "cssrs_q", 6) + " of 6 answered Yes";
            case WHO5 -> "Wellbeing score " + f.get("who5_raw") + " / 25 (" + (f.get("who5_raw") * 4) + "%)";
            case PSS10 -> "Total score " + f.get("pss10_total") + " / 40";
            case PSQI -> "Global score " + f.get("psqi_global") + " / 21";
            case WSAS -> "Total score " + f.get("wsas_total") + " / 40";
            case ASRS -> f.get("asrs_shaded_count") + " of 6 answers in the screening range";
            case PCPTSD -> countYes(answers, "pcptsd_q", 4) + " of 4 answered Yes";
        };
    }

    static String scoreNote(Section section) {
        return switch (section) {
            case WHO5 -> "Higher is better. 50% or below suggests low wellbeing.";
            case PSQI -> "Lower is better. Above 5 suggests poor sleep.";
            case CSSRS -> "Any Yes answer is followed up in the findings.";
            case ASRS -> "4 or more suggests symptoms worth discussing with a professional.";
            case PCPTSD -> "3 or more suggests trauma-related symptoms.";
            default -> "Higher means more symptoms.";
        };
    }

    /** Counts Yes answers: any answered value other than "0" (covers graded Safety Check answers). */
    private static int countYes(Map<String, String> answers, String prefix, int count) {
        int yes = 0;
        for (int i = 1; i <= count; i++) {
            String v = answers.get(prefix + i);
            if (v != null && !"0".equals(v)) yes++;
        }
        return yes;
    }
}
