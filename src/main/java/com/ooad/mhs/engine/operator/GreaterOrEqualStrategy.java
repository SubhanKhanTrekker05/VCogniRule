package com.ooad.mhs.engine.operator;

/** Greater than or equal: {@code actual >= required}. */
public class GreaterOrEqualStrategy extends SingleValueStrategy {

    @Override
    protected boolean compare(int actual, int required) {
        return actual >= required;
    }

    @Override
    protected String symbol() {
        return ">=";
    }
}
