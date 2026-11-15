package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/** Greater than: {@code actual > required}. */
public class GreaterThanStrategy extends SingleValueStrategy {

    @Override
    public boolean test(int actual, Condition condition) {
        return hasValue(condition) && actual > condition.valueInt();
    }

    @Override
    public String describe(Condition condition) {
        return describeWith(">", condition);
    }
}
