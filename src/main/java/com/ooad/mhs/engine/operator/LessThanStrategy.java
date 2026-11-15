package com.ooad.mhs.engine.operator;

/** Less than: {@code actual < required}. */
public class LessThanStrategy extends SingleValueStrategy {

    @Override
    protected boolean compare(int actual, int required) {
        return actual < required;
    }

    @Override
    protected String symbol() {
        return "<";
    }
}
