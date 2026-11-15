package com.ooad.mhs.engine;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.engine.operator.OperatorStrategyFactory;

import java.util.List;
import java.util.Map;

/**
 * Creates a Finding object from a fired rule and its matched conditions.
 *
 * Factory (static factory method): {@link #create} hides how a Finding and its
 * trace are put together. The caller (RuleEngine) passes the rule, the matched
 * conditions and the working facts and gets back a finished Finding.
 *
 * Pure logic, no Spring annotations.
 */
public class FindingFactory {

    /**
     * @param rule              The rule that fired
     * @param matchedConditions The conditions that matched (for the trace)
     * @param workingFacts      The facts map (to look up actual values)
     * @return A Finding with trace showing what matched and why
     */
    public static Finding create(Rule rule, List<Condition> matchedConditions,
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