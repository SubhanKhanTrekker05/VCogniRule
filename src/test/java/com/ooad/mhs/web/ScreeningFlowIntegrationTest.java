package com.ooad.mhs.web;

import com.ooad.mhs.service.AssessmentService.SectionStatus;
import com.ooad.mhs.service.QuestionnaireService.SectionView;
import com.ooad.mhs.service.ReportService.ScreeningReport;
import com.ooad.mhs.util.Section;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integration tests: the user's screening flow through the real web app (Spring Boot Test + MockMvc). */
class ScreeningFlowIntegrationTest extends IntegrationTestBase {

    private static final String DISCLAIMER = "This is a screening result, not a medical diagnosis.";

    private MvcResult post(Map<String, String> answers) throws Exception {
        return mvc.perform(submit(answers)).andExpect(status().isOk()).andExpect(view().name("report")).andReturn();
    }

    // ------------------------------------------------------------------ pages

    @Test
    @DisplayName("Welcome, About and Assessment pages load; an unknown page gives 404")
    void pagesLoad() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("welcome"));
        mvc.perform(get("/about")).andExpect(status().isOk()).andExpect(view().name("about"));
        mvc.perform(get("/screening")).andExpect(status().isOk()).andExpect(view().name("assessment"));
        mvc.perform(get("/no-such-page")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The assessment shows 10 sections with 84 questions, Safety Check first")
    void assessmentHasAllQuestions() throws Exception {
        MvcResult r = mvc.perform(get("/screening")).andReturn();
        @SuppressWarnings("unchecked")
        List<SectionView> sections = (List<SectionView>) r.getModelAndView().getModel().get("sections");
        assertThat(sections).hasSize(10);
        assertThat(sections.get(0).section()).isEqualTo(Section.CSSRS);
        assertThat(sections.stream().mapToInt(s -> s.questions().size()).sum()).isEqualTo(84);
    }

    // ------------------------------------------------------------------ the 4 original screening tests

    @Test
    @DisplayName("S1: all answers healthy → MINIMAL, no crisis box, disclaimer shown")
    void healthyScreening() throws Exception {
        MvcResult r = post(allAnswers(false));
        ScreeningReport rep = report(r);
        assertThat(rep.overallKey()).isEqualTo("minimal");
        assertThat(rep.hasCrisis()).isFalse();
        assertThat(ruleIds(rep)).contains("PHQ9-MINIMAL", "GAD7-MINIMAL", "CSSRS-NO-RISK", "WHO5-GOOD");
        assertThat(rep.partial()).isEmpty();
        String html = r.getResponse().getContentAsString();
        assertThat(html).contains(DISCLAIMER).contains("Total score 0 / 27").doesNotContain("1166");
    }

    @Test
    @DisplayName("S2: all answers at the worst value → NEEDS ATTENTION and the crisis helplines")
    void highSymptomScreening() throws Exception {
        MvcResult r = post(allAnswers(true));
        ScreeningReport rep = report(r);
        assertThat(rep.overallKey()).isEqualTo("attention");
        assertThat(rep.hasCrisis()).isTrue();
        assertThat(ruleIds(rep)).contains("PHQ9-Q9-CRISIS", "PHQ9-SEVERE", "GAD7-SEVERE");
        assertThat(r.getResponse().getContentAsString()).contains("1166").contains(DISCLAIMER);
    }

    @Test
    @DisplayName("S3: Safety Check items 1 and 4 = Yes → NEEDS ATTENTION, crisis finding first")
    void safetyCheckCrisis() throws Exception {
        Map<String, String> a = allAnswers(false);
        a.put("cssrs_q1", "1");
        a.put("cssrs_q4", "1");
        ScreeningReport rep = report(post(a));
        assertThat(rep.overallKey()).isEqualTo("attention");
        assertThat(rep.hasCrisis()).isTrue();
        assertThat(ruleIds(rep)).contains("CSSRS-INTENT", "CSSRS-PASSIVE");
        assertThat(rep.results().get(0).tone()).isEqualTo("crisis");
    }

    @Test
    @DisplayName("Crisis: engine steps (GATE-*) only in 'How the system reasoned'; crisis box and banner unchanged")
    void gateFindingsOnlyInReasoning() throws Exception {
        Map<String, String> a = allAnswers(false);
        a.put("cssrs_q4", "1");
        ScreeningReport rep = report(post(a));
        assertThat(rep.hasCrisis()).isTrue();
        assertThat(rep.overallKey()).isEqualTo("attention");
        assertThat(rep.results().get(0).finding().ruleId()).isEqualTo("CSSRS-INTENT");
        assertThat(rep.results()).extracting(fv -> fv.finding().ruleId()).noneMatch(id -> id.startsWith("GATE-"));
        assertThat(rep.reasoning()).extracting(fv -> fv.finding().ruleId()).contains("GATE-CRISIS");
    }

    @Test
    @DisplayName("S4: Safety Check skipped → sent back to the Safety Check with an error")
    void safetyCheckIsMandatory() throws Exception {
        Map<String, String> a = allAnswers(false);
        a.keySet().removeIf(k -> k.startsWith("cssrs_"));
        mvc.perform(submit(a)).andExpect(redirectedUrl("/screening?resume=C-SSRS&error=safety"));
        mvc.perform(get("/screening").param("resume", "C-SSRS").param("error", "safety"))
            .andExpect(model().attribute("safetyRequired", true));
    }

    // ------------------------------------------------------------------ demo script answer sets

    private static final Map<String, String> NO_SAFETY_RISK = items("cssrs_q", 0, 0, 0, 0, 0, 0);

    @Test
    @DisplayName("Demo A (healthy): Safety Check No, PHQ-9 all 0, GAD-7 all 0 → MINIMAL")
    void demoHealthy() throws Exception {
        ScreeningReport rep = report(post(merge(NO_SAFETY_RISK, items("phq9_q", 0, 0, 0, 0, 0, 0, 0, 0, 0),
            items("gad7_q", 0, 0, 0, 0, 0, 0, 0))));
        assertThat(rep.overallKey()).isEqualTo("minimal");
    }

    @Test
    @DisplayName("Demo B (mild): PHQ-9 total 8 → MILD")
    void demoMild() throws Exception {
        ScreeningReport rep = report(post(merge(NO_SAFETY_RISK, items("phq9_q", 1, 1, 1, 1, 1, 1, 1, 1, 0),
            items("gad7_q", 1, 1, 1, 0, 0, 0, 0))));
        assertThat(rep.overallKey()).isEqualTo("mild");
        assertThat(ruleIds(rep)).contains("PHQ9-MILD", "MAY-DEP");
    }

    @Test
    @DisplayName("Demo C (moderate): PHQ-9 12 and GAD-7 14 → MODERATE (depression + anxiety together)")
    void demoModerate() throws Exception {
        ScreeningReport rep = report(post(merge(NO_SAFETY_RISK, items("phq9_q", 2, 2, 2, 2, 2, 2, 0, 0, 0),
            items("gad7_q", 2, 2, 2, 2, 2, 2, 2))));
        assertThat(rep.overallKey()).isEqualTo("moderate");
        assertThat(rep.hasCrisis()).isFalse();
        assertThat(ruleIds(rep)).contains("PHQ9-MODERATE", "GAD7-MODERATE", "COMP-MIXED-AD");
    }

    @Test
    @DisplayName("Demo D (crisis): only Safety Check item 4 (intent) = Yes → NEEDS ATTENTION")
    void demoCrisis() throws Exception {
        ScreeningReport rep = report(post(merge(items("cssrs_q", 0, 0, 0, 1, 0, 0),
            items("phq9_q", 0, 0, 0, 0, 0, 0, 0, 0, 0), items("gad7_q", 0, 0, 0, 0, 0, 0, 0))));
        assertThat(rep.overallKey()).isEqualTo("attention");
        assertThat(ruleIds(rep)).contains("CSSRS-INTENT");
    }

    @Test
    @DisplayName("A graded Safety Check answer ('Yes, a few times' = 2) counts as Yes")
    void gradedSafetyAnswerCountsAsYes() throws Exception {
        Map<String, String> a = merge(items("cssrs_q", 0, 0, 0, 2, 0, 0), items("phq9_q", 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(report(post(a)).hasCrisis()).isTrue();
    }

    // ------------------------------------------------------------------ wrong input, partial sections

    @Test
    @DisplayName("Only the Safety Check answered → overall 'Not determined', PHQ-9 'not assessed'")
    void onlySafetyCheck() throws Exception {
        ScreeningReport rep = report(post(NO_SAFETY_RISK));
        assertThat(rep.overallKey()).isEqualTo("undetermined");
        assertThat(rep.depressionMissing()).isTrue();
        assertThat(rep.notStarted()).extracting(SectionStatus::section).contains(Section.PHQ9);
    }

    @Test
    @DisplayName("An invalid answer ('abc') makes the section partly answered instead of scoring it")
    void invalidAnswer() throws Exception {
        Map<String, String> a = allAnswers(false);
        a.put("phq9_q1", "abc");
        ScreeningReport rep = report(post(a));
        assertThat(rep.overallKey()).isEqualTo("undetermined");
        assertThat(rep.partial()).extracting(SectionStatus::section).containsExactly(Section.PHQ9);
        assertThat(ruleIds(rep)).doesNotContain("PHQ9-MINIMAL");
    }

    @Test
    @DisplayName("Out-of-range and unknown values are ignored (99 on a 0-3 scale, unknown field)")
    void outOfRangeAndUnknown() throws Exception {
        Map<String, String> a = allAnswers(false);
        a.put("gad7_q1", "99");
        a.put("hack", "1");
        ScreeningReport rep = report(post(a));
        assertThat(rep.partial()).extracting(SectionStatus::section).containsExactly(Section.GAD7);
        assertThat(rep.overallKey()).isEqualTo("minimal");
        assertThat(rep.overallNote()).contains("Based only on the sections you completed");
    }

    // ------------------------------------------------------------------ resume

    @Test
    @DisplayName("'Complete this section' restores the previous answers; a new screening clears them")
    void resumeKeepsAnswers() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(submit(NO_SAFETY_RISK).session(session)).andExpect(status().isOk());
        MvcResult resumed = mvc.perform(get("/screening").param("resume", "PHQ-9").session(session)).andReturn();
        @SuppressWarnings("unchecked")
        Map<String, String> restored = (Map<String, String>) resumed.getModelAndView().getModel().get("answers");
        assertThat(restored).containsEntry("cssrs_q1", "0").hasSize(6);
        assertThat(resumed.getModelAndView().getModel().get("resumeSection")).isEqualTo("PHQ-9");

        MvcResult fresh = mvc.perform(get("/screening").session(session)).andReturn();
        assertThat((Map<?, ?>) fresh.getModelAndView().getModel().get("answers")).isEmpty();
    }

    @Test
    @DisplayName("An unknown section code in ?resume= is not echoed into the page")
    void unknownResumeCodeIgnored() throws Exception {
        mvc.perform(get("/screening").param("resume", "<script>"))
            .andExpect(status().isOk()).andExpect(model().attribute("resumeSection", (Object) null));
    }
}
