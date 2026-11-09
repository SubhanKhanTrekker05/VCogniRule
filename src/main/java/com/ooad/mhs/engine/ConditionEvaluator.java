package com.ooad.mhs.engine;

import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.util.Operator;

import java.util.Map;

/**
 * Evaluates a single condition against the working facts map.
 * 
 * Pure logic, no Spring annotations. Static methods only.
 * 
 * Given a Condition (field_key, operator, value) and the working facts,
 * returns true if the condition is satisfied, false otherwise.
 */
public class ConditionEvaluator {

    /**
     * @param condition  The condition to evaluate
     * @param workingFacts  The current facts map (responses + derived fields + conclusions)
     * @return true if the condition is satisfied
     */
    public static boolean evaluate(Condition condition, Map<String, Integer> workingFacts) {
        String fieldKey = condition.fieldKey();
        Operator operator = condition.operator();

        // Field not found in facts → condition fails
        if (!workingFacts.containsKey(fieldKey)) {
            return false;
        }

        int actual = workingFacts.get(fieldKey);

        return switch (operator) {
            case EQ       -> condition.valueInt() != null && actual == condition.valueInt();
            case NE       -> condition.valueInt() != null && actual != condition.valueInt();
            case GT       -> condition.valueInt() != null && actual > condition.valueInt();
            case GTE      -> condition.valueInt() != null && actual >= condition.valueInt();
            case LT       -> condition.valueInt() != null && actual < condition.valueInt();
            case LTE      -> condition.valueInt() != null && actual <= condition.valueInt();
            case BETWEEN  -> condition.valueMin() != null && condition.valueMax() != null
                                && actual >= condition.valueMin() && actual <= condition.valueMax();
            case IN       -> condition.valueText() != null && evaluateIn(actual, condition.valueText());
        };
    }

    private static boolean evaluateIn(int actual, String commaSeparatedValues) {
        for (String s : commaSeparatedValues.split(",")) {
            if (actual == Integer.parseInt(s.trim())) return true;
        }
        return false;
    }
}