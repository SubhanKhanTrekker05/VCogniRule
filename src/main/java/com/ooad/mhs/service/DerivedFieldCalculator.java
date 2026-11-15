package com.ooad.mhs.service;

import com.ooad.mhs.questionnaire.Questionnaire;
import com.ooad.mhs.questionnaire.QuestionnaireRegistry;
import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Computes all derived fields (totals, subscales, flags) from the user's answers
 * BEFORE rule evaluation.
 *
 * This is the pre-processor for the rule engine. It does not know how any questionnaire
 * is scored: it asks each completed {@link Questionnaire} to add its own derived fields
 * (polymorphism; see the questionnaire package).
 *
 * The output map (working_facts) contains BOTH the raw responses AND the
 * derived fields — the engine reads from this single map.
 *
 * Called by: AssessmentService.prepare(), before RuleEngine.evaluate()
 */
@Service
public class DerivedFieldCalculator {

    private final QuestionnaireRegistry questionnaires;

    public DerivedFieldCalculator(QuestionnaireRegistry questionnaires) {
        this.questionnaires = questionnaires;
    }

    /**
     * Scores only the completed sections. Derived fields of sections the user
     * did not complete are left out of working_facts, so rules that need them
     * do not fire instead of firing on default values.
     */
    public Map<String, Integer> calculate(Map<String, Integer> responses, Set<Section> completed) {
        Map<String, Integer> facts = new HashMap<>(responses);
        for (Questionnaire questionnaire : questionnaires.all()) {
            if (completed.contains(questionnaire.getSection())) {
                questionnaire.contributeTo(facts);
            }
        }
        return facts;
    }
}
