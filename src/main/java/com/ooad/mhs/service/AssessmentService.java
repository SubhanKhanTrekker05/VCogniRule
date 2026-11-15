package com.ooad.mhs.service;

import com.ooad.mhs.service.QuestionnaireService.Option;
import com.ooad.mhs.service.QuestionnaireService.QuestionView;
import com.ooad.mhs.service.QuestionnaireService.SectionView;
import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

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
 * Input validation: an answer counts only if it is valid for its question
 * (one of the question's options, a real HH:MM time, or a number inside the
 * field's min/max). Anything else is treated as NOT ANSWERED, so its section
 * is "partly answered" and not scored. No value is ever invented.
 * Exception (safety): any non-blank Safety Check (C-SSRS) answer other than "0"
 * counts as Yes, so an error can never hide a risk answer.
 *
 * Value conversion (after validation):
 * - Radio buttons and whole numbers: parseInt (e.g., "2" → 2)
 * - Time inputs (PSQI bedtime / wake time): "23:00" → 1380 (minutes from midnight)
 * - Decimal numbers (PSQI hours slept): "7.5" → 8 (rounded to integer)
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

    /** A 24-hour clock time as sent by the browser's time input, e.g. "23:05". */
    private static final Pattern TIME = Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)$");

    private final DerivedFieldCalculator derivedFieldCalculator;
    private final QuestionnaireService questionnaireService;

    public AssessmentService(DerivedFieldCalculator derivedFieldCalculator,
                             QuestionnaireService questionnaireService) {
        this.derivedFieldCalculator = derivedFieldCalculator;
        this.questionnaireService = questionnaireService;
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

    /**
     * Keeps only valid answers to real questions. Unknown fields, blank answers and
     * invalid values (e.g. "abc", 99 on a 0-3 scale, "25:00") are dropped, so they count
     * as not answered.
     */
    public Map<String, String> filterAnswers(Map<String, String> formData) {
        Map<String, String> answers = new HashMap<>();
        for (SectionView section : questionnaireService.getSections()) {
            for (QuestionView q : section.questions()) {
                String value = formData.get(q.fieldKey());
                if (value != null && isValid(q, value.trim())) {
                    answers.put(q.fieldKey(), value.trim());
                }
            }
        }
        return answers;
    }

    /** True if the (trimmed) value is an acceptable answer to the question. */
    static boolean isValid(QuestionView q, String value) {
        if (value.isEmpty()) return false;
        // Safety Check: any non-blank answer counts (anything except "0" is scored as Yes)
        if (q.fieldKey().startsWith(GRADED_YES_PREFIX)) return true;
        return switch (q.kind()) {
            case "choice" -> q.options().stream().map(Option::value).anyMatch(value::equals);
            case "time" -> TIME.matcher(value).matches();
            case "number" -> isNumberInRange(q, value);
            default -> false;
        };
    }

    private static boolean isNumberInRange(QuestionView q, String value) {
        try {
            double v = isDecimal(q) ? Double.parseDouble(value) : Integer.parseInt(value);
            return Double.isFinite(v)
                && (q.min() == null || v >= Double.parseDouble(q.min()))
                && (q.max() == null || v <= Double.parseDouble(q.max()));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Number fields with a fractional step (e.g. "0.5" hours) accept decimals. */
    private static boolean isDecimal(QuestionView q) {
        return q.step() != null && q.step().contains(".");
    }

    /**
     * Takes the user's answers and returns working_facts ready for the RuleEngine,
     * together with which sections were completed.
     */
    public PreparedAssessment prepare(Map<String, String> submitted) {
        // Validate again, so prepare() is safe even if a caller skipped filterAnswers()
        Map<String, String> answers = filterAnswers(submitted);

        Map<Section, List<String>> keysBySection = new EnumMap<>(Section.class);
        Map<String, QuestionView> viewsByKey = new HashMap<>();
        for (SectionView sv : questionnaireService.getSections()) {
            for (QuestionView q : sv.questions()) {
                keysBySection.computeIfAbsent(sv.section(), k -> new ArrayList<>()).add(q.fieldKey());
                viewsByKey.put(q.fieldKey(), q);
            }
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

        Map<String, Integer> responses = convertResponses(answers, keysToScore, viewsByKey);
        Map<String, Integer> facts = derivedFieldCalculator.calculate(responses, completed);
        return new PreparedAssessment(facts, statuses);
    }

    /** Converts the (already validated) answers to integers for the rule engine. */
    private Map<String, Integer> convertResponses(Map<String, String> answers, Set<String> keysToScore,
                                                  Map<String, QuestionView> viewsByKey) {
        Map<String, Integer> responses = new HashMap<>();

        for (String key : keysToScore) {
            String value = answers.get(key);
            QuestionView q = viewsByKey.get(key);
            if (value == null || q == null) continue;

            // Safety Check graded answers: anything other than "0" (No) is Yes.
            if (key.startsWith(GRADED_YES_PREFIX)) {
                responses.put(key, "0".equals(value) ? 0 : 1);
            }
            // Time inputs: "23:00" → 1380 minutes from midnight
            else if ("time".equals(q.kind())) {
                String[] parts = value.split(":");
                responses.put(key, Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]));
            }
            // Decimal numbers (hours slept): "7.5" → 8 (rounded)
            else if ("number".equals(q.kind()) && isDecimal(q)) {
                responses.put(key, (int) Math.round(Double.parseDouble(value)));
            }
            // Radio buttons and whole numbers
            else {
                responses.put(key, Integer.parseInt(value));
            }
        }

        return responses;
    }
}
