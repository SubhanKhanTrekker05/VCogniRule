package com.ooad.mhs.engine.operator;

/** Not equals: {@code actual != required}. */
public class NotEqualsStrategy extends SingleValueStrategy {

    @Override
    protected boolean compare(int actual, int required) {
        return actual != required;
    }

    @Override
    protected String symbol() {
        return "!=";
    }
}
