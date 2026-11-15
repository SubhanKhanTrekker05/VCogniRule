package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/** Not equals: {@code actual != required}. */
public class NotEqualsStrategy extends SingleValueStrategy {

    @Override
    public boolean test(int actual, Condition condition) {
        return hasValue(condition) && actual != condition.valueInt();
    }

    @Override
    public String describe(Condition condition) {
        return describeWith("!=", condition);
    }
}
