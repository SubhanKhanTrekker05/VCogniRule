package com.ooad.mhs.engine.operator;

/** Greater than: {@code actual > required}. */
public class GreaterThanStrategy extends SingleValueStrategy {

    @Override
    protected boolean compare(int actual, int required) {
        return actual > required;
    }

    @Override
    protected String symbol() {
        return ">";
    }
}
