package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PSS-10: perceived stress (10 items, each 0-4, total 0-40).
 * Items 4, 5, 7 and 8 are positively worded, so they are REVERSE-scored (4 - answer).
 * Derived fields: pss10_helplessness, pss10_efficacy, pss10_total.
 */
@Component
public class Pss10Questionnaire extends Questionnaire {

    /** Highest answer per item ("Very often"); reverse score = MAX_ITEM_SCORE - answer. */
    private static final int MAX_ITEM_SCORE = 4;

    public Pss10Questionnaire() {
        super(Section.PSS10);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        // Helplessness: q1, q2, q3, q6, q9, q10 (summed directly)
        int helpless = value(f, "pss10_q1") + value(f, "pss10_q2") + value(f, "pss10_q3")
            + value(f, "pss10_q6") + value(f, "pss10_q9") + value(f, "pss10_q10");
        f.put("pss10_helplessness", helpless);

        // Efficacy: q4, q5, q7, q8 (reverse-scored: 4 - answer)
        int efficacy = (MAX_ITEM_SCORE - value(f, "pss10_q4")) + (MAX_ITEM_SCORE - value(f, "pss10_q5"))
            + (MAX_ITEM_SCORE - value(f, "pss10_q7")) + (MAX_ITEM_SCORE - value(f, "pss10_q8"));
        f.put("pss10_efficacy", efficacy);

        f.put("pss10_total", helpless + efficacy);
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Total score " + f.get("pss10_total") + " / 40";
    }
}
