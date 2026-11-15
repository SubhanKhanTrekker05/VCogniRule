package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/**
 * Strategy pattern: one comparison operator of a rule condition (EQ, GTE, BETWEEN, ...).
 *
 * Each operator is its own class behind this interface, so the engine never needs a
 * switch on the operator. Adding an operator means adding a class (Open/Closed Principle).
 * Instances are created by the Factory Method Operator.createStrategy() and shared
 * through the Singleton {@link OperatorStrategyRegistry}; the Decorator
 * {@link TracingOperatorStrategy} can wrap any of them (demo switch).
 */
public interface OperatorStrategy {

    /**
     * Checks the user's value against the condition.
     *
     * @param actual    the value from the working facts (e.g. phq9_total = 14)
     * @param condition the condition holding the required value(s)
     * @return true if the condition is satisfied
     */
    boolean test(int actual, Condition condition);

    /**
     * Describes what the condition requires, e.g. {@code ">= 10"}, {@code "5 - 9"} or {@code "in [1,2]"}.
     * Shown in the report's "Why this appears" list.
     */
    String describe(Condition condition);
}
