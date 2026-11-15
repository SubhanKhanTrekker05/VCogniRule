package com.ooad.mhs.util;

import com.ooad.mhs.engine.operator.*;

/**
 * The comparison operators used in rule conditions (stored in rule_condition.operator).
 *
 * Factory Method pattern: Operator (the Creator) declares the factory method
 * {@link #createStrategy()}; each constant (a Concrete Creator) overrides it and decides
 * which {@link OperatorStrategy} class (the Product) to create.
 * OperatorStrategyRegistry calls it once per operator and shares the result.
 */
public enum Operator {

    /** Equals: {@code actual == required} */
    EQ { public OperatorStrategy createStrategy() { return new EqualsStrategy(); } },

    /** Not equals: {@code actual != required} */
    NE { public OperatorStrategy createStrategy() { return new NotEqualsStrategy(); } },

    /** Greater than: {@code actual > required} */
    GT { public OperatorStrategy createStrategy() { return new GreaterThanStrategy(); } },

    /** Greater than or equal: {@code actual >= required} */
    GTE { public OperatorStrategy createStrategy() { return new GreaterOrEqualStrategy(); } },

    /** Less than: {@code actual < required} */
    LT { public OperatorStrategy createStrategy() { return new LessThanStrategy(); } },

    /** Less than or equal: {@code actual <= required} */
    LTE { public OperatorStrategy createStrategy() { return new LessOrEqualStrategy(); } },

    /** Between: {@code value_min <= actual <= value_max} */
    BETWEEN { public OperatorStrategy createStrategy() { return new BetweenStrategy(); } },

    /** In: {@code actual} is one of the values in {@code value_text} (comma-separated) */
    IN { public OperatorStrategy createStrategy() { return new InStrategy(); } };

    /** The factory method: creates the strategy object for this operator. */
    public abstract OperatorStrategy createStrategy();
}
