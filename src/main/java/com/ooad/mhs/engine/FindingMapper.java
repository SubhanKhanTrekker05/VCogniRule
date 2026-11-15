package com.ooad.mhs.engine;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.engine.operator.OperatorStrategyRegistry;

import java.util.List;
import java.util.Map;

/**
 * Creates a Finding object from a fired rule and its matched conditions.
 *
 * A small helper (not a design pattern): {@link #toFinding} copies the rule's details
 * and turns each matched condition into a ConditionTrace line for the report.
 *
 * Pure logic, no Spring annotations.
 */
public class FindingMapper {

    /**
     * @param rule              The rule that fired
     * @param matchedConditions The conditions that matched (for the trace)
     * @param workingFacts      The facts map (to look up actual values)
     * @return A Finding with trace showing what matched and why
     */
    public static Finding toFinding(Rule rule, List<Condition> matchedConditions,
                                 Map<String, Integer> workingFacts) {

        List<ConditionTrace> trace = matchedConditions.stream()
            .map(c -> new ConditionTrace(
                c.fieldKey(),
                String.valueOf(workingFacts.getOrDefault(c.fieldKey(), 0)),
                OperatorStrategyRegistry.getInstance().strategyFor(c.operator()).describe(c),
                c.operator()
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