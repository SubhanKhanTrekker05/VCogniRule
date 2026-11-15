package com.ooad.mhs.web;

import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.repository.RuleRepository;
import com.ooad.mhs.service.AdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integration tests for the admin panel: login, protection of admin pages, rule and condition changes. */
class AdminIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AdminService adminService;

    @Autowired
    private RuleRepository ruleRepository;

    /** Every test that changes rules is undone with a factory reset, so tests never affect each other. */
    @AfterEach
    void restoreKnowledgeBase() {
        ruleRepository.resetKnowledgeBase();
    }

    private Rule ruleByRuleId(String ruleId) {
        return adminService.getAllRules().stream().filter(r -> r.ruleId().equals(ruleId)).findFirst().orElseThrow();
    }

    /** How many times {@code part} occurs in {@code text}. */
    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    private static MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder b, Map<String, String> fields) {
        fields.forEach(b::param);
        return b;
    }

    private static final Map<String, String> GOOD_RULE = Map.of("ruleId", "TEST-1", "name", "Test rule",
        "level", "1", "category", "MOOD", "priority", "50", "actionType", "MONITOR",
        "explanation", "Test", "sourceRef", "Test", "conclusionKey", "test_flag", "conclusionValue", "1");

    // ================================================================== login and protection
    @Nested
    @DisplayName("Login, logout and protected pages")
    class Security {

        @Test
        @DisplayName("Wrong password → login page with an error, no session")
        void wrongPassword() throws Exception {
            mvc.perform(post("/admin/login").param("username", "admin").param("password", "wrong"))
                .andExpect(status().isOk()).andExpect(view().name("admin-login"))
                .andExpect(model().attribute("error", "Invalid username or password"));
        }

        @Test
        @DisplayName("Right password → dashboard with all 150 rules")
        void rightPassword() throws Exception {
            MockHttpSession session = loginAsAdmin();
            assertThat(session.getAttribute("admin")).isEqualTo("admin");
            MvcResult r = mvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk()).andExpect(view().name("admin-rule-list")).andReturn();
            assertThat((List<?>) r.getModelAndView().getModel().get("rules")).hasSize(150);
        }

        @Test
        @DisplayName("Login page while logged in goes straight to the dashboard")
        void loginPageWhenLoggedIn() throws Exception {
            mvc.perform(get("/admin").session(loginAsAdmin())).andExpect(redirectedUrl("/admin/dashboard"));
        }

        @Test
        @DisplayName("Logout ends the session; the dashboard is then protected again")
        void logout() throws Exception {
            MockHttpSession session = loginAsAdmin();
            mvc.perform(get("/admin/logout").session(session)).andExpect(redirectedUrl("/admin"));
            assertThat(session.isInvalid()).isTrue();
            mvc.perform(get("/admin/dashboard")).andExpect(redirectedUrl("/admin"));
        }

        @DisplayName("Every protected admin URL sends a logged-out visitor to the login page")
        @ParameterizedTest(name = "{0} {1}")
        @CsvSource({
            "GET, /admin/dashboard", "POST, /admin/toggle/1", "GET, /admin/rule/new", "POST, /admin/rule/new",
            "GET, /admin/rule/1", "POST, /admin/rule/1", "POST, /admin/rule/2/delete",
            "GET, /admin/rule/1/condition/new", "POST, /admin/rule/1/condition/new",
            "GET, /admin/rule/1/condition/1/edit", "POST, /admin/rule/1/condition/1/edit",
            "POST, /admin/rule/1/condition/1/delete", "POST, /admin/reset",
        })
        void protectedUrls(String method, String url) throws Exception {
            MockHttpServletRequestBuilder b = method.equals("GET") ? get(url) : post(url).param("active", "false");
            mvc.perform(b).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
        }

        @Test
        @DisplayName("Logged-out POSTs change nothing (delete, toggle, edit)")
        void loggedOutChangesNothing() throws Exception {
            mvc.perform(post("/admin/rule/2/delete"));
            mvc.perform(post("/admin/toggle/1").param("active", "false"));
            mvc.perform(form(post("/admin/rule/1"), Map.of("name", "HACKED", "level", "1", "category", "MOOD",
                "priority", "1", "actionType", "MONITOR")));
            assertThat(adminService.getAllRules()).hasSize(150);
            assertThat(adminService.getRuleById(1L).orElseThrow().name()).isEqualTo("Minimal depression");
            assertThat(adminService.getRuleById(1L).orElseThrow().isActive()).isTrue();
        }
    }

    // ================================================================== rule and condition changes
    @Nested
    @DisplayName("Create, edit, delete and reset")
    class Changes {

        @Test
        @DisplayName("Create a rule → saved, active, with a success message")
        void createRule() throws Exception {
            mvc.perform(form(post("/admin/rule/new").session(loginAsAdmin()), GOOD_RULE))
                .andExpect(redirectedUrl("/admin/dashboard")).andExpect(flash().attribute("toast", "Rule TEST-1 created."));
            Rule r = ruleByRuleId("TEST-1");
            assertThat(r.isActive()).isTrue();
            assertThat(r.priority()).isEqualTo(50);
            assertThat(adminService.getAllRules()).hasSize(151);
        }

        @Test
        @DisplayName("Create a rule exactly as the blank form sends it (no conclusion key, default value '1')")
        void createRuleWithFormDefaults() throws Exception {
            mvc.perform(form(post("/admin/rule/new").session(loginAsAdmin()), Map.of("ruleId", "TEST-2",
                    "name", "Simple rule", "level", "1", "category", "MOOD", "priority", "50", "actionType", "MONITOR",
                    "explanation", "", "sourceRef", "", "conclusionKey", "", "conclusionValue", "1")))
                .andExpect(redirectedUrl("/admin/dashboard")).andExpect(flash().attribute("toast", "Rule TEST-2 created."));
            Rule r = ruleByRuleId("TEST-2");
            assertThat(r.conclusionKey()).isNull();
            assertThat(r.conclusionValue()).isNull();
        }

        @Test
        @DisplayName("Add, edit and delete a condition")
        void conditionLifecycle() throws Exception {
            MockHttpSession s = loginAsAdmin();
            mvc.perform(form(post("/admin/rule/new").session(s), GOOD_RULE));
            Long id = ruleByRuleId("TEST-1").id();

            mvc.perform(post("/admin/rule/" + id + "/condition/new").session(s)
                    .param("fieldKey", "phq9_total").param("operator", "GTE").param("valueInt", "7"))
                .andExpect(redirectedUrl("/admin/rule/" + id));
            Condition c = ruleByRuleId("TEST-1").groups().get(0).conditions().get(0);
            assertThat(c.operator().name()).isEqualTo("GTE");
            assertThat(c.valueInt()).isEqualTo(7);

            mvc.perform(post("/admin/rule/" + id + "/condition/" + c.id() + "/edit").session(s)
                    .param("fieldKey", "phq9_total").param("operator", "BETWEEN").param("valueMin", "3").param("valueMax", "8"))
                .andExpect(redirectedUrl("/admin/rule/" + id));
            Condition edited = ruleByRuleId("TEST-1").groups().get(0).conditions().get(0);
            assertThat(edited.operator().name()).isEqualTo("BETWEEN");
            assertThat(edited.valueMin()).isEqualTo(3);
            assertThat(edited.valueInt()).as("unused value cleared").isNull();

            mvc.perform(post("/admin/rule/" + id + "/condition/" + c.id() + "/delete").session(s))
                .andExpect(redirectedUrl("/admin/rule/" + id));
            assertThat(ruleByRuleId("TEST-1").groups().get(0).conditions()).isEmpty();
        }

        @Test
        @DisplayName("Edit a rule's details")
        void editRule() throws Exception {
            mvc.perform(form(post("/admin/rule/1").session(loginAsAdmin()), Map.of("ruleId", "PHQ9-MINIMAL",
                    "name", "Minimal depression (edited)", "level", "1", "category", "MOOD", "priority", "21",
                    "actionType", "DISPLAY_INFO", "conclusionKey", "depression_minimal", "conclusionValue", "1")))
                .andExpect(redirectedUrl("/admin/rule/1"));
            Rule r = adminService.getRuleById(1L).orElseThrow();
            assertThat(r.name()).isEqualTo("Minimal depression (edited)");
            assertThat(r.priority()).isEqualTo(21);
        }

        @Test
        @DisplayName("Switch a rule off: the engine stops using it, its page still opens (M4)")
        void toggleOffAndOpen() throws Exception {
            MockHttpSession s = loginAsAdmin();
            mvc.perform(post("/admin/toggle/1").param("active", "false").session(s)).andExpect(redirectedUrl("/admin/dashboard"));
            assertThat(ruleRepository.getActiveRules()).extracting(Rule::ruleId).doesNotContain("PHQ9-MINIMAL");
            mvc.perform(get("/admin/rule/1").session(s)).andExpect(status().isOk()).andExpect(view().name("admin-rule-detail"));
        }

        @Test
        @DisplayName("Dashboard shows Active / Inactive in words; only switching OFF asks for confirmation")
        void dashboardStatusAndConfirm() throws Exception {
            MockHttpSession s = loginAsAdmin();
            mvc.perform(post("/admin/toggle/1").param("active", "false").session(s));
            String html = mvc.perform(get("/admin/dashboard").session(s)).andReturn().getResponse().getContentAsString();
            assertThat(count(html, "class=\"status-pill is-off\">Inactive<")).isEqualTo(1);
            assertThat(count(html, "class=\"status-pill is-on\">Active<")).isEqualTo(149);
            assertThat(count(html, "data-confirm=\"Switch off rule ")).isEqualTo(149);   // every active rule asks first
            assertThat(html).doesNotContain("data-confirm=\"Switch off rule PHQ9-MINIMAL");  // the inactive one does not
            assertThat(count(html, "row-inactive")).isEqualTo(1);
        }

        @Test
        @DisplayName("A rule that does not exist → back to the dashboard with a message, not an error page")
        void missingRule() throws Exception {
            mvc.perform(get("/admin/rule/99999").session(loginAsAdmin()))
                .andExpect(redirectedUrl("/admin/dashboard")).andExpect(flash().attribute("toast", "Rule 99999 was not found."));
        }

        @Test
        @DisplayName("Delete a rule")
        void deleteRule() throws Exception {
            mvc.perform(post("/admin/rule/2/delete").session(loginAsAdmin())).andExpect(redirectedUrl("/admin/dashboard"));
            assertThat(adminService.getRuleById(2L)).isEmpty();
            assertThat(adminService.getAllRules()).hasSize(149);
        }

        @Test
        @DisplayName("Factory reset restores the original 150 rules after changes")
        void factoryReset() throws Exception {
            MockHttpSession s = loginAsAdmin();
            mvc.perform(post("/admin/rule/2/delete").session(s));
            mvc.perform(post("/admin/toggle/1").param("active", "false").session(s));
            mvc.perform(post("/admin/reset").session(s)).andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("toast", "Knowledge base restored to factory defaults. 150 rules reloaded."));
            assertThat(adminService.getAllRules()).hasSize(150);
            assertThat(ruleRepository.getActiveRules()).hasSize(150);
        }

        @Test
        @DisplayName("M5: the PHQ9-MINIMAL rule covers totals 0-4")
        void phq9MinimalBand() {
            Condition c = ruleByRuleId("PHQ9-MINIMAL").groups().get(0).conditions().get(0);
            assertThat(c.valueMin()).isZero();
            assertThat(c.valueMax()).isEqualTo(4);
        }
    }

    // ================================================================== bad admin input
    static Stream<Arguments> badInputs() {
        return Stream.of(
            Arguments.of("new rule: duplicate rule ID", "/admin/rule/new", Map.of("ruleId", "PHQ9-MINIMAL"), "already exists"),
            Arguments.of("new rule: empty rule ID", "/admin/rule/new", Map.of("ruleId", ""), "Rule ID is required."),
            Arguments.of("new rule: priority 150", "/admin/rule/new", Map.of("ruleId", "T-2", "priority", "150"), "Priority must be between 0 and 100."),
            Arguments.of("new rule: level 7", "/admin/rule/new", Map.of("ruleId", "T-3", "level", "7"), "Level must be between 1 and 4."),
            Arguments.of("new rule: category BANANA", "/admin/rule/new", Map.of("ruleId", "T-4", "category", "BANANA"), "Invalid value for category"),
            Arguments.of("new rule: empty name", "/admin/rule/new", Map.of("ruleId", "T-5", "name", " "), "Name is required."),
            Arguments.of("new rule: priority 'abc'", "/admin/rule/new", Map.of("ruleId", "T-6", "priority", "abc"), "Invalid value for priority"),
            Arguments.of("edit rule: priority -5", "/admin/rule/1", Map.of("priority", "-5"), "Priority must be between 0 and 100.")
        );
    }

    @DisplayName("Bad rule input is refused with a message; nothing is saved")
    @ParameterizedTest(name = "{0}")
    @MethodSource("badInputs")
    void badRuleInput(String label, String url, Map<String, String> changes, String expectedMessage) throws Exception {
        Map<String, String> fields = new java.util.HashMap<>(GOOD_RULE);
        fields.putAll(changes);
        MvcResult r = mvc.perform(form(post(url).session(loginAsAdmin()), fields))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("formError")).andReturn();
        assertThat((String) r.getFlashMap().get("formError")).contains(expectedMessage);
        assertThat(adminService.getAllRules()).hasSize(150);
        assertThat(adminService.getRuleById(1L).orElseThrow().priority()).isEqualTo(20);
    }

    @DisplayName("Bad condition input is refused with a message; nothing is saved")
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "BETWEEN without min/max | phq9_total | BETWEEN |    |     |   |   | The values do not fit the operator",
        "BETWEEN min 9 > max 5   | phq9_total | BETWEEN |    |     | 9 | 5 | The values do not fit the operator",
        "GTE without a value     | phq9_total | GTE     |    |     |   |   | The values do not fit the operator",
        "IN with text 'a,b'      | phq9_total | IN      |    | a,b |   |   | The values do not fit the operator",
        "unknown field 'xyz'     | xyz        | EQ      | 1  |     |   |   | Unknown field: xyz.",
        "operator 'LIKE'         | phq9_total | LIKE    | 1  |     |   |   | Invalid value for operator",
    })
    void badConditionInput(String label, String field, String op, String value, String text, String min, String max,
                           String expectedMessage) throws Exception {
        MockHttpServletRequestBuilder b = post("/admin/rule/2/condition/new").session(loginAsAdmin())
            .param("fieldKey", field).param("operator", op);
        if (value != null) b.param("valueInt", value);
        if (text != null) b.param("valueText", text);
        if (min != null) b.param("valueMin", min);
        if (max != null) b.param("valueMax", max);
        MvcResult r = mvc.perform(b).andExpect(status().is3xxRedirection()).andReturn();
        assertThat((String) r.getFlashMap().get("formError")).contains(expectedMessage);
        assertThat(adminService.getRuleById(2L).orElseThrow().groups().get(0).conditions()).hasSize(1);
    }

    @Test
    @DisplayName("After a refused bad IN condition, screenings still work (was a crash before S4)")
    void screeningStillWorksAfterBadInput() throws Exception {
        mvc.perform(post("/admin/rule/1/condition/new").session(loginAsAdmin())
            .param("fieldKey", "phq9_total").param("operator", "IN").param("valueText", "a,b"));
        mvc.perform(submit(allAnswers(false))).andExpect(status().isOk()).andExpect(view().name("report"));
    }

    @Test
    @DisplayName("A condition cannot be deleted through another rule's URL")
    void conditionOfOtherRule() throws Exception {
        Long conditionOfRule1 = adminService.getRuleById(1L).orElseThrow().groups().get(0).conditions().get(0).id();
        mvc.perform(post("/admin/rule/2/condition/" + conditionOfRule1 + "/delete").session(loginAsAdmin()))
            .andExpect(redirectedUrl("/admin/rule/2")).andExpect(flash().attributeExists("formError"));
        assertThat(adminService.getRuleById(1L).orElseThrow().groups().get(0).conditions()).hasSize(1);
    }
}
