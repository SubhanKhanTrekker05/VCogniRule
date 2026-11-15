package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ASRS: adult ADHD screener (6 items, each 0-4). Counts answers in the "shaded" screening range:
 * inattention items 1-3 at 3 or more, hyperactivity items 4-6 at 2 or more.
 * Derived fields: asrs_inattentive_count, asrs_hyperactive_count, asrs_shaded_count.
 */
@Component
public class AsrsQuestionnaire extends Questionnaire {

    public AsrsQuestionnaire() {
        super(Section.ASRS);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        int inattentive = countAtLeast(f, "asrs_q", 1, 3, 3);
        int hyperactive = countAtLeast(f, "asrs_q", 4, 6, 2);
        f.put("asrs_inattentive_count", inattentive);
        f.put("asrs_hyperactive_count", hyperactive);
        f.put("asrs_shaded_count", inattentive + hyperactive);
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return f.get("asrs_shaded_count") + " of 6 answers in the screening range";
    }

    @Override
    public String scoreNote() {
        return "4 or more suggests symptoms worth discussing with a professional.";
    }
}
