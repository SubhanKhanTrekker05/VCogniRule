package com.ooad.mhs.engine.operator;

/** Equals: {@code actual == required}. */
public class EqualsStrategy extends SingleValueStrategy {

    @Override
    protected boolean compare(int actual, int required) {
        return actual == required;
    }

    @Override
    protected String symbol() {
        return "==";
    }
}
