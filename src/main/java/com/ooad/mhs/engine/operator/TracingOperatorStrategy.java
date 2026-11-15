package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator pattern: wraps any {@link OperatorStrategy} and adds one thing - it writes each
 * check to the console log. The answer always comes from the wrapped strategy, so results
 * never change.
 *
 * Turned on only by the demo switch {@code vcognirule.trace-operators=true}
 * in application.properties (off by default).
 */
public class TracingOperatorStrategy implements OperatorStrategy {

    private static final Logger log = LoggerFactory.getLogger(TracingOperatorStrategy.class);

    private final OperatorStrategy wrapped;

    public TracingOperatorStrategy(OperatorStrategy wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean test(int actual, Condition condition) {
        boolean result = wrapped.test(actual, condition);
        log.info("[trace] {} = {} needs {} -> {}",
            condition.fieldKey(), actual, wrapped.describe(condition), result);
        return result;
    }

    @Override
    public String describe(Condition condition) {
        return wrapped.describe(condition);
    }

    /** @return the strategy this decorator wraps */
    public OperatorStrategy wrapped() {
        return wrapped;
    }
}
