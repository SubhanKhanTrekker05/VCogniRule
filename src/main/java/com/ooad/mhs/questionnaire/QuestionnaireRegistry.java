package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Holds the one {@link Questionnaire} object of each {@link Section}.
 *
 * Spring finds every Questionnaire subclass (they are @Component) and passes them in as a
 * list. The registry checks that every section has exactly one, so a missing or duplicate
 * questionnaire stops the app at startup instead of silently skipping a section.
 *
 * Aggregation: the registry refers to the questionnaires but does not create them.
 */
@Component
public class QuestionnaireRegistry {

    private final Map<Section, Questionnaire> bySection = new EnumMap<>(Section.class);

    public QuestionnaireRegistry(List<Questionnaire> questionnaires) {
        for (Questionnaire q : questionnaires) {
            if (bySection.put(q.getSection(), q) != null) {
                throw new IllegalStateException("Two questionnaires for section " + q.getSection());
            }
        }
        for (Section s : Section.values()) {
            if (!bySection.containsKey(s)) {
                throw new IllegalStateException("No questionnaire for section " + s);
            }
        }
    }

    /** The questionnaire of a section (never null: every section is checked at startup). */
    public Questionnaire get(Section section) {
        return bySection.get(section);
    }

    /** All questionnaires, in tab order. */
    public Collection<Questionnaire> all() {
        return Collections.unmodifiableCollection(bySection.values());
    }

    /** All ten questionnaires, created without Spring (used by unit tests). */
    public static QuestionnaireRegistry createDefault() {
        return new QuestionnaireRegistry(List.of(
            new CssrsQuestionnaire(), new Phq9Questionnaire(), new Gad7Questionnaire(),
            new Who5Questionnaire(), new Pss10Questionnaire(), new PsqiQuestionnaire(),
            new Phq15Questionnaire(), new PcPtsdQuestionnaire(), new AsrsQuestionnaire(),
            new WsasQuestionnaire()));
    }
}
