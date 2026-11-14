package com.ooad.mhs.service;

import com.ooad.mhs.domain.Question;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Converts raw form responses to a working_facts map with derived fields.
 *
 * Flow: HTML form (Map<String, String>) → section completion check → convertResponses
 *       → DerivedFieldCalculator → working_facts
 *
 * Only sections the user fully answered are scored. Answers from partly
 * answered sections are left out, so the rule engine never evaluates a
 * section on guessed defaults. Rules whose fields are missing simply do
 * not fire (see ConditionEvaluator).
 *
 * Exception: safety-critical single items (PHQ-9 item 9, thoughts of
 * self-harm) are always kept when answered, so a risk signal is never
 * dropped just because the rest of the section was not finished.
 *
 * Value conversion:
 * - Radio buttons: direct parseInt (e.g., "2" → 2)
 * - PSQI time inputs: "23:00" → 1380 (minutes from midnight)
 * - PSQI hours: "7.5" → 8 (rounded to integer)
 */
@Service
public class AssessmentService {

    /** Items kept even when their section is incomplete (rules 6 and 69 read them directly). */
    private static final Set<String> SAFETY_ITEMS = Set.of("phq9_q9");

    /**
     * Safety Check (C-SSRS) questions offer graded answers
     * (0 = No, 1 = once or twice, 2 = a few times, 3 = many times).
     * C-SSRS is a yes/no screener and every rule expects 0 or 1, so ANY value
     * other than 0 is scored as 1 (Yes). How often only changes what the
     * report displays, never whether a risk rule fires.
     */
    public static final String GRADED_YES_PREFIX = "cssrs_";

    private final DerivedFieldCalculator derivedFieldCalculator;
    private final QuestionRepository questionRepository;

    public AssessmentService(DerivedFieldCalculator derivedFieldCalculator,
                             QuestionRepository questionRepository) {
        this.derivedFieldCalculator = derivedFieldCalculator;
        this.questionRepository = questionRepository;
    }

    /** How much of one section the user answered. */
    public record SectionStatus(Section section, int answered, int total) {
        public boolean isComplete() { return total > 0 && answered == total; }
        public boolean isPartial() { return answered > 0 && answered < total; }
        public boolean isNotStarted() { return answered == 0; }
    }

    /** Working facts for the engine plus the completion status of every section (in tab order). */
    public record PreparedAssessment(Map<String, Integer> workingFacts,
                                     Map<Section, SectionStatus> statuses) {
        public boolean isComplete(Section section) {
            SectionStatus s = statuses.get(section);
            return s != null && s.isComplete();
        }
    }

    /** Keeps only answered fields that belong to real questions (drops unknown or blank params). */
    public Map<String, String> filterAnswers(Map<String, String> formData) {
        Map<String, String> answers = new HashMap<>();
        for (Question q : questionRepository.findAllReal()) {
            String value = formData.get(q.fieldKey());
            if (value != null && !value.isBlank()) {
                answers.put(q.fieldKey(), value.trim());
            }
        }
        return answers;
    }

    /**
     * Takes the user's answers and returns working_facts ready for the RuleEngine,
     * together with which sections were completed.
     */
    public PreparedAssessment prepare(Map<String, String> answers) {
        Map<Section, List<String>> keysBySection = new EnumMap<>(Section.class);
        for (Question q : questionRepository.findAllReal()) {
            Section.fromCode(q.instrument()).ifPresent(
                s -> keysBySection.computeIfAbsent(s, k -> new ArrayList<>()).add(q.fieldKey()));
        }

        Map<Section, SectionStatus> statuses = new LinkedHashMap<>();
        Set<Section> completed = EnumSet.noneOf(Section.class);
        Set<String> keysToScore = new HashSet<>();

        for (Section section : Section.values()) {
            List<String> keys = keysBySection.getOrDefault(section, List.of());
            int answered = (int) keys.stream().filter(answers::containsKey).count();
            SectionStatus status = new SectionStatus(section, answered, keys.size());
            statuses.put(section, status);
            if (status.isComplete()) {
                completed.add(section);
                keysToScore.addAll(keys);
            }
        }
        for (String key : SAFETY_ITEMS) {
            if (answers.containsKey(key)) keysToScore.add(key);
        }

        Map<String, Integer> responses = convertResponses(answers, keysToScore);
        Map<String, Integer> facts = derivedFieldCalculator.calculate(responses, completed);
        return new PreparedAssessment(facts, statuses);
    }

    private Map<String, Integer> convertResponses(Map<String, String> answers, Set<String> keysToScore) {
        Map<String, Integer> responses = new HashMap<>();

        for (String key : keysToScore) {
            String value = answers.get(key);
            if (value == null) continue;

            // Safety Check graded answers: anything other than "0" (No) is Yes.
            // Unparseable values also count as Yes, so an error can never hide a risk answer.
            if (key.startsWith(GRADED_YES_PREFIX)) {
                responses.put(key, "0".equals(value) ? 0 : 1);
            }
            // PSQI time inputs: "23:00" → 1380 minutes
            else if (key.equals("psqi_q1") || key.equals("psqi_q3")) {
                responses.put(key, parseTimeToMinutes(value));
            }
            // PSQI hours: "7.5" → 8 (rounded)
            else if (key.equals("psqi_q4")) {
                try {
                    responses.put(key, (int) Math.round(Double.parseDouble(value)));
                } catch (NumberFormatException e) {
                    responses.put(key, 7);
                }
            }
            // Regular integer (radio buttons, number inputs)
            else {
                try {
                    responses.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    responses.put(key, 0);
                }
            }
        }

        return responses;
    }

    private int parseTimeToMinutes(String time) {
        if (time == null || time.isEmpty()) return 1380; // default 23:00
        String[] parts = time.split(":");
        if (parts.length != 2) return 1380;
        try {
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return 1380;
        }
    }
}
