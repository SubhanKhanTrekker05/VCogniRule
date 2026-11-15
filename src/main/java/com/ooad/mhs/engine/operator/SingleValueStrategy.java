package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/**
 * Base class for the six operators that compare against one number ({@code valueInt}):
 * EQ, NE, GT, GTE, LT and LTE.
 *
 * Template Method pattern: {@link #test} and {@link #describe} are written once here
 * (check that a value exists, then compare); each subclass only supplies its own
 * comparison and symbol.
 */
public abstract class SingleValueStrategy implements OperatorStrategy {

    @Override
    public final boolean test(int actual, Condition condition) {
        return condition.valueInt() != null && compare(actual, condition.valueInt());
    }

    @Override
    public final String describe(Condition condition) {
        return symbol() + " " + condition.valueInt();
    }

    /** The comparison itself, e.g. {@code actual >= required}. */
    protected abstract boolean compare(int actual, int required);

    /** The symbol shown in the report, e.g. {@code ">="}. */
    protected abstract String symbol();
}
