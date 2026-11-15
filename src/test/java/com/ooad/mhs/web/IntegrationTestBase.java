package com.ooad.mhs.web;

import com.ooad.mhs.service.QuestionnaireService;
import com.ooad.mhs.service.QuestionnaireService.Option;
import com.ooad.mhs.service.QuestionnaireService.QuestionView;
import com.ooad.mhs.service.QuestionnaireService.SectionView;
import com.ooad.mhs.service.ReportService.FindingView;
import com.ooad.mhs.service.ReportService.ScreeningReport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Base class of the integration tests: starts the whole application (Spring Boot Test) with its
 * own fresh SQLite database file in target/, and gives each test a MockMvc to send HTTP requests.
 *
 * All integration test classes extend this class, so Spring starts the application only once
 * and they share it.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:target/integration-test-${random.uuid}.db",
    "logging.level.com.ooad.mhs=INFO",
    "logging.level.org.springframework.jdbc.core=WARN"
})
abstract class IntegrationTestBase {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected QuestionnaireService questionnaireService;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    /** Logs in as the default admin and returns the logged-in session. */
    protected MockHttpSession loginAsAdmin() throws Exception {
        MvcResult r = mvc.perform(post("/admin/login").param("username", "admin").param("password", "admin123"))
            .andExpect(redirectedUrl("/admin/dashboard")).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    /**
     * Answers to every question of every section (the same sets as the earlier manual trial runs).
     * healthy = lowest symptom answer everywhere (WHO-5 highest), Safety Check all "No";
     * high    = highest symptom answer everywhere (WHO-5 lowest), Safety Check all "No".
     */
    protected Map<String, String> allAnswers(boolean high) {
        Map<String, String> a = new LinkedHashMap<>();
        for (SectionView s : questionnaireService.getSections()) {
            for (QuestionView q : s.questions()) {
                String key = q.fieldKey();
                switch (q.kind()) {
                    case "time" -> a.put(key, key.equals("psqi_q1") ? "23:00" : (high ? "04:00" : "07:00"));
                    case "number" -> a.put(key, key.equals("psqi_q2") ? (high ? "90" : "10") : (high ? "4" : "8"));
                    default -> {
                        List<Option> o = q.options();
                        boolean wellbeing = key.startsWith("who5_");
                        boolean highest = !key.startsWith("cssrs_") && (high != wellbeing);
                        a.put(key, highest ? o.get(o.size() - 1).value() : o.get(0).value());
                    }
                }
            }
        }
        return a;
    }

    /** Answers given as values for prefix1, prefix2, ... */
    protected static Map<String, String> items(String prefix, int... values) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) m.put(prefix + (i + 1), String.valueOf(values[i]));
        return m;
    }

    @SafeVarargs
    protected static Map<String, String> merge(Map<String, String>... maps) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map<String, String> x : maps) m.putAll(x);
        return m;
    }

    /** A POST /screening request with the given answers. */
    protected static MockHttpServletRequestBuilder submit(Map<String, String> answers) {
        MockHttpServletRequestBuilder b = post("/screening");
        answers.forEach(b::param);
        return b;
    }

    protected static ScreeningReport report(MvcResult r) {
        return (ScreeningReport) Objects.requireNonNull(r.getModelAndView()).getModel().get("report");
    }

    /** Rule IDs of every finding card on the report. */
    protected static List<String> ruleIds(ScreeningReport report) {
        return Stream.of(report.results(), report.guidance(), report.reasoning())
            .flatMap(List::stream).map(FindingView::finding).map(f -> f.ruleId()).toList();
    }
}
