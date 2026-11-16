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

    /** The three groups of finding cards (parameter object). */
    public record FindingGroups(
        List<FindingView> results,      // about the user; safety-related always here, most serious first
        List<FindingView> guidance,     // NICE recommendations written for professionals
        List<FindingView> reasoning     // intermediate engine steps (pathway gates, NICE synthesis)
    ) {
        public FindingGroups {
            results = List.copyOf(results);
            guidance = List.copyOf(guidance);
            reasoning = List.copyOf(reasoning);
        }
    }

    /** One row of "Your scores": a completed section, its score text, and the band from its rules. */
    public record ScoreRow(Section section, String score, String band, ScoreBar bar) {}

    /**
     * A coloured bar for one score, built from the section's existing band rules (no new scoring).
     *
     * @param value    the user's score
     * @param max      the top of the scale
     * @param segments one part per band rule, with its range and colour step (0 = green … 3 = red)
     */
    public record ScoreBar(int value, int max, List<BarSegment> segments) {
        public ScoreBar { segments = List.copyOf(segments); }
        /** Where the marker sits, as a CSS percentage (middle of the score's slot). */
        public double markerPercent() { return (value + 0.5) * 100.0 / (max + 1); }
    }

    /** One band on the score bar: scores from..to (inclusive), the band name, and its colour step 0–3. */
    public record BarSegment(int from, int to, String name, int colour) {
        public double widthPercent(int max) { return (to - from + 1) * 100.0 / (max + 1); }
    }

    /**
     * Display-only extras for the report page (parameter object). Nothing here changes
     * which findings fired, the crisis box or the banner.
     *
     * @param actionSteps   the "What to do now" steps for the overall result
     * @param scores        one row per completed section
     * @param safetyItems   the C-SSRS crisis findings, shown together as one "Safety concern" card
     * @param otherResults  the remaining "Your results" cards (results minus safetyItems)
     */
    public record ReportExtras(List<String> actionSteps, List<ScoreRow> scores,
                               List<FindingView> safetyItems, List<FindingView> otherResults,
                               List<TopicGroup> topics) {
        public ReportExtras {
            actionSteps = List.copyOf(actionSteps);
            scores = List.copyOf(scores);
            safetyItems = List.copyOf(safetyItems);
            otherResults = List.copyOf(otherResults);
            topics = List.copyOf(topics);
        }
    }

    /**
     * "Your results" cards under one topic heading (display grouping only).
     *
     * @param name        Safety, Mood, Anxiety, … Combined
     * @param safetyItems the grouped "Safety concern" items (only in the Safety topic)
     * @param cards       the other cards of this topic, in the same order as before
     */
    public record TopicGroup(String name, List<FindingView> safetyItems, List<FindingView> cards) {
        public TopicGroup {
            safetyItems = List.copyOf(safetyItems);
            cards = List.copyOf(cards);
        }
    }

    /** Which sections were completed, partly answered, or not started (parameter object). */
    public record Coverage(List<SectionStatus> assessed, List<SectionStatus> partial, List<SectionStatus> notStarted) {
        public Coverage {
            assessed = List.copyOf(assessed);
            partial = List.copyOf(partial);
            notStarted = List.copyOf(notStarted);
        }
    }

    /**
     * The complete report data passed to the Thymeleaf template.
     *
     * Refactoring "Introduce Parameter Object": the 12 values are grouped into
     * OverallResult, FindingGroups and Coverage, so the constructor has 6 parameters.
     * The short accessor methods below keep the page template unchanged.
     *
     * @param overall           the overall banner (key, label, optional note)
     * @param hasCrisis         true if a crisis rule fired (shows the helpline box)
     * @param findings          the three groups of finding cards
     * @param coverage          which sections were assessed, partly answered, not started
     * @param depressionMissing true when the PHQ-9 (Depression) section was not completed
     * @param answers           the "Your answers" list
     * @param extras            display-only parts: action steps, scores, the grouped safety card
     */
    public record ScreeningReport(
        OverallResultResolver.OverallResult overall,
        boolean hasCrisis,
        FindingGroups findings,
        Coverage coverage,
        boolean depressionMissing,
        List<SectionAnswers> answers,
        ReportExtras extras
    ) {
        public ScreeningReport {
            java.util.Objects.requireNonNull(overall, "overall");
            java.util.Objects.requireNonNull(extras, "extras");
            java.util.Objects.requireNonNull(findings, "findings");
            java.util.Objects.requireNonNull(coverage, "coverage");
            answers = List.copyOf(answers);
        }

        /** A report without the display extras (no action steps, scores or grouped safety card). */
        public ScreeningReport(OverallResultResolver.OverallResult overall, boolean hasCrisis,
                               FindingGroups findings, Coverage coverage,
                               boolean depressionMissing, List<SectionAnswers> answers) {
            this(overall, hasCrisis, findings, coverage, depressionMissing, answers,
                new ReportExtras(List.of(), List.of(), List.of(),
                    findings == null ? List.of() : findings.results(), List.of()));
        }

        public String overallKey() { return overall.key(); }
        public String overallLabel() { return overall.label(); }
        public String overallNote() { return overall.note(); }
        public List<FindingView> results() { return findings.results(); }
        public List<FindingView> guidance() { return findings.guidance(); }
        public List<FindingView> reasoning() { return findings.reasoning(); }
        public List<SectionStatus> assessed() { return coverage.assessed(); }
        public List<SectionStatus> partial() { return coverage.partial(); }
        public List<SectionStatus> notStarted() { return coverage.notStarted(); }
        public List<String> actionSteps() { return extras.actionSteps(); }
        public List<ScoreRow> scores() { return extras.scores(); }
        public List<FindingView> safetyItems() { return extras.safetyItems(); }
        public List<FindingView> otherResults() { return extras.otherResults(); }
        public List<TopicGroup> topics() { return extras.topics(); }
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
            // Engine steps (pathway gates such as GATE-CRISIS, and the NICE synthesis) always go to
            // "How the system reasoned", even when crisis-coloured. Display grouping only: the crisis
            // box and banner are decided from ALL findings above, so they do not change.
            if (conclusion.endsWith("_unlocked") || conclusion.equals("nice_overall")) reasoning.add(fv);
            else if (fv.tone().equals(FindingStyler.CRISIS_TONE)) results.add(fv);
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

        // ---- Your answers (completed and partly answered sections) and Your scores ----
        List<SectionAnswers> sectionAnswers = new ArrayList<>();
        List<ScoreRow> scores = new ArrayList<>();
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
            if (status.isComplete()) {
                scores.add(new ScoreRow(sv.section(), score, band(sv.section(), findings, rulesById),
                    scoreBar(sv.section(), facts, rulesById.values())));
            }
        }

        // ---- Display grouping: all C-SSRS crisis cards become one "Safety concern" card ----
        // (results itself is unchanged; this only decides how the page lays the cards out)
        List<FindingView> safetyItems = new ArrayList<>();
        List<FindingView> otherResults = new ArrayList<>();
        for (FindingView fv : results) {
            boolean cssrsCrisis = fv.finding().ruleId().startsWith(CSSRS_RULE_PREFIX)
                && fv.tone().equals(FindingStyler.CRISIS_TONE);
            if (cssrsCrisis) safetyItems.add(fv);
            else otherResults.add(fv);
        }

        return new ScreeningReport(overall, hasCrisis,
            new FindingGroups(results, guidance, reasoning),
            new Coverage(assessed, partial, notStarted),
            !depressionComplete, sectionAnswers,
            new ReportExtras(actionSteps(overall.key(), hasCrisis), scores, safetyItems, otherResults,
                topics(safetyItems, otherResults, rulesById.values())));
    }

    // ------------------------------------------------------------------ helpers

    private static final String CSSRS_RULE_PREFIX = "CSSRS-";

    /** The calculated score that each section's band rules check (the same facts the summaries print). */
    private static final Map<Section, String> SCORE_KEYS = Map.of(
        Section.PHQ9, "phq9_total", Section.GAD7, "gad7_total", Section.PHQ15, "phq15_total",
        Section.WHO5, "who5_raw", Section.PSS10, "pss10_total", Section.PSQI, "psqi_global",
        Section.WSAS, "wsas_total", Section.ASRS, "asrs_shaded_count", Section.PCPTSD, "pcptsd_total");

    /**
     * The band of a section, read from the rules that fired (no new scoring): the Level 1 rule of
     * that questionnaire whose only condition is on the section's score. If two match (PSQI "poor"
     * and "severe"), the higher-priority rule wins. C-SSRS has no score: any crisis rule gives
     * "Safety concern", otherwise its non-crisis rule ("No suicide risk indicators").
     * Returns null when no band rule fired.
     */
    /** Topic headings of "Your results", in display order. */
    public static final List<String> TOPICS = List.of(
        "Safety", "Mood", "Anxiety", "Stress", "Sleep", "Physical", "Trauma", "Attention", "Daily life", "Combined");

    /** Field-key prefix → topic (the questionnaire the answer or score belongs to). */
    private static final Map<String, String> KEY_TOPICS = Map.ofEntries(
        Map.entry("cssrs_", "Safety"), Map.entry("crisis_detected", "Safety"),
        Map.entry("phq15_", "Physical"), Map.entry("phq9_", "Mood"), Map.entry("who5_", "Mood"),
        Map.entry("gad7_", "Anxiety"), Map.entry("pss10_", "Stress"), Map.entry("psqi_", "Sleep"),
        Map.entry("pcptsd_", "Trauma"), Map.entry("asrs_", "Attention"), Map.entry("wsas_", "Daily life"));

    /**
     * Puts the result cards under topic headings, keeping their order inside each topic.
     * Crisis-coloured cards → Safety. Otherwise the topic of the questionnaire(s) the rule's
     * matched conditions use (an earlier finding counts as the questionnaire its rule used);
     * more than one questionnaire, or unknown → Combined.
     */
    static List<TopicGroup> topics(List<FindingView> safetyItems, List<FindingView> cards, Collection<Rule> rules) {
        Map<String, Rule> byConclusion = new HashMap<>();
        for (Rule r : rules) if (r.conclusionKey() != null) byConclusion.putIfAbsent(r.conclusionKey(), r);

        Map<String, List<FindingView>> byTopic = new LinkedHashMap<>();
        for (FindingView fv : cards) {
            String topic;
            if (fv.tone().equals(FindingStyler.CRISIS_TONE)) {
                topic = "Safety";
            } else {
                Set<String> found = new HashSet<>();
                for (ConditionTrace t : fv.finding().trace()) {
                    String direct = keyTopic(t.fieldKey());
                    if (direct != null) { found.add(direct); continue; }
                    Rule earlier = byConclusion.get(t.fieldKey());
                    if (earlier == null || earlier.groups() == null) { found.add("Combined"); continue; }
                    for (var g : earlier.groups()) for (var c : g.conditions()) {
                        String t2 = keyTopic(c.fieldKey());
                        found.add(t2 != null ? t2 : "Combined");
                    }
                }
                topic = found.size() == 1 ? found.iterator().next() : "Combined";
            }
            byTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(fv);
        }

        List<TopicGroup> groups = new ArrayList<>();
        for (String name : TOPICS) {
            List<FindingView> list = byTopic.getOrDefault(name, List.of());
            List<FindingView> safety = name.equals("Safety") ? safetyItems : List.of();
            if (!list.isEmpty() || !safety.isEmpty()) groups.add(new TopicGroup(name, safety, list));
        }
        return groups;
    }

    private static String keyTopic(String key) {
        if (key == null) return null;
        for (var e : KEY_TOPICS.entrySet()) if (key.startsWith(e.getKey())) return e.getValue();
        return null;
    }

    /** Top of each score's scale (the same maximums the summaries and labels print). */
    private static final Map<Section, Integer> SCORE_MAX = Map.of(
        Section.PHQ9, 27, Section.GAD7, 21, Section.PHQ15, 30, Section.WHO5, 25, Section.PSS10, 40,
        Section.PSQI, 21, Section.WSAS, 40, Section.ASRS, 6, Section.PCPTSD, 4);

    /**
     * The score bar for a section, read from its active Level 1 band rules (one condition on the
     * section's score). Each score point belongs to the highest-priority band rule covering it;
     * colours follow the rules' priority order (lowest priority = green, highest = red).
     * Returns null when the section has no score or no band rules.
     */
    static ScoreBar scoreBar(Section section, Map<String, Integer> facts, Collection<Rule> rules) {
        String key = SCORE_KEYS.get(section);
        Integer max = SCORE_MAX.get(section);
        Integer value = key == null ? null : facts.get(key);
        if (max == null || value == null) return null;
        String prefix = section.getCode().replace("-", "") + "-";

        // band rule → its score range [lo, hi]
        Map<Rule, int[]> ranges = new LinkedHashMap<>();
        for (Rule r : rules) {
            if (r.level() == null || r.level() != 1 || !r.ruleId().startsWith(prefix) || r.groups() == null
                    || r.groups().size() != 1 || r.groups().get(0).conditions().size() != 1) continue;
            var c = r.groups().get(0).conditions().get(0);
            if (!key.equals(c.fieldKey())) continue;
            int[] range = range(c, max);
            if (range != null && range[0] <= range[1]) ranges.put(r, range);
        }
        if (ranges.isEmpty()) return null;

        List<Rule> byPriority = new ArrayList<>(ranges.keySet());
        byPriority.sort(Comparator.comparingInt(r -> r.priority() == null ? 0 : r.priority()));
        int steps = byPriority.size() - 1;

        List<BarSegment> segments = new ArrayList<>();
        Rule current = null;
        int start = 0;
        for (int point = 0; point <= max + 1; point++) {
            Rule owner = null;
            if (point <= max) {
                for (Rule r : byPriority) {               // ascending, so the last match has the highest priority
                    int[] g = ranges.get(r);
                    if (point >= g[0] && point <= g[1]) owner = r;
                }
            }
            if (owner != current) {
                if (current != null) {
                    int colour = steps == 0 ? 0 : (int) Math.round(byPriority.indexOf(current) * 3.0 / steps);
                    segments.add(new BarSegment(start, point - 1, current.name(), colour));
                }
                current = owner;
                start = point;
            }
        }
        return segments.isEmpty() ? null : new ScoreBar(Math.min(Math.max(value, 0), max), max, segments);
    }

    private static int[] range(com.ooad.mhs.domain.Condition c, int max) {
        if (c.operator() == null) return null;
        Integer v = c.valueInt();
        return switch (c.operator()) {
            case BETWEEN -> c.valueMin() == null || c.valueMax() == null ? null
                : new int[] {Math.max(c.valueMin(), 0), Math.min(c.valueMax(), max)};
            case EQ -> v == null ? null : new int[] {v, v};
            case GTE -> v == null ? null : new int[] {v, max};
            case GT -> v == null ? null : new int[] {v + 1, max};
            case LTE -> v == null ? null : new int[] {0, v};
            case LT -> v == null ? null : new int[] {0, v - 1};
            default -> null;
        };
    }

    private static String band(Section section, List<Finding> findings, Map<String, Rule> rulesById) {
        String prefix = section.getCode().replace("-", "") + "-";
        boolean cssrs = section == Section.CSSRS;
        if (cssrs && findings.stream().anyMatch(f -> f.ruleId().startsWith(prefix)
                && f.actionType() == ActionType.CRISIS_PROTOCOL)) {
            return "Safety concern";
        }
        String scoreKey = SCORE_KEYS.get(section);
        Finding best = null;
        for (Finding f : findings) {
            if (f.level() == null || f.level() != 1 || !f.ruleId().startsWith(prefix)) continue;
            boolean bandRule = cssrs
                ? f.actionType() != ActionType.CRISIS_PROTOCOL
                : scoreKey != null && f.trace().size() == 1 && scoreKey.equals(f.trace().get(0).fieldKey());
            if (bandRule && (best == null || priority(f, rulesById) > priority(best, rulesById))) best = f;
        }
        return best == null ? null : best.name();
    }

    private static int priority(Finding f, Map<String, Rule> rulesById) {
        Rule r = rulesById.get(f.ruleId());
        Integer p = r != null ? r.priority() : f.priority();
        return p == null ? 0 : p;
    }

    /** "What to do now": plain steps for the overall result. A crisis always gets the crisis steps. */
    static List<String> actionSteps(String overallKey, boolean hasCrisis) {
        if (hasCrisis || "critical".equals(overallKey) || "attention".equals(overallKey)) {
            return List.of(
                "Call one of these numbers now.",
                "Talk to someone you trust today: a friend, a family member or a teacher.",
                "Do not stay alone right now. Stay with someone, or somewhere safe.",
                "See a doctor or mental health professional urgently.");
        }
        return switch (overallKey) {
            case "severe", "moderate" -> List.of(
                "Book a visit with a doctor or counsellor soon.",
                "Share this report with them (use Save as PDF at the bottom).",
                "Take healthy steps: regular sleep, meals, some movement, and time with people you trust.");
            case "mild" -> List.of(
                "Watch how you feel over the next few weeks.",
                "Keep healthy habits: sleep, meals, movement, and talking to people you trust.",
                "Do this screening again in a few weeks, or sooner if you feel worse.");
            case "minimal" -> List.of(
                "Keep up your healthy habits.",
                "Do this screening again if things change.");
            default -> List.of(
                "Complete the missing sections so the report can give an overall result.",
                "Read your results below.",
                "If you are worried about how you feel, talk to a doctor or counsellor.");
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
