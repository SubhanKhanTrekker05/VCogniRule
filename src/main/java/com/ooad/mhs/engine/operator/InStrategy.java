package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;

/** In: {@code actual} is one of the comma-separated numbers in {@code valueText}, e.g. "1,2". */
public class InStrategy implements OperatorStrategy {

    @Override
    public boolean test(int actual, Condition condition) {
        if (condition.valueText() == null) return false;
        for (String s : condition.valueText().split(",")) {
            try {
                if (actual == Integer.parseInt(s.trim())) return true;
            } catch (NumberFormatException e) {
                // Not a number (bad data saved before validation existed): it can never match.
                // Skipping it keeps one bad condition from breaking every screening.
            }
        }
        return false;
    }

    @Override
    public String describe(Condition condition) {
        return "in [" + condition.valueText() + "]";
    }
}
