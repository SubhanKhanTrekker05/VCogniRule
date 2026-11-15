package com.ooad.mhs.engine;

import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.engine.operator.OperatorStrategyFactory;

import java.util.Map;

/**
 * Evaluates a single condition against the working facts map.
 *
 * Pure logic, no Spring annotations. Static methods only.
 *
 * Given a Condition (field_key, operator, value) and the working facts,
 * returns true if the condition is satisfied, false otherwise.
 * The comparison itself is delegated to the operator's strategy
 * (see {@link com.ooad.mhs.engine.operator.OperatorStrategy}).
 */
public class ConditionEvaluator {

    /**
     * @param condition  The condition to evaluate
     * @param workingFacts  The current facts map (responses + derived fields + conclusions)
     * @return true if the condition is satisfied
     */
    public static boolean evaluate(Condition condition, Map<String, Integer> workingFacts) {
        String fieldKey = condition.fieldKey();

        // Field not found in facts → condition fails
        if (!workingFacts.containsKey(fieldKey)) {
            return false;
        }

        int actual = workingFacts.get(fieldKey);

        return OperatorStrategyFactory.forOperator(condition.operator()).test(actual, condition);
    }
}
