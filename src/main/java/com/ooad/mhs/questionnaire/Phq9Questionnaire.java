package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PHQ-9: depression (9 items, each 0-3, total 0-27).
 * Derived fields: phq9_total, phq9_core_symptom, phq9_symptom_count,
 * depression_any (total >= 5), depression_moderate_plus (total >= 10).
 */
@Component
public class Phq9Questionnaire extends Questionnaire {

    public Phq9Questionnaire() {
        super(Section.PHQ9);
    }

    @Override
    public void score(Map<String, Integer> f) {
        f.put("phq9_total", sum(f, "phq9_q", 1, 9));
        // Core symptom: low interest (q1) or low mood (q2) on more than half the days
        f.put("phq9_core_symptom", flag(value(f, "phq9_q1") >= MORE_THAN_HALF_THE_DAYS || value(f, "phq9_q2") >= MORE_THAN_HALF_THE_DAYS));
        f.put("phq9_symptom_count", countAtLeast(f, "phq9_q", 1, 9, MORE_THAN_HALF_THE_DAYS));

        // Flags read by combined rules
        f.put("depression_any", flag(value(f, "phq9_total") >= MILD_CUTOFF));
        f.put("depression_moderate_plus", flag(value(f, "phq9_total") >= MODERATE_CUTOFF));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Total score " + f.get("phq9_total") + " / 27";
    }
}
