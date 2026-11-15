package com.ooad.mhs.service;

import com.ooad.mhs.questionnaire.QuestionnaireRegistry;
import com.ooad.mhs.service.AssessmentService.PreparedAssessment;
import com.ooad.mhs.service.QuestionnaireService.QuestionView;
import com.ooad.mhs.util.Section;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for answer validation and preparation (bad answers count as not answered). */
class AssessmentServiceTest {

    private final QuestionnaireService questionnaireService = new QuestionnaireService(new Fakes.FakeQuestionRepository());
    private final AssessmentService service = new AssessmentService(
        new DerivedFieldCalculator(QuestionnaireRegistry.createDefault()), questionnaireService);

    private QuestionView view(String key) {
        return questionnaireService.getSections().stream().flatMap(s -> s.questions().stream())
            .filter(q -> q.fieldKey().equals(key)).findFirst().orElseThrow();
    }

    private static Map<String, String> phq9(String... values) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < values.length; i++) m.put("phq9_q" + (i + 1), values[i]);
        return m;
    }

    private static Map<String, String> cssrs(String... values) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < values.length; i++) m.put("cssrs_q" + (i + 1), values[i]);
        return m;
    }

    @DisplayName("isValid: each answer must fit its question")
    @ParameterizedTest(name = "{0} = \"{1}\" -> {2}")
    @CsvSource({
        // radio buttons (0-3)
        "phq9_q1, 0, true", "phq9_q1, 3, true", "phq9_q1, 4, false", "phq9_q1, -1, false",
        "phq9_q1, abc, false", "phq9_q1, 1.5, false",
        // time input
        "psqi_q1, 23:00, true", "psqi_q1, 7:05, true", "psqi_q1, 25:00, false", "psqi_q1, 23:60, false",
        "psqi_q1, 11pm, false",
        // whole number 0-180
        "psqi_q2, 0, true", "psqi_q2, 180, true", "psqi_q2, 181, false", "psqi_q2, 45.5, false", "psqi_q2, abc, false",
        // decimal 0-24
        "psqi_q4, 7.5, true", "psqi_q4, 24, true", "psqi_q4, 30, false", "psqi_q4, -1, false", "psqi_q4, abc, false",
        // Safety Check: any non-blank answer counts (fail-safe)
        "cssrs_q1, 0, true", "cssrs_q1, 3, true", "cssrs_q1, abc, true",
    })
    void isValid(String key, String value, boolean expected) {
        assertThat(AssessmentService.isValid(view(key), value)).isEqualTo(expected);
    }

    @Test
    @DisplayName("A blank answer is never valid")
    void blankIsInvalid() {
        assertThat(AssessmentService.isValid(view("phq9_q1"), "")).isFalse();
        assertThat(AssessmentService.isValid(view("cssrs_q1"), "")).isFalse();
    }

    @Test
    @DisplayName("filterAnswers drops unknown fields, blank and invalid answers, and trims spaces")
    void filterAnswers() {
        Map<String, String> form = new HashMap<>(Map.of("phq9_q1", " 2 ", "phq9_q2", "", "phq9_q3", "9",
            "hack", "1", "phq9_total", "27"));
        assertThat(service.filterAnswers(form)).containsExactly(Map.entry("phq9_q1", "2"));
    }

    @Test
    @DisplayName("A complete section is scored")
    void completeSectionIsScored() {
        PreparedAssessment p = service.prepare(phq9("1", "1", "1", "1", "1", "1", "1", "1", "0"));
        assertThat(p.isComplete(Section.PHQ9)).isTrue();
        assertThat(p.workingFacts()).containsEntry("phq9_total", 8);
    }

    @Test
    @DisplayName("A partly answered section is not scored, but PHQ-9 item 9 (self-harm) is kept")
    void partialSectionNotScoredButSafetyItemKept() {
        Map<String, String> a = phq9("1", "1", "1", "1", "1", "1", "1", "", "2");   // item 8 missing
        PreparedAssessment p = service.prepare(a);
        assertThat(p.statuses().get(Section.PHQ9).isPartial()).isTrue();
        assertThat(p.statuses().get(Section.PHQ9).answered()).isEqualTo(8);
        assertThat(p.workingFacts()).doesNotContainKey("phq9_total").doesNotContainKey("phq9_q1")
            .containsEntry("phq9_q9", 2);
    }

    @Test
    @DisplayName("An invalid answer makes its section partly answered (no made-up value)")
    void invalidAnswerMakesSectionPartial() {
        PreparedAssessment p = service.prepare(phq9("abc", "1", "1", "1", "1", "1", "1", "1", "0"));
        assertThat(p.isComplete(Section.PHQ9)).isFalse();
        assertThat(p.workingFacts()).doesNotContainKey("phq9_total");
    }

    @Test
    @DisplayName("Safety Check: graded answers 1-3 and even unreadable answers count as Yes (1)")
    void safetyCheckFailSafe() {
        PreparedAssessment p = service.prepare(cssrs("3", "abc", "0", "1", "0", "0"));
        assertThat(p.workingFacts()).containsEntry("cssrs_q1", 1).containsEntry("cssrs_q2", 1)
            .containsEntry("cssrs_q3", 0).containsEntry("cssrs_q4", 1).containsEntry("crisis_detected", 1);
    }

    @Test
    @DisplayName("PSQI: times become minutes after midnight, hours are rounded")
    void psqiConversion() {
        Map<String, String> a = new HashMap<>(Map.of("psqi_q1", "23:30", "psqi_q2", "20", "psqi_q3", "06:45", "psqi_q4", "7.5"));
        PreparedAssessment p = service.prepare(a);
        assertThat(p.isComplete(Section.PSQI)).isTrue();
        assertThat(p.workingFacts()).containsEntry("psqi_q1", 23 * 60 + 30).containsEntry("psqi_q3", 6 * 60 + 45)
            .containsEntry("psqi_q4", 8).containsEntry("psqi_q2", 20).containsKey("psqi_global");
    }

    @Test
    @DisplayName("prepare() validates again even if filterAnswers() was skipped")
    void prepareValidatesItself() {
        Map<String, String> a = phq9("0", "0", "0", "0", "0", "0", "0", "0", "99");
        a.put("hack", "1");
        PreparedAssessment p = service.prepare(a);
        assertThat(p.isComplete(Section.PHQ9)).isFalse();
        assertThat(p.workingFacts()).doesNotContainKey("hack").doesNotContainKey("phq9_q9");
    }
}
