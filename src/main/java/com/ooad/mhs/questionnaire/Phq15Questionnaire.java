package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PHQ-15: physical (somatic) symptoms (15 items a-o, each 0-2, total 0-30).
 * Derived fields: phq15_total, phq15_severe_count (items answered 2, "bothered a lot"),
 * panic_physical_symptom (racing heart (item i) or shortness of breath (item j)).
 */
@Component
public class Phq15Questionnaire extends Questionnaire {

    /** Answer value 2 = "Bothered a lot" (the highest answer). */
    private static final int BOTHERED_A_LOT = 2;

    private static final String[] ITEMS = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o"};

    public Phq15Questionnaire() {
        super(Section.PHQ15);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        int total = 0;
        int severe = 0;
        for (String item : ITEMS) {
            int v = value(f, "phq15_" + item);
            total += v;
            if (v == BOTHERED_A_LOT) severe++;
        }
        f.put("phq15_total", total);
        f.put("phq15_severe_count", severe);
    }

    @Override
    protected void computeFlags(Map<String, Integer> f) {
        f.put("panic_physical_symptom", flag(value(f, "phq15_i") >= 1 || value(f, "phq15_j") >= 1));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Total score " + f.get("phq15_total") + " / 30";
    }
}
