package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/** Between: {@code valueMin <= actual <= valueMax} (both ends included). */
public class BetweenStrategy implements OperatorStrategy {

    @Override
    public boolean test(int actual, Condition condition) {
        return condition.valueMin() != null && condition.valueMax() != null
            && actual >= condition.valueMin() && actual <= condition.valueMax();
    }

    @Override
    public String describe(Condition condition) {
        return condition.valueMin() + " - " + condition.valueMax();
    }
}
