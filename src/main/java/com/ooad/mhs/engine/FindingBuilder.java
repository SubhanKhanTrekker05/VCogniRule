package com.ooad.mhs.engine;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.engine.operator.OperatorStrategyFactory;

import java.util.List;
import java.util.Map;

/**
 * Assembles a Finding object from a fired rule and its matched conditions.
 * 
 * Uses the Builder pattern conceptually — takes multiple inputs (rule,
 * matched conditions, working facts) and produces a single Finding
 * with trace data.
 * 
 * Pure logic, no Spring annotations.
 */
public class FindingBuilder {

    /**
     * @param rule              The rule that fired
     * @param matchedConditions The conditions that matched (for the trace)
     * @param workingFacts      The facts map (to look up actual values)
     * @return A Finding with trace showing what matched and why
     */
    public static Finding build(Rule rule, List<Condition> matchedConditions,
                                 Map<String, Integer> workingFacts) {

        List<ConditionTrace> trace = matchedConditions.stream()
            .map(c -> new ConditionTrace(
                c.fieldKey(),
                String.valueOf(workingFacts.getOrDefault(c.fieldKey(), 0)),
                OperatorStrategyFactory.forOperator(c.operator()).describe(c),
                c.operator(),
                true
            ))
            .toList();

        return new Finding(
            rule.ruleId(),
            rule.name(),
            rule.category(),
            rule.priority(),
            rule.actionType(),
            rule.explanation(),
            rule.sourceRef(),
            rule.level(),
            trace
        );
    }
}