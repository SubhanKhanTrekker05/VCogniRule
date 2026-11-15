package com.ooad.mhs.engine;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;
import com.ooad.mhs.util.Operator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the inference engine, ConditionEvaluator, FindingMapper and ConflictResolver. */
class RuleEngineTest {

    private long nextId = 1;

    private Condition cond(String field, Operator op, Integer value) {
        return new Condition(nextId++, 0L, field, op, value, null, null, null);
    }

    private Condition between(String field, int min, int max) {
        return new Condition(nextId++, 0L, field, Operator.BETWEEN, null, null, min, max);
    }

    private ConditionGroup group(Condition... conditions) {
        return new ConditionGroup(nextId++, 0L, "AND", List.of(conditions));
    }

    private Rule rule(String id, int level, int priority, ActionType action, String key, String value, ConditionGroup... groups) {
        return new Rule(nextId++, id, id + " name", level, Category.MOOD, priority, action,
            "explanation", "source", key, value, true, List.of(groups));
    }

    private static Map<String, Integer> facts(Object... kv) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static List<String> ids(RuleEngine.RuleEngineResult r) {
        return r.findings().stream().map(Finding::ruleId).toList();
    }

    // ------------------------------------------------------------------ ConditionEvaluator

    @Test
    @DisplayName("A condition on a field that is not in the facts fails (section not answered)")
    void missingFieldFails() {
        assertThat(ConditionEvaluator.evaluate(cond("phq9_total", Operator.GTE, 0), facts())).isFalse();
    }

    @Test
    void presentFieldUsesTheOperatorStrategy() {
        assertThat(ConditionEvaluator.evaluate(cond("phq9_total", Operator.GTE, 10), facts("phq9_total", 12))).isTrue();
        assertThat(ConditionEvaluator.evaluate(cond("phq9_total", Operator.GTE, 10), facts("phq9_total", 9))).isFalse();
    }

    // ------------------------------------------------------------------ AND / OR

    @Test
    @DisplayName("AND inside a group: the rule fires only when every condition matches")
    void andWithinGroup() {
        Rule r = rule("R", 1, 50, ActionType.MONITOR, null, null,
            group(cond("a", Operator.EQ, 1), cond("b", Operator.EQ, 1)));
        assertThat(ids(new RuleEngine().evaluate(List.of(r), facts("a", 1, "b", 1)))).containsExactly("R");
        assertThat(ids(new RuleEngine().evaluate(List.of(r), facts("a", 1, "b", 0)))).isEmpty();
    }

    @Test
    @DisplayName("OR between groups: one matching group is enough, and its conditions form the trace")
    void orBetweenGroups() {
        Rule r = rule("R", 1, 50, ActionType.MONITOR, null, null,
            group(cond("a", Operator.EQ, 1)), group(cond("b", Operator.EQ, 1)));
        RuleEngine.RuleEngineResult result = new RuleEngine().evaluate(List.of(r), facts("a", 0, "b", 1));
        assertThat(ids(result)).containsExactly("R");
        assertThat(result.findings().get(0).trace()).extracting(ConditionTrace::fieldKey).containsExactly("b");
    }

    @Test
    @DisplayName("A rule with an empty condition group never fires")
    void emptyGroupNeverFires() {
        Rule r = rule("R", 1, 50, ActionType.MONITOR, null, null, new ConditionGroup(99L, 0L, "AND", List.of()));
        assertThat(ids(new RuleEngine().evaluate(List.of(r), facts()))).isEmpty();
    }

    // ------------------------------------------------------------------ forward chaining

    @Test
    @DisplayName("Forward chaining: a Level 2 rule reads the conclusion of a Level 1 rule, whatever the list order")
    void forwardChaining() {
        Rule l2 = rule("L2", 2, 50, ActionType.SUGGEST_PROFESSIONAL, null, null, group(cond("depression_mild", Operator.EQ, 1)));
        Rule l1 = rule("L1", 1, 30, ActionType.MONITOR, "depression_mild", "1", group(between("phq9_total", 5, 9)));
        Map<String, Integer> f = facts("phq9_total", 7);
        RuleEngine.RuleEngineResult result = new RuleEngine().evaluate(List.of(l2, l1), f);
        assertThat(ids(result)).containsExactlyInAnyOrder("L1", "L2");
        assertThat(f).containsEntry("depression_mild", 1);
    }

    @Test
    @DisplayName("A non-number conclusion (e.g. 'critical') is stored as 1")
    void textConclusionStoredAsOne() {
        Rule r = rule("R", 1, 50, ActionType.MONITOR, "flag", "critical", group(cond("a", Operator.EQ, 1)));
        Map<String, Integer> f = facts("a", 1);
        new RuleEngine().evaluate(List.of(r), f);
        assertThat(f).containsEntry("flag", 1);
    }

    @Test
    @DisplayName("any_treatment_recommendation is 1 after Level 1 when a finding recommends more than information")
    void anyTreatmentFlag() {
        Rule info = rule("INFO", 1, 10, ActionType.DISPLAY_INFO, null, null, group(cond("a", Operator.EQ, 1)));
        Rule monitor = rule("MON", 1, 20, ActionType.MONITOR, null, null, group(cond("b", Operator.EQ, 1)));
        Map<String, Integer> onlyInfo = facts("a", 1, "b", 0);
        new RuleEngine().evaluate(List.of(info, monitor), onlyInfo);
        assertThat(onlyInfo).containsEntry(RuleEngine.ANY_TREATMENT_KEY, 0);
        Map<String, Integer> both = facts("a", 1, "b", 1);
        new RuleEngine().evaluate(List.of(info, monitor), both);
        assertThat(both).containsEntry(RuleEngine.ANY_TREATMENT_KEY, 1);
    }

    // ------------------------------------------------------------------ overall severity

    @Test
    @DisplayName("Overall severity: when several Level 4 severity rules fire, the highest priority one wins")
    void highestPrioritySeverityWins() {
        Rule mild = rule("MILD", 4, 30, ActionType.MONITOR, RuleEngine.OVERALL_SEVERITY_KEY, "mild", group(cond("a", Operator.EQ, 1)));
        Rule severe = rule("SEVERE", 4, 80, ActionType.URGENT_ASSESSMENT, RuleEngine.OVERALL_SEVERITY_KEY, "severe", group(cond("a", Operator.EQ, 1)));
        assertThat(new RuleEngine().evaluate(List.of(mild, severe), facts("a", 1)).overallSeverity()).isEqualTo("severe");
    }

    @Test
    @DisplayName("No overall severity rule fired → severity is null (never guessed as minimal)")
    void noSeverityRuleGivesNull() {
        Rule r = rule("R", 1, 50, ActionType.MONITOR, null, null, group(cond("a", Operator.EQ, 1)));
        assertThat(new RuleEngine().evaluate(List.of(r), facts("a", 1)).overallSeverity()).isNull();
    }

    @Test
    @DisplayName("Findings are sorted by priority, highest first (ConflictResolver)")
    void findingsSortedByPriority() {
        Rule low = rule("LOW", 1, 10, ActionType.MONITOR, null, null, group(cond("a", Operator.EQ, 1)));
        Rule high = rule("HIGH", 1, 100, ActionType.CRISIS_PROTOCOL, null, null, group(cond("a", Operator.EQ, 1)));
        Rule mid = rule("MID", 2, 50, ActionType.MONITOR, null, null, group(cond("a", Operator.EQ, 1)));
        assertThat(ids(new RuleEngine().evaluate(List.of(low, mid, high), facts("a", 1)))).containsExactly("HIGH", "MID", "LOW");
    }

    // ------------------------------------------------------------------ FindingMapper

    @Test
    @DisplayName("FindingMapper copies the rule and builds a trace with the user's value and the rule's need")
    void findingMapperBuildsTrace() {
        Condition c1 = between("phq9_total", 10, 14);
        Condition c2 = cond("phq9_q9", Operator.GTE, 1);
        Rule r = rule("PHQ9-MODERATE", 1, 50, ActionType.SUGGEST_PROFESSIONAL, "depression_moderate", "1", group(c1, c2));
        Finding f = FindingMapper.toFinding(r, List.of(c1, c2), facts("phq9_total", 12));
        assertThat(f.ruleId()).isEqualTo("PHQ9-MODERATE");
        assertThat(f.actionType()).isEqualTo(ActionType.SUGGEST_PROFESSIONAL);
        assertThat(f.priority()).isEqualTo(50);
        assertThat(f.level()).isEqualTo(1);
        assertThat(f.trace()).hasSize(2);
        assertThat(f.trace().get(0).actualValue()).isEqualTo("12");
        assertThat(f.trace().get(0).requiredValue()).isEqualTo("10 - 14");
        assertThat(f.trace().get(1).actualValue()).as("missing fact shown as 0").isEqualTo("0");
        assertThat(f.trace().get(1).requiredValue()).isEqualTo(">= 1");
    }
}
