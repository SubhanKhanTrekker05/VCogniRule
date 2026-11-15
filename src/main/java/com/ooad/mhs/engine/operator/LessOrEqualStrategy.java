package com.ooad.mhs.engine.operator;

/** Less than or equal: {@code actual <= required}. */
public class LessOrEqualStrategy extends SingleValueStrategy {

    @Override
    protected boolean compare(int actual, int required) {
        return actual <= required;
    }

    @Override
    protected String symbol() {
        return "<=";
    }
}
