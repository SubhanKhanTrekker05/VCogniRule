package com.ooad.mhs.domain;

import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;
import com.ooad.mhs.util.Operator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the Bean Validation rules on the admin form objects RuleDetails and ConditionDetails. */
class FormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> messages(Object o) {
        return validator.validate(o).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static RuleDetails rule(String ruleId, String name, Integer level, Integer priority,
                                    String conclusionKey, String conclusionValue) {
        return new RuleDetails(ruleId, name, level, Category.MOOD, priority, ActionType.MONITOR,
            "explanation", "source", conclusionKey, conclusionValue);
    }

    // ------------------------------------------------------------------ RuleDetails

    @Test
    @DisplayName("A correct rule has no validation errors")
    void validRule() {
        assertThat(messages(rule("CUSTOM-001", "My rule", 2, 50, "my_flag", "1"))).isEmpty();
        assertThat(messages(rule("CUSTOM-002", "No conclusion", 1, 0, null, null))).isEmpty();
    }

    @DisplayName("Bad rule details are refused with a clear message")
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "priority 150         | R-1     | Name | 1 | 150 | Priority must be between 0 and 100.",
        "priority -5          | R-1     | Name | 1 | -5  | Priority must be between 0 and 100.",
        "level 0              | R-1     | Name | 0 | 50  | Level must be between 1 and 4.",
        "level 7              | R-1     | Name | 7 | 50  | Level must be between 1 and 4.",
        "blank name           | R-1     | '  ' | 1 | 50  | Name is required.",
        "rule ID with spaces  | 'bad id'| Name | 1 | 50  | Rule ID may only contain letters, digits, - and _.",
    })
    void badRuleDetails(String label, String ruleId, String name, int level, int priority, String expected) {
        assertThat(messages(rule(ruleId, name, level, priority, null, null))).contains(expected);
    }

    @Test
    @DisplayName("A conclusion key needs a value")
    void conclusionKeyNeedsValue() {
        assertThat(messages(rule("R-1", "Name", 1, 50, "my_flag", "")))
            .contains("A conclusion key needs a conclusion value.");
    }

    @Test
    @DisplayName("No conclusion key + the form's default value '1' is fine; the value is ignored (bug found in the browser trial)")
    void defaultValueWithoutKeyIsAllowed() {
        RuleDetails r = rule("R-1", "Name", 1, 50, "", "1");
        assertThat(messages(r)).isEmpty();
        assertThat(r.normalized().conclusionValue()).isNull();
    }

    @Test
    @DisplayName("A conclusion key must be lower case")
    void conclusionKeyLowerCase() {
        assertThat(messages(rule("R-1", "Name", 1, 50, "My_Flag", "1")))
            .contains("Conclusion key may only contain lower-case letters, digits and _.");
    }

    @Test
    @DisplayName("Missing category and action type are refused")
    void missingEnums() {
        RuleDetails r = new RuleDetails("R-1", "Name", 1, null, 50, null, null, null, null, null);
        assertThat(messages(r)).contains("Category is required.", "Action type is required.");
    }

    @Test
    @DisplayName("normalized() trims texts and turns blank optional texts into null")
    void ruleNormalized() {
        RuleDetails n = rule(" R-1 ", " Name ", 1, 50, " ", "").normalized();
        assertThat(n.ruleId()).isEqualTo("R-1");
        assertThat(n.name()).isEqualTo("Name");
        assertThat(n.conclusionKey()).isNull();
        assertThat(n.conclusionValue()).isNull();
    }

    // ------------------------------------------------------------------ ConditionDetails

    private static ConditionDetails cond(Operator op, Integer value, String text, Integer min, Integer max) {
        return new ConditionDetails("phq9_total", op, value, text, min, max);
    }

    @Test
    @DisplayName("Correct conditions have no validation errors")
    void validConditions() {
        assertThat(messages(cond(Operator.GTE, 10, null, null, null))).isEmpty();
        assertThat(messages(cond(Operator.NE, 0, null, null, null))).isEmpty();
        assertThat(messages(cond(Operator.BETWEEN, null, null, 5, 9))).isEmpty();
        assertThat(messages(cond(Operator.BETWEEN, null, null, 5, 5))).isEmpty();
        assertThat(messages(cond(Operator.IN, null, "1,2,3", null, null))).isEmpty();
        assertThat(messages(cond(Operator.IN, null, " 0, 2 ", null, null))).isEmpty();
    }

    @Test
    @DisplayName("Values that do not fit the operator are refused")
    void valuesMustFitOperator() {
        String msg = "The values do not fit the operator: BETWEEN needs Min and Max (Min not above Max), "
            + "IN needs whole numbers separated by commas (e.g. 1,2,3), all other operators need Value.";
        assertThat(messages(cond(Operator.GTE, null, null, null, null))).as("GTE without value").contains(msg);
        assertThat(messages(cond(Operator.BETWEEN, null, null, null, 9))).as("BETWEEN without min").contains(msg);
        assertThat(messages(cond(Operator.BETWEEN, null, null, 9, 5))).as("BETWEEN min > max").contains(msg);
        assertThat(messages(cond(Operator.IN, null, "a,b", null, null))).as("IN with text").contains(msg);
        assertThat(messages(cond(Operator.IN, null, null, null, null))).as("IN without list").contains(msg);
    }

    @Test
    @DisplayName("Field and operator are required")
    void fieldAndOperatorRequired() {
        assertThat(messages(new ConditionDetails(" ", null, 1, null, null, null)))
            .contains("Field is required.", "Operator is required.");
    }

    @Test
    @DisplayName("normalized() keeps only the values the operator uses")
    void conditionNormalized() {
        ConditionDetails between = cond(Operator.BETWEEN, 7, "x", 5, 9).normalized();
        assertThat(between.valueInt()).isNull();
        assertThat(between.valueText()).isNull();
        assertThat(between.valueMin()).isEqualTo(5);
        ConditionDetails in = cond(Operator.IN, 7, " 1, 2 ", 5, 9).normalized();
        assertThat(in.valueText()).isEqualTo("1,2");
        assertThat(in.valueInt()).isNull();
    }
}
