package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WHO-5: wellbeing (5 items, each 0-5, raw score 0-25). Higher is BETTER
 * (positively worded items, not reverse-scored).
 * Derived field: who5_raw. A missing answer counts as 5 (as in the original scoring).
 */
@Component
public class Who5Questionnaire extends Questionnaire {

    /** Highest answer per item ("All of the time"); also used for a missing answer, as in the original scoring. */
    private static final int MAX_ITEM_SCORE = 5;

    /** The raw score (0-25) times 4 gives the WHO-5 percentage score (0-100). */
    private static final int PERCENT_PER_POINT = 4;

    public Who5Questionnaire() {
        super(Section.WHO5);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        int total = 0;
        for (int i = 1; i <= 5; i++) total += f.getOrDefault("who5_q" + i, MAX_ITEM_SCORE);
        f.put("who5_raw", total);
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Wellbeing score " + f.get("who5_raw") + " / 25 (" + (f.get("who5_raw") * PERCENT_PER_POINT) + "%)";
    }

    @Override
    public String scoreNote() {
        return "Higher is better. 50% or below suggests low wellbeing.";
    }
}
