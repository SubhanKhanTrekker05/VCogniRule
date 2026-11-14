package com.ooad.mhs.service;

import com.ooad.mhs.domain.ConditionTrace;
import com.ooad.mhs.domain.Finding;
import com.ooad.mhs.domain.Question;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.engine.RuleEngine;
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
 */
@Service
public class ReportService {

    private final QuestionRepository questionRepository;
    private final QuestionnaireService questionnaireService;
    private final RuleRepository ruleRepository;

    public ReportService(QuestionRepository questionRepository,
                         QuestionnaireService questionnaireService,
                         RuleRepository ruleRepository) {
        this.questionRepository = questionRepository;
        this.questionnaireService = questionnaireService;
        this.ruleRepository = ruleRepository;
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

    /**
     * Facts that signal a safety risk when their value is not 0.
     * A finding whose basis contains one of these (or a conclusion of a
     * CRISIS_PROTOCOL rule) is styled as crisis, whatever its action type.
     */
    private static final Set<String> RISK_FACTS = Set.of(
        "crisis_detected", "cssrs_passive_ideation", "cssrs_any_positive", "phq9_q9",
        "cssrs_q1", "cssrs_q2", "cssrs_q3", "cssrs_q4", "cssrs_q5", "cssrs_q6");

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
     */
    public record ScreeningReport(
        List<FindingView> findings,
        String overallKey,
        String overallLabel,
        String overallNote,
        boolean hasCrisis,
        List<SectionStatus> assessed,
        List<SectionStatus> partial,
        List<SectionStatus> notStarted,
        boolean depressionMissing,
        List<SectionAnswers> answers
    ) {}

    // ------------------------------------------------------------------ build

    public ScreeningReport buildReport(RuleEngine.RuleEngineResult engineResult,
                                       PreparedAssessment assessment,
                                       Map<String, String> answers) {
        List<Finding> findings = engineResult.findings();
        Map<String, Integer> facts = assessment.workingFacts();

        boolean hasCrisis = findings.stream()
            .anyMatch(f -> f.actionType() == ActionType.CRISIS_PROTOCOL);
        boolean depressionComplete = assessment.isComplete(Section.PHQ9);

        // ---- Overall result ----
        // The rule base has five overall rules (L4). When none fires, there is no
        // basis for a severity, so we never fall back to "minimal". A crisis
        // finding always outranks a reassuring overall label.
        String severity = engineResult.overallSeverity();
        String key;
        String label;
        String note = null;
        if (hasCrisis && (severity == null || severity.equals("mild") || severity.equals("minimal")
                || severity.equals("moderate"))) {
            key = "attention";
            label = "Needs attention";
            note = "One or more answers suggest a safety concern. This takes priority over every other result.";
        } else if (severity != null) {
            key = severity;
            label = severity;
        } else if (!depressionComplete) {
            key = "undetermined";
            label = "Not determined";
            note = "The overall result needs the Depression & Mood section (see below).";
        } else {
            key = "unclassified";
            label = "Not classified";
            note = "Your answers did not match one of the overall patterns. Please read the individual findings below.";
        }

        // ---- Lookups for readable text ----
        Map<String, Question> questionsByKey = new HashMap<>();
        for (Question q : questionRepository.findAll()) questionsByKey.putIfAbsent(q.fieldKey(), q);

        List<SectionView> sectionViews = questionnaireService.getSections();
        Map<String, QuestionView> viewsByKey = new HashMap<>();
        for (SectionView sv : sectionViews) for (QuestionView qv : sv.questions()) viewsByKey.put(qv.fieldKey(), qv);

        Map<String, String> conclusionNames = new HashMap<>();
        Set<String> crisisConclusions = new HashSet<>();
        for (Rule r : ruleRepository.getActiveRules()) {
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
                boolean positive = !"0".equals(t.actualValue());
                if (positive && (RISK_FACTS.contains(t.fieldKey()) || crisisConclusions.contains(t.fieldKey()))) {
                    safetyLinked = true;
                }
            }
            findingViews.add(styled(f, basis, safetyLinked));
        }

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
            String score = status.isComplete() ? scoreText(sv.section(), facts, answers) : null;
            String scoreNote = status.isComplete() ? scoreNote(sv.section()) : null;
            sectionAnswers.add(new SectionAnswers(status, score, scoreNote, rows));
        }

        return new ScreeningReport(findingViews, key, label, note, hasCrisis,
            assessed, partial, notStarted, !depressionComplete, sectionAnswers);
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Colour/icon of a finding:
     * 1. Safety-linked (basis contains a positive risk signal) or a crisis rule → crisis.
     * 2. Otherwise by the rule's action type (how serious the recommendation is).
     * Priority is NOT used: it orders findings, it does not say how serious they are.
     */
    private static FindingView styled(Finding f, List<BasisLine> basis, boolean safetyLinked) {
        ActionType a = f.actionType();
        String action = switch (a) {
            case CRISIS_PROTOCOL -> "Crisis support";
            case URGENT_ASSESSMENT -> "Urgent assessment";
            case RECOMMEND_ASSESSMENT -> "Assessment recommended";
            case SUGGEST_PROFESSIONAL -> "Consider professional support";
            case MONITOR -> "Monitor";
            case DISPLAY_INFO -> "Information";
        };
        if (a == ActionType.CRISIS_PROTOCOL || safetyLinked) {
            if (a != ActionType.CRISIS_PROTOCOL) action += " · safety-related";
            return new FindingView(f, basis, "crisis", "⚠", action);
        }
        return switch (a) {
            case URGENT_ASSESSMENT -> new FindingView(f, basis, "urgent", "⚠", action);
            case RECOMMEND_ASSESSMENT -> new FindingView(f, basis, "recommend", "!", action);
            case SUGGEST_PROFESSIONAL -> new FindingView(f, basis, "suggest", "!", action);
            case MONITOR -> new FindingView(f, basis, "monitor", "•", action);
            default -> new FindingView(f, basis, "info", "✓", action);
        };
    }

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
        String label = q != null ? q.text() : key;
        boolean binary = q != null && "BINARY".equals(q.scaleType());
        if (binary) {
            return new BasisLine(label, "1".equals(t.actualValue()) ? "Yes" : "No",
                needed.equals("== 1") ? "Yes" : needed.equals("== 0") ? "No" : needed, false);
        }
        return new BasisLine(label, t.actualValue(), needed, false);
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
        if (!"choice".equals(qv.kind()) || needed == null) return needed;
        String[] parts = needed.trim().split("\\s+");
        if (parts.length != 2) return needed;
        String target = null;
        for (Option o : qv.options()) if (o.value().equals(parts[1])) target = o.label();
        if (target == null || target.equals(parts[1])) return needed;
        return switch (parts[0]) {
            case "==" -> target;
            case ">=" -> target + " or more";
            case "<=" -> target + " or less";
            default -> needed;
        };
    }

    private static String scoreText(Section section, Map<String, Integer> f, Map<String, String> answers) {
        return switch (section) {
            case PHQ9 -> "Total score " + f.get("phq9_total") + " / 27";
            case GAD7 -> "Total score " + f.get("gad7_total") + " / 21";
            case PHQ15 -> "Total score " + f.get("phq15_total") + " / 30";
            case CSSRS -> countYes(answers, "cssrs_q", 6) + " of 6 answered Yes";
            case WHO5 -> "Wellbeing score " + f.get("who5_raw") + " / 25 (" + (f.get("who5_raw") * 4) + "%)";
            case PSS10 -> "Total score " + f.get("pss10_total") + " / 40";
            case PSQI -> "Global score " + f.get("psqi_global") + " / 21";
            case WSAS -> "Total score " + f.get("wsas_total") + " / 40";
            case ASRS -> f.get("asrs_shaded_count") + " of 6 answers in the screening range";
            case PCPTSD -> countYes(answers, "pcptsd_q", 4) + " of 4 answered Yes";
        };
    }

    private static String scoreNote(Section section) {
        return switch (section) {
            case WHO5 -> "Higher is better. 50% or below suggests low wellbeing.";
            case PSQI -> "Lower is better. Above 5 suggests poor sleep.";
            case CSSRS -> "Any Yes answer is followed up in the findings.";
            case ASRS -> "4 or more suggests symptoms worth discussing with a professional.";
            case PCPTSD -> "3 or more suggests trauma-related symptoms.";
            default -> "Higher means more symptoms.";
        };
    }

    /** Counts Yes answers: any answered value other than "0" (covers graded Safety Check answers). */
    private static int countYes(Map<String, String> answers, String prefix, int count) {
        int yes = 0;
        for (int i = 1; i <= count; i++) {
            String v = answers.get(prefix + i);
            if (v != null && !"0".equals(v)) yes++;
        }
        return yes;
    }
}
