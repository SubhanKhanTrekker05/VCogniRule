package com.ooad.mhs.web;

import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.domain.ConditionDetails;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.domain.RuleDetails;
import com.ooad.mhs.repository.RuleRepository;
import com.ooad.mhs.util.Operator;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin forms keep what the admin typed when a save fails, the condition editor opens
 * pre-filled, and errors show the themed error page instead of the Whitelabel page.
 */
@DisplayName("Form recovery and error page")
class FormRecoveryAndErrorPageIntegrationTest extends IntegrationTestBase {

    /** Rule 20 = CSSRS-PASSIVE, one condition: cssrs_passive_ideation EQ 1. */
    private static final long RULE_ID = 20;

    @Autowired
    private RuleRepository ruleRepository;

    @AfterEach
    void restoreKnowledgeBase() {
        ruleRepository.resetKnowledgeBase();
    }

    private Condition firstCondition(MockHttpSession session) throws Exception {
        MvcResult r = mvc.perform(get("/admin/rule/" + RULE_ID).session(session)).andReturn();
        Rule rule = (Rule) r.getModelAndView().getModel().get("rule");
        return rule.groups().get(0).conditions().get(0);
    }

    @Test
    @DisplayName("Invalid new condition → back to the form with the error AND the typed values")
    void invalidNewConditionKeepsValues() throws Exception {
        MvcResult r = mvc.perform(post("/admin/rule/" + RULE_ID + "/condition/new").session(loginAsAdmin())
                .param("fieldKey", "cssrs_q1").param("operator", "IN").param("valueText", "a,b"))
            .andExpect(redirectedUrl("/admin/rule/" + RULE_ID + "/condition/new"))
            .andExpect(flash().attributeExists("formError"))
            .andReturn();
        ConditionDetails kept = (ConditionDetails) r.getFlashMap().get("form");
        assertThat(kept).isNotNull();
        assertThat(kept.fieldKey()).isEqualTo("cssrs_q1");
        assertThat(kept.operator()).isEqualTo(Operator.IN);
        assertThat(kept.valueText()).isEqualTo("a,b");
    }

    @Test
    @DisplayName("Invalid condition edit → back to the editor with the typed values")
    void invalidConditionEditKeepsValues() throws Exception {
        MockHttpSession s = loginAsAdmin();
        Condition c = firstCondition(s);
        MvcResult r = mvc.perform(post("/admin/rule/" + RULE_ID + "/condition/" + c.id() + "/edit").session(s)
                .param("fieldKey", "cssrs_q2").param("operator", "BETWEEN").param("valueMin", "5").param("valueMax", "1"))
            .andExpect(redirectedUrl("/admin/rule/" + RULE_ID + "/condition/" + c.id() + "/edit"))
            .andExpect(flash().attributeExists("formError"))
            .andReturn();
        ConditionDetails kept = (ConditionDetails) r.getFlashMap().get("form");
        assertThat(kept.fieldKey()).isEqualTo("cssrs_q2");
        assertThat(kept.valueMin()).isEqualTo(5);
        assertThat(kept.valueMax()).isEqualTo(1);
    }

    @Test
    @DisplayName("Condition editor opens pre-filled with the saved condition")
    void conditionEditorIsPrefilled() throws Exception {
        MockHttpSession s = loginAsAdmin();
        Condition c = firstCondition(s);
        MvcResult r = mvc.perform(get("/admin/rule/" + RULE_ID + "/condition/" + c.id() + "/edit").session(s))
            .andExpect(status().isOk()).andExpect(view().name("admin-condition-form"))
            .andReturn();
        ConditionDetails form = (ConditionDetails) r.getModelAndView().getModel().get("form");
        assertThat(form).isNotNull();
        assertThat(form.fieldKey()).isEqualTo(c.fieldKey());
        assertThat(form.operator()).isEqualTo(c.operator());
        assertThat(form.valueInt()).isEqualTo(c.valueInt());
        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("value=\"cssrs_passive_ideation\" selected");
        assertThat(html).contains("value=\"EQ\" selected");
    }

    @Test
    @DisplayName("Invalid rule edit → back to the rule with the typed values, nothing saved")
    void invalidRuleEditKeepsValues() throws Exception {
        MockHttpSession s = loginAsAdmin();
        MvcResult r = mvc.perform(post("/admin/rule/" + RULE_ID).session(s)
                .param("ruleId", "CSSRS-PASSIVE").param("name", "Edited name").param("level", "1")
                .param("category", "RISK").param("priority", "500").param("actionType", "CRISIS_PROTOCOL"))
            .andExpect(redirectedUrl("/admin/rule/" + RULE_ID))
            .andExpect(flash().attributeExists("formError"))
            .andReturn();
        RuleDetails kept = (RuleDetails) r.getFlashMap().get("form");
        assertThat(kept.name()).isEqualTo("Edited name");
        assertThat(kept.priority()).isEqualTo(500);

        MvcResult page = mvc.perform(get("/admin/rule/" + RULE_ID).session(s)).andReturn();
        Rule saved = (Rule) page.getModelAndView().getModel().get("rule");
        assertThat(saved.name()).isEqualTo("Passive suicidal ideation");
    }

    @Test
    @DisplayName("404 → themed error page, no Whitelabel page and no technical details")
    void notFoundUsesThemedErrorPage() throws Exception {
        String html = mvc.perform(get("/error").accept(MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/does-not-exist"))
            .andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Page not found").contains("Go to home page").contains("/css/style.css");
        assertThat(html).doesNotContain("Whitelabel").doesNotContain("Exception");
    }

    @Test
    @DisplayName("Non-browser clients get JSON without technical details")
    void jsonErrorHasNoDetails() throws Exception {
        String json = mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret internal detail")))
            .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("secret internal detail").doesNotContain("trace");
    }

    @Test
    @DisplayName("500 → themed error page with a generic message")
    void serverErrorUsesThemedErrorPage() throws Exception {
        String html = mvc.perform(get("/error").accept(MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret internal detail")))
            .andExpect(status().isInternalServerError())
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Something went wrong");
        assertThat(html).doesNotContain("secret internal detail").doesNotContain("Whitelabel");
    }
}
