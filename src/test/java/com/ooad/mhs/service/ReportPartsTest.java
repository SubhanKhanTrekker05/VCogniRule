package com.ooad.mhs.service;

import com.ooad.mhs.domain.ConditionTrace;
import com.ooad.mhs.domain.Finding;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.service.AssessmentService.SectionStatus;
import com.ooad.mhs.service.ReportService.ScreeningReport;
import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;
import com.ooad.mhs.util.Operator;
import com.ooad.mhs.util.Section;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the report parts: ScreeningReport (parameter objects), OverallResultResolver and FindingStyler. */
class ReportPartsTest {

    @Nested
    @DisplayName("ScreeningReport (Introduce Parameter Object)")
    class ScreeningReportTests {

        private final ReportService.FindingGroups noFindings = new ReportService.FindingGroups(List.of(), List.of(), List.of());

        private static OverallResultResolver.OverallResult overall(String key, String note) {
            return new OverallResultResolver.OverallResult(key, key, note);
        }

        @Test
        @DisplayName("The grouped values are reachable through the short accessors the page uses")
        void accessorsReadTheGroups() {
            List<SectionStatus> done = List.of(new SectionStatus(Section.CSSRS, 6, 6));
            ScreeningReport r = new ScreeningReport(overall("moderate", "a note"), true, noFindings,
                new ReportService.Coverage(done, List.of(), List.of()), true, List.of());
            assertThat(r.overallKey()).isEqualTo("moderate");
            assertThat(r.overallNote()).isEqualTo("a note");
            assertThat(r.hasCrisis()).isTrue();
            assertThat(r.depressionMissing()).isTrue();
            assertThat(r.assessed()).containsExactlyElementsOf(done);
            assertThat(r.results()).isEmpty();
        }

        @Test
        @DisplayName("A report needs its parameter objects (no nulls)")
        void requiresParameterObjects() {
            assertThatThrownBy(() -> new ScreeningReport(null, false, noFindings,
                new ReportService.Coverage(List.of(), List.of(), List.of()), false, List.of()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("The finished report cannot be changed")
        void reportIsUnchangeable() {
            List<SectionStatus> list = new ArrayList<>();
            ScreeningReport r = new ScreeningReport(overall("minimal", null), false, noFindings,
                new ReportService.Coverage(list, List.of(), List.of()), false, List.of());
            list.add(new SectionStatus(Section.PHQ9, 9, 9));        // changing the original list...
            assertThat(r.assessed()).isEmpty();                       // ...does not change the report
            assertThatThrownBy(() -> r.assessed().add(null)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("OverallResultResolver (banner)")
    class OverallTests {
        private final OverallResultResolver resolver = new OverallResultResolver();

        @DisplayName("Crisis outranks a mild / minimal / moderate / missing severity")
        @ParameterizedTest(name = "severity {0} + crisis -> attention")
        @CsvSource(value = {"null", "minimal", "mild", "moderate"}, nullValues = "null")
        void crisisOutranksLowSeverity(String severity) {
            assertThat(resolver.resolve(severity, true, true, false).key()).isEqualTo("attention");
        }

        @Test
        @DisplayName("The crisis note also names the rules' own severity")
        void crisisNoteNamesSeverity() {
            assertThat(resolver.resolve("moderate", true, true, false).note()).contains("MODERATE");
            assertThat(resolver.resolve(null, true, true, false).note()).doesNotContain("Apart from");
        }

        @Test
        @DisplayName("Severe and critical stay as they are, even with a crisis")
        void severeStays() {
            assertThat(resolver.resolve("severe", true, true, false).key()).isEqualTo("severe");
            assertThat(resolver.resolve("critical", true, true, false).key()).isEqualTo("critical");
        }

        @Test
        @DisplayName("A severity with a partly answered section gets a note")
        void partialNote() {
            assertThat(resolver.resolve("minimal", false, true, false).note()).isNull();
            assertThat(resolver.resolve("minimal", false, true, true).note()).contains("Based only on the sections");
        }

        @Test
        @DisplayName("No severity: 'Not determined' without PHQ-9, otherwise 'Not classified'")
        void noSeverity() {
            assertThat(resolver.resolve(null, false, false, false).key()).isEqualTo("undetermined");
            assertThat(resolver.resolve(null, false, true, false).key()).isEqualTo("unclassified");
        }
    }

    @Nested
    @DisplayName("FindingStyler (colour, icon, label)")
    class StylerTests {
        private final FindingStyler styler = new FindingStyler();

        private Finding finding(ActionType a) {
            return new Finding("R", "name", Category.MOOD, 50, a, "x", "src", 1, List.of());
        }

        private Rule rule(String key, String value) {
            return new Rule(1L, "R", "name", 1, Category.MOOD, 50, ActionType.DISPLAY_INFO,
                "x", "src", key, value, true, List.of());
        }

        @DisplayName("Action type decides the tone")
        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"CRISIS_PROTOCOL, crisis", "URGENT_ASSESSMENT, urgent", "RECOMMEND_ASSESSMENT, recommend",
                    "SUGGEST_PROFESSIONAL, suggest", "MONITOR, monitor"})
        void toneByAction(ActionType a, String tone) {
            assertThat(styler.style(finding(a), false, null).tone()).isEqualTo(tone);
        }

        @Test
        @DisplayName("A safety-linked finding is shown as crisis, whatever its action")
        void safetyLinkedIsCrisis() {
            FindingStyler.Style s = styler.style(finding(ActionType.MONITOR), true, null);
            assertThat(s.tone()).isEqualTo(FindingStyler.CRISIS_TONE);
            assertThat(s.action()).isEqualTo("Monitor · safety-related");
        }

        @DisplayName("Information findings: green only when reassuring")
        @ParameterizedTest(name = "conclusion {0} -> {1} / {2}")
        @CsvSource({"depression_minimal, info, No concern", "cssrs_no_risk, info, No concern",
                    "anhedonia_clinical, note, Symptom noted", "risk_assessment_unlocked, note, Follow-up checks",
                    "something_else, note, Information"})
        void infoTones(String key, String tone, String action) {
            FindingStyler.Style s = styler.style(finding(ActionType.DISPLAY_INFO), false, rule(key, "1"));
            assertThat(s.tone()).isEqualTo(tone);
            assertThat(s.action()).isEqualTo(action);
        }

        @Test
        void overallMinimalIsReassuring() {
            assertThat(styler.style(finding(ActionType.DISPLAY_INFO), false,
                rule(RuleEngine.OVERALL_SEVERITY_KEY, "minimal")).tone()).isEqualTo("info");
        }

        @Test
        @DisplayName("Safety signals: a non-zero risk fact or crisis conclusion")
        void safetySignals() {
            assertThat(styler.isSafetySignal(new ConditionTrace("phq9_q9", "2", ">= 1", Operator.GTE), Set.of())).isTrue();
            assertThat(styler.isSafetySignal(new ConditionTrace("phq9_q9", "0", "== 0", Operator.EQ), Set.of())).isFalse();
            assertThat(styler.isSafetySignal(new ConditionTrace("gad7_total", "15", ">= 15", Operator.GTE), Set.of())).isFalse();
            assertThat(styler.isSafetySignal(new ConditionTrace("my_crisis", "1", "== 1", Operator.EQ), Set.of("my_crisis"))).isTrue();
        }

        @Test
        @DisplayName("Most serious first: crisis before info")
        void seriousnessOrder() {
            assertThat(styler.seriousness("crisis")).isLessThan(styler.seriousness("urgent"));
            assertThat(styler.seriousness("monitor")).isLessThan(styler.seriousness("info"));
        }
    }
}
