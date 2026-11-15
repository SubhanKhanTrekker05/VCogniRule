package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * C-SSRS Safety Check (6 yes/no items, mandatory). Safety-based, not a total score.
 * Derived fields: cssrs_passive_ideation (q1 or q2), cssrs_any_positive (any item),
 * crisis_detected (q4 intent, q5 plan or q6 behaviour).
 * Answers arrive as 0/1: AssessmentService converts every graded Yes to 1.
 */
@Component
public class CssrsQuestionnaire extends Questionnaire {

    public CssrsQuestionnaire() {
        super(Section.CSSRS);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        boolean q1 = value(f, "cssrs_q1") == 1;
        boolean q2 = value(f, "cssrs_q2") == 1;
        boolean q3 = value(f, "cssrs_q3") == 1;
        boolean q4 = value(f, "cssrs_q4") == 1;
        boolean q5 = value(f, "cssrs_q5") == 1;
        boolean q6 = value(f, "cssrs_q6") == 1;

        f.put("cssrs_passive_ideation", flag(q1 || q2));
        f.put("cssrs_any_positive", flag(q1 || q2 || q3 || q4 || q5 || q6));
        f.put("crisis_detected", flag(q4 || q5 || q6));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return countYes(answers, "cssrs_q", 6) + " of 6 answered Yes";
    }

    @Override
    public String scoreNote() {
        return "Any Yes answer is followed up in the findings.";
    }
}
