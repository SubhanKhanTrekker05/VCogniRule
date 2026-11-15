package com.ooad.mhs.service;

import com.ooad.mhs.domain.ConditionTrace;
import com.ooad.mhs.domain.Finding;
import com.ooad.mhs.domain.Question;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.questionnaire.Questionnaire;
import com.ooad.mhs.questionnaire.QuestionnaireRegistry;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.repository.RuleRepository;
import com.ooad.mhs.service.AssessmentService.PreparedAssessment;
import com.ooad.mhs.service.AssessmentService.SectionStatus;
import com.ooad.mhs.service.QuestionnaireService.Option;
import com.ooad.mhs.service.QuestionnaireService.QuestionView;
import com.ooad.mhs.service.QuestionnaireService.SectionView;
import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the report model that the Thymeleaf template renders.
 *
 * Takes the RuleEngineResult and adds:
 * - Overall result (severity from the rules, or a status when no overall rule applies)
 * - Crisis flag (whether any crisis rule fired)
 * - For every finding, a readable "why this appears" list built from the
 *   matched conditions: the user's own answers, scores, or earlier findings
 * - The user's answers per section, with each section's score
 * - Which sections were assessed, partly answered, or skipped
 *
 * Two jobs are delegated (Single Responsibility): FindingStyler decides how each finding
 * looks, OverallResultResolver decides the overall banner.
 */
@Service
public class ReportService {

    private final QuestionRepository questionRepository;
    private final QuestionnaireService questionnaireService;
    private final RuleRepository ruleRepository;
    private final QuestionnaireRegistry questionnaires;
    private final FindingStyler styler;
    private final OverallResultResolver overallResolver;

    public ReportService(QuestionRepository questionRepository,
                         QuestionnaireService questionnaireService,
                         RuleRepository ruleRepository,
                         QuestionnaireRegistry questionnaires,
                         FindingStyler styler,
                         OverallResultResolver overallResolver) {
        this.questionRepository = questionRepository;
        this.questionnaireService = questionnaireService;
        this.ruleRepository = ruleRepository;
        this.questionnaires = questionnaires;
        this.styler = styler;
        this.overallResolver = overallResolver;
    }

    // ------------------------------------------------------------------ model

    /** One line of "why this appears": what was checked, the user's value, what the rule needed. */
    public record BasisLine(String label, String value, String needed, boolean fromEarlierFinding) {}

    /**
     * A finding plus its readable basis and how it is styled.
     *
     * @param tone   crisis | urgent | recommend | suggest | monitor | info (CSS class tone-*)
     * @param icon   symbol shown before the name
     * @param action text label of the action, so meaning never relies on colour alone
     */
    public record FindingView(Finding finding, List<BasisLine> basis,
                              String tone, String icon, String action) {}

    private static final Pattern BETWEEN_NEED = Pattern.compile("^(-?\\d+) - (-?\\d+)$");

    /** Plain-language labels for calculated scores and flags shown in "Why this appears". */
    private static final Map<String, String> FRIENDLY_LABELS = Map.ofEntries(
        Map.entry("phq9_total", "Depression score (PHQ-9 total, 0–27)"),
        Map.entry("phq9_core_symptom", "Low mood or loss of interest on more than half the days"),
        Map.entry("phq9_symptom_count", "Depression symptoms on more than half the days"),
        Map.entry("gad7_total", "Anxiety score (GAD-7 total, 0–21)"),
        Map.entry("gad7_symptom_count", "Anxiety symptoms on more than half the days"),
        Map.entry("phq15_total", "Physical symptoms score (PHQ-15 total, 0–30)"),
        Map.entry("phq15_severe_count", "Physical symptoms that bother you a lot"),
        Map.entry("cssrs_passive_ideation", "Thoughts of death or of not waking up"),
        Map.entry("cssrs_any_positive", "Any Yes answer in the Safety Check"),
        Map.entry("crisis_detected", "Safety Check: intent, plan or past actions"),
        Map.entry("who5_raw", "Wellbeing score (WHO-5, 0–25)"),
        Map.entry("pss10_total", "Stress score (PSS-10 total, 0–40)"),
        Map.entry("pss10_helplessness", "Stress: feeling helpless (0–24)"),
        Map.entry("pss10_efficacy", "Stress: low confidence in coping (0–16)"),
        Map.entry("psqi_c1", "Sleep: how you rate your sleep (0–3)"),
        Map.entry("psqi_c2", "Sleep: time to fall asleep (0–3)"),
        Map.entry("psqi_c3", "Sleep: hours of sleep (0–3)"),
        Map.entry("psqi_c4", "Sleep: time in bed actually asleep (0–3)"),
        Map.entry("psqi_c5", "Sleep: disturbances during the night (0–3)"),
        Map.entry("psqi_c6", "Sleep: use of sleep medicine (0–3)"),
        Map.entry("psqi_c7", "Sleep: daytime tiredness and drive (0–3)"),
        Map.entry("psqi_global", "Sleep quality score (PSQI, 0–21)"),
        Map.entry("wsas_total", "Daily functioning score (WSAS, 0–40)"),
        Map.entry("asrs_inattentive_count", "Attention: inattention answers in the screening range (0–3)"),
        Map.entry("asrs_hyperactive_count", "Attention: restlessness answers in the screening range (0–3)"),
        Map.entry("asrs_shaded_count", "Attention answers in the screening range (0–6)"),
        Map.entry("pcptsd_total", "Trauma response: Yes answers (0–4)"),
        Map.entry("panic_physical_symptom", "Racing heart or shortness of breath"),
        Map.entry("depression_any", "Depression score of 5 or more (at least mild)"),
        Map.entry("depression_moderate_plus", "Depression score of 10 or more (moderate or higher)"),
        Map.entry("anxiety_any", "Anxiety score of 5 or more (at least mild)"),
        Map.entry("anxiety_moderate_plus", "Anxiety score of 10 or more (moderate or higher)"),
        Map.entry("sleep_poor_any", "Sleep score above 5 (poor sleep)"),
        Map.entry(RuleEngine.ANY_TREATMENT_KEY, "At least one other finding recommends support or monitoring"));

    /** One answered (or unanswered) question in the "Your answers" list. */
    public record AnswerRow(int number, String question, String answer, boolean answered) {}

    /** One section in the "Your answers" list. */
    public record SectionAnswers(SectionStatus status, String score, String scoreNote, List<AnswerRow> rows) {}

    /**
     * The complete report data passed to the Thymeleaf template.
     *
     * @param overallKey   CSS/state key: a severity (critical, severe, moderate, mild, minimal)
     *                     or a status (attention, undetermined, unclassified)
     * @param overallLabel text shown in the overall banner
     * @param overallNote  explanation under the banner, or null
     * @param depressionMissing true when the PHQ-9 (Depression) section was not completed
     *
     * Create it with {@link #builder()} rather than the 12-argument constructor.
     */
    public record ScreeningReport(
        List<FindingView> results,      // about the user; safety-related always here, most serious first
        List<FindingView> guidance,     // NICE recommendations written for professionals
        List<FindingView> reasoning,    // intermediate engine steps (pathway gates, NICE synthesis)
        String overallKey,
        String overallLabel,
        String overallNote,
        boolean hasCrisis,
        List<SectionStatus> assessed,
        List<SectionStatus> partial,
        List<SectionStatus> notStarted,
        boolean depressionMissing,
        List<SectionAnswers> answers
    ) {
        /** Starts building a report. */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder pattern: sets the report's 12 parts by name, step by step, instead of one
         * 12-argument constructor call where two lists or two booleans could easily be swapped.
         * Lists that are not set are empty; {@link #build()} checks the overall result is set.
         */
        public static final class Builder {
            private List<FindingView> results = List.of();
            private List<FindingView> guidance = List.of();
            private List<FindingView> reasoning = List.of();
            private String overallKey;
            private String overallLabel;
            private String overallNote;
            private boolean hasCrisis;
            private List<SectionStatus> assessed = List.of();
            private List<SectionStatus> partial = List.of();
            private List<SectionStatus> notStarted = List.of();
            private boolean depressionMissing;
            private List<SectionAnswers> answers = List.of();

            private Builder() {
            }

            /** The three groups of finding cards. */
            public Builder findings(List<FindingView> results, List<FindingView> guidance,
                                    List<FindingView> reasoning) {
                this.results = results;
                this.guidance = guidance;
                this.reasoning = reasoning;
                return this;
            }

            /** The overall banner: state key, label and optional note (may be null). */
            public Builder overall(String key, String label, String note) {
                this.overallKey = key;
                this.overallLabel = label;
                this.overallNote = note;
                return this;
            }

            /** Whether a crisis rule fired (shows the helpline box). */
            public Builder crisis(boolean hasCrisis) {
                this.hasCrisis = hasCrisis;
                return this;
            }

            /** Which sections were completed, partly answered, or not started. */
            public Builder coverage(List<SectionStatus> assessed, List<SectionStatus> partial,
                                    List<SectionStatus> notStarted) {
                this.assessed = assessed;
                this.partial = partial;
                this.notStarted = notStarted;
                return this;
            }

            /** Whether the Depression and Mood (PHQ-9) section is missing. */
            public Builder depressionMissing(boolean depressionMissing) {
                this.depressionMissing = depressionMissing;
                return this;
            }

            /** The "Your answers" list. */
            public Builder answers(List<SectionAnswers> answers) {
                this.answers = answers;
                return this;
            }

            /**
             * @return the finished, unchangeable report
             * @throws IllegalStateException if the overall result was not set
             */
            public ScreeningReport build() {
                if (overallKey == null || overallLabel == null) {
                    throw new IllegalStateException("The overall result must be set before build()");
                }
                return new ScreeningReport(List.copyOf(results), List.copyOf(guidance), List.copyOf(reasoning),
                    overallKey, overallLabel, overallNote, hasCrisis,
                    List.copyOf(assessed), List.copyOf(partial), List.copyOf(notStarted),
                    depressionMissing, List.copyOf(answers));
            }
        }
    }

    // ------------------------------------------------------------------ build

    public ScreeningReport buildReport(RuleEngine.RuleEngineResult engineResult,
                                       PreparedAssessment assessment,
                                       Map<String, String> answers) {
        List<Finding> findings = engineResult.findings();
        Map<String, Integer> facts = assessment.workingFacts();

        boolean hasCrisis = findings.stream()
            .anyMatch(f -> f.actionType() == ActionType.CRISIS_PROTOCOL);
        boolean depressionComplete = assessment.isComplete(Section.PHQ9);

        // ---- Overall result (banner) ----
        boolean anyPartial = assessment.statuses().values().stream().anyMatch(SectionStatus::isPartial);
        OverallResultResolver.OverallResult overall =
            overallResolver.resolve(engineResult.overallSeverity(), hasCrisis, depressionComplete, anyPartial);

        // ---- Lookups for readable text ----
        Map<String, Question> questionsByKey = new HashMap<>();
        for (Question q : questionRepository.findAll()) questionsByKey.putIfAbsent(q.fieldKey(), q);

        List<SectionView> sectionViews = questionnaireService.getSections();
        Map<String, QuestionView> viewsByKey = new HashMap<>();
        for (SectionView sv : sectionViews) for (QuestionView qv : sv.questions()) viewsByKey.put(qv.fieldKey(), qv);

        Map<String, String> conclusionNames = new HashMap<>();
        Set<String> crisisConclusions = new HashSet<>();
        Map<String, Rule> rulesById = new HashMap<>();
        for (Rule r : ruleRepository.getActiveRules()) {
            rulesById.put(r.ruleId(), r);
            if (r.conclusionKey() != null) {
                conclusionNames.putIfAbsent(r.conclusionKey(), r.name());
                if (r.actionType() == ActionType.CRISIS_PROTOCOL) crisisConclusions.add(r.conclusionKey());
            }
        }

        // ---- Findings with readable basis and tone ----
        List<FindingView> findingViews = new ArrayList<>();
        for (Finding f : findings) {
            List<BasisLine> basis = new ArrayList<>();
            boolean safetyLinked = false;
            for (ConditionTrace t : f.trace()) {
                basis.add(basisLine(t, questionsByKey, viewsByKey, conclusionNames, answers));
                if (styler.isSafetySignal(t, crisisConclusions)) safetyLinked = true;
            }
            FindingStyler.Style style = styler.style(f, safetyLinked, rulesById.get(f.ruleId()));
            findingViews.add(new FindingView(f, basis, style.tone(), style.icon(), style.action()));
        }

        // ---- Group findings by audience ----
        // - overall_severity rules are shown in the banner, so they are not repeated as cards
        // - anything safety-related stays in "results", whatever its source
        // - NICE recommendations are written for professionals → "guidance"
        // - pathway gates and the NICE synthesis are engine steps → "reasoning"
        List<FindingView> results = new ArrayList<>();
        List<FindingView> guidance = new ArrayList<>();
        List<FindingView> reasoning = new ArrayList<>();
        for (FindingView fv : findingViews) {
            Rule rule = rulesById.get(fv.finding().ruleId());
            String conclusion = rule != null && rule.conclusionKey() != null ? rule.conclusionKey() : "";
            String source = fv.finding().sourceRef() != null ? fv.finding().sourceRef() : "";
            if (conclusion.equals(RuleEngine.OVERALL_SEVERITY_KEY)) continue;
            if (fv.tone().equals(FindingStyler.CRISIS_TONE)) results.add(fv);
            else if (conclusion.endsWith("_unlocked") || conclusion.equals("nice_overall")) reasoning.add(fv);
            else if (source.startsWith("NICE")) guidance.add(fv);
            else results.add(fv);
        }
        // Most serious first; the sort is stable, so rule priority order is kept within a tone
        Comparator<FindingView> bySeriousness = Comparator.comparingInt(fv -> styler.seriousness(fv.tone()));
        results.sort(bySeriousness);
        guidance.sort(bySeriousness);

        // ---- Section coverage ----
        List<SectionStatus> statuses = List.copyOf(assessment.statuses().values());
        List<SectionStatus> assessed = statuses.stream().filter(SectionStatus::isComplete).toList();
        List<SectionStatus> partial = statuses.stream().filter(SectionStatus::isPartial).toList();
        List<SectionStatus> notStarted = statuses.stream().filter(SectionStatus::isNotStarted).toList();

        // ---- Your answers (completed and partly answered sections) ----
        List<SectionAnswers> sectionAnswers = new ArrayList<>();
        for (SectionView sv : sectionViews) {
            SectionStatus status = assessment.statuses().get(sv.section());
            if (status == null || status.isNotStarted()) continue;

            List<AnswerRow> rows = new ArrayList<>();
            int n = 1;
            for (QuestionView qv : sv.questions()) {
                String raw = answers.get(qv.fieldKey());
                rows.add(new AnswerRow(n++, qv.text(),
                    raw == null ? "Not answered" : answerText(qv, raw), raw != null));
            }
            // Each questionnaire describes its own score (polymorphism, no switch on the section)
            Questionnaire questionnaire = questionnaires.get(sv.section());
            String score = status.isComplete() ? questionnaire.summary(facts, answers) : null;
            String scoreNote = status.isComplete() ? questionnaire.scoreNote() : null;
            sectionAnswers.add(new SectionAnswers(status, score, scoreNote, rows));
        }

        return ScreeningReport.builder()
            .overall(overall.key(), overall.label(), overall.note())
            .crisis(hasCrisis)
            .findings(results, guidance, reasoning)
            .coverage(assessed, partial, notStarted)
            .depressionMissing(!depressionComplete)
            .answers(sectionAnswers)
            .build();
    }

    // ------------------------------------------------------------------ helpers

    private BasisLine basisLine(ConditionTrace t, Map<String, Question> questionsByKey,
                                Map<String, QuestionView> viewsByKey, Map<String, String> conclusionNames,
                                Map<String, String> answers) {
        String key = t.fieldKey();
        String needed = t.requiredValue();

        // 1. One of the user's own answers
        QuestionView qv = viewsByKey.get(key);
        if (qv != null) {
            String raw = answers.getOrDefault(key, t.actualValue());
            // Safety Check: every graded Yes counts as Yes, so the requirement is "any Yes"
            String need = key.startsWith(AssessmentService.GRADED_YES_PREFIX)
                ? ("== 1".equals(needed) ? "Any Yes answer" : "== 0".equals(needed) ? "No" : needed)
                : neededText(qv, needed);
            return new BasisLine(qv.text(), answerText(qv, raw), need, false);
        }

        // 2. A conclusion set by an earlier rule (forward chaining)
        if (conclusionNames.containsKey(key)) {
            return new BasisLine("Earlier finding", conclusionNames.get(key), null, true);
        }

        // 3. A calculated score or flag
        Question q = questionsByKey.get(key);
        String label = FRIENDLY_LABELS.getOrDefault(key, q != null ? q.text() : key);
        boolean binary = q != null && "BINARY".equals(q.scaleType());
        if (binary) {
            return new BasisLine(label, "1".equals(t.actualValue()) ? "Yes" : "No",
                needed.equals("== 1") ? "Yes" : needed.equals("== 0") ? "No" : readableNeed(needed), false);
        }
        return new BasisLine(label, t.actualValue(), readableNeed(needed), false);
    }

    /** ">= 16" → "16 or more", "15 - 19" → "between 15 and 19", "in [1,2]" → "one of 1, 2". */
    private static String readableNeed(String needed) {
        if (needed == null) return null;
        String n = needed.trim();
        Matcher m = BETWEEN_NEED.matcher(n);
        if (m.matches()) return "between " + m.group(1) + " and " + m.group(2);
        String[] p = n.split("\\s+", 2);
        if (p.length != 2) return n;
        return switch (p[0]) {
            case "==" -> p[1];
            case "!=" -> "not " + p[1];
            case ">=" -> p[1] + " or more";
            case "<=" -> p[1] + " or less";
            case ">" -> "more than " + p[1];
            case "<" -> "less than " + p[1];
            case "in" -> "one of " + p[1].replaceAll("[\\[\\]]", "").replace(",", ", ");
            default -> n;
        };
    }

    /** "Nearly every day" for choices, "23:30" for times, "45 minutes" for numbers. */
    private static String answerText(QuestionView qv, String raw) {
        if ("choice".equals(qv.kind())) {
            for (Option o : qv.options()) {
                if (o.value().equals(raw)) {
                    // 0–8 scale: the label for in-between values is just the number
                    return o.label().equals(o.value()) ? raw : o.label();
                }
            }
            return raw;
        }
        if ("number".equals(qv.kind()) && qv.unit() != null) return raw + " " + qv.unit();
        return raw;
    }

    /** Turns a numeric requirement on a choice question into its answer label, e.g. ">= 2" → "More than half the days or more". */
    private static String neededText(QuestionView qv, String needed) {
        if (!"choice".equals(qv.kind()) || needed == null) return readableNeed(needed);
        String[] parts = needed.trim().split("\\s+");
        if (parts.length != 2) return readableNeed(needed);
        String target = null;
        for (Option o : qv.options()) if (o.value().equals(parts[1])) target = o.label();
        if (target == null || target.equals(parts[1])) return readableNeed(needed);
        return switch (parts[0]) {
            case "==" -> target;
            case ">=" -> target + " or more";
            case "<=" -> target + " or less";
            default -> readableNeed(needed);
        };
    }

}
