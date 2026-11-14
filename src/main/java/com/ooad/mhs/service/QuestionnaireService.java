package com.ooad.mhs.service;

import com.ooad.mhs.domain.Question;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds the view model for the tabbed, one-question-at-a-time assessment page.
 *
 * Groups the real questions by Section (tab) and attaches the answer options
 * for each question. Answer values are unchanged from the original form, so
 * the scoring in DerivedFieldCalculator is unaffected; only the labels shown
 * to the user are more accurate (e.g. PSQI frequency labels).
 */
@Service
public class QuestionnaireService {

    private final QuestionRepository questionRepository;

    public QuestionnaireService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /** One answer choice: the submitted value and its label. */
    public record Option(String value, String label) {}

    /**
     * One question as rendered on the page.
     * kind = "choice" (radio buttons), "time" or "number".
     */
    public record QuestionView(String fieldKey, String text, String lead, String kind,
                               List<Option> options, String min, String max, String step,
                               String unit) {}

    /** One tab: the section and its questions in display order. */
    public record SectionView(Section section, List<QuestionView> questions) {}

    /** Returns the sections in tab order, each with its questions. */
    public List<SectionView> getSections() {
        Map<Section, List<QuestionView>> bySection = new EnumMap<>(Section.class);
        for (Question q : questionRepository.findAllReal()) {
            Optional<Section> section = Section.fromCode(q.instrument());
            section.ifPresent(s -> bySection.computeIfAbsent(s, k -> new ArrayList<>()).add(toView(q)));
        }
        List<SectionView> result = new ArrayList<>();
        for (Section s : Section.values()) {
            List<QuestionView> questions = bySection.get(s);
            if (questions != null && !questions.isEmpty()) {
                result.add(new SectionView(s, questions));
            }
        }
        return result;
    }

    private QuestionView toView(Question q) {
        String key = q.fieldKey();
        String lead = leadFor(key);
        // WSAS items contain a "[problem]" blank meant to be filled in; show it as plain text
        String text = q.text().replaceAll("my \\[problem\\],? ", "my current difficulties, ");
        return switch (q.scaleType()) {
            case "TEXT" -> new QuestionView(key, text, lead, "time", List.of(), null, null, null, null);
            case "NUMBER_INT" -> new QuestionView(key, text, lead, "number", List.of(), "0", "180", "1", "minutes");
            case "NUMBER_DEC" -> new QuestionView(key, text, lead, "number", List.of(), "0", "24", "0.5", "hours");
            default -> new QuestionView(key, text, lead, "choice", optionsFor(q), null, null, null, null);
        };
    }

    /** Extra lead-in text for questions that only make sense with it. */
    private String leadFor(String key) {
        if (key.startsWith("psqi_q5")) {
            return "During the past month, how often have you had trouble sleeping because you…";
        }
        if (key.equals("pcptsd_q2") || key.equals("pcptsd_q3") || key.equals("pcptsd_q4")) {
            return "Thinking about that experience:";
        }
        return null;
    }

    private List<Option> optionsFor(Question q) {
        String key = q.fieldKey();
        return switch (q.scaleType()) {
            // Safety Check: graded Yes answers so users are not forced into a bare "Yes".
            // Every value >= 1 is scored as Yes (see AssessmentService.GRADED_YES_PREFIX).
            case "BINARY" -> key.startsWith(AssessmentService.GRADED_YES_PREFIX)
                ? labels("No", "Yes, once or twice", "Yes, a few times", "Yes, many times")
                : labels("No", "Yes");
            case "LIKERT_02" -> labels("Not bothered at all", "Bothered a little", "Bothered a lot");
            case "LIKERT_03" -> {
                if (key.equals("psqi_q6")) yield labels("Very good", "Fairly good", "Fairly bad", "Very bad");
                if (key.equals("psqi_q9")) yield labels("No problem at all", "Only a very slight problem",
                    "Somewhat of a problem", "A very big problem");
                if (key.startsWith("psqi_")) yield labels("Not during the past month", "Less than once a week",
                    "Once or twice a week", "Three or more times a week");
                yield labels("Not at all", "Several days", "More than half the days", "Nearly every day");
            }
            case "LIKERT_04" -> key.startsWith("pss10_")
                ? labels("Never", "Almost never", "Sometimes", "Fairly often", "Very often")
                : labels("Never", "Rarely", "Sometimes", "Often", "Very often");
            case "LIKERT_05" -> labels("At no time", "Some of the time", "Less than half of the time",
                "More than half of the time", "Most of the time", "All of the time");
            case "LIKERT_08" -> labels("Not at all", "1", "Slightly", "3", "Definitely", "5",
                "Markedly", "7", "Very severely");
            default -> labels("0", "1", "2", "3");
        };
    }

    /** Labels in value order: the first label is value 0, the next value 1, and so on. */
    private static List<Option> labels(String... labels) {
        List<Option> options = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            options.add(new Option(String.valueOf(i), labels[i]));
        }
        return options;
    }
}
