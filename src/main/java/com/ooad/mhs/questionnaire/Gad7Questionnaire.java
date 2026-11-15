package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * GAD-7: anxiety (7 items, each 0-3, total 0-21).
 * Derived fields: gad7_total, gad7_symptom_count (items 2-7 at 2 or more),
 * anxiety_any (total >= 5), anxiety_moderate_plus (total >= 10).
 */
@Component
public class Gad7Questionnaire extends Questionnaire {

    public Gad7Questionnaire() {
        super(Section.GAD7);
    }

    @Override
    public void score(Map<String, Integer> f) {
        f.put("gad7_total", sum(f, "gad7_q", 1, 7));
        f.put("gad7_symptom_count", countAtLeast(f, "gad7_q", 2, 7, MORE_THAN_HALF_THE_DAYS));

        // Flags read by combined rules
        f.put("anxiety_any", flag(value(f, "gad7_total") >= MILD_CUTOFF));
        f.put("anxiety_moderate_plus", flag(value(f, "gad7_total") >= MODERATE_CUTOFF));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Total score " + f.get("gad7_total") + " / 21";
    }
}
