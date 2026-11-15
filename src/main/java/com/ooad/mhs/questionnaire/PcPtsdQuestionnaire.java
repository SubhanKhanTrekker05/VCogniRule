package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PC-PTSD: trauma response (4 yes/no items, total 0-4).
 * Derived field: pcptsd_total.
 */
@Component
public class PcPtsdQuestionnaire extends Questionnaire {

    public PcPtsdQuestionnaire() {
        super(Section.PCPTSD);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        f.put("pcptsd_total", sum(f, "pcptsd_q", 1, 4));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return countYes(answers, "pcptsd_q", 4) + " of 4 answered Yes";
    }

    @Override
    public String scoreNote() {
        return "3 or more suggests trauma-related symptoms.";
    }
}
