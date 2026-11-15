package com.ooad.mhs.engine.operator;

import com.ooad.mhs.util.Operator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory pattern: returns the {@link OperatorStrategy} for an {@link Operator}.
 *
 * The strategies hold no state, so one shared instance per operator is created once
 * and reused for every condition.
 */
public final class OperatorStrategyFactory {

    private static final Map<Operator, OperatorStrategy> STRATEGIES = new EnumMap<>(Operator.class);

    static {
        STRATEGIES.put(Operator.EQ, new EqualsStrategy());
        STRATEGIES.put(Operator.NE, new NotEqualsStrategy());
        STRATEGIES.put(Operator.GT, new GreaterThanStrategy());
        STRATEGIES.put(Operator.GTE, new GreaterOrEqualStrategy());
        STRATEGIES.put(Operator.LT, new LessThanStrategy());
        STRATEGIES.put(Operator.LTE, new LessOrEqualStrategy());
        STRATEGIES.put(Operator.BETWEEN, new BetweenStrategy());
        STRATEGIES.put(Operator.IN, new InStrategy());
    }

    /** Not meant to be instantiated: use {@link #forOperator}. */
    private OperatorStrategyFactory() {
    }

    /**
     * @param operator the operator stored on a condition
     * @return the matching strategy (never null)
     * @throws IllegalArgumentException if no strategy is registered for the operator
     */
    public static OperatorStrategy forOperator(Operator operator) {
        OperatorStrategy strategy = STRATEGIES.get(operator);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy registered for operator " + operator);
        }
        return strategy;
    }
}
