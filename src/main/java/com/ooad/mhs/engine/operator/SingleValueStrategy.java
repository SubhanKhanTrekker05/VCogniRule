package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/**
 * Abstract base class of the six operators that compare against one number ({@code valueInt}):
 * EQ, NE, GT, GTE, LT and LTE. It only offers two shared helpers; each subclass implements
 * {@link #test} and {@link #describe} itself.
 */
public abstract class SingleValueStrategy implements OperatorStrategy {

    /** True if the condition has the number this operator needs. */
    protected static boolean hasValue(Condition condition) {
        return condition.valueInt() != null;
    }

    /** The text shown under "Rule needs", e.g. {@code ">= 10"}. */
    protected static String describeWith(String symbol, Condition condition) {
        return symbol + " " + condition.valueInt();
    }
}
