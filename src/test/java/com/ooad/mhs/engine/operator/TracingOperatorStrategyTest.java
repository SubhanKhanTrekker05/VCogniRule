package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.util.Operator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Decorator pattern: TracingOperatorStrategy adds logging but never changes a result. */
class TracingOperatorStrategyTest {

    private final OperatorStrategyRegistry registry = OperatorStrategyRegistry.getInstance();

    @AfterEach
    void switchOff() {
        registry.setTracing(false);   // the registry is shared, so leave it as we found it
    }

    @Test
    @DisplayName("Decorator: same answers and same description as the wrapped strategy")
    void sameResultsAsWrapped() {
        Condition c = new Condition(1L, 1L, "phq9_total", Operator.GTE, 10, null, null, null);
        OperatorStrategy plain = new GreaterOrEqualStrategy();
        OperatorStrategy traced = new TracingOperatorStrategy(plain);

        for (int actual = 0; actual <= 27; actual++) {
            assertThat(traced.test(actual, c)).isEqualTo(plain.test(actual, c));
        }
        assertThat(traced.describe(c)).isEqualTo(plain.describe(c));
    }

    @Test
    @DisplayName("Demo switch: on wraps every operator, off gives the plain strategies")
    void switchWrapsStrategies() {
        registry.setTracing(true);
        for (Operator op : Operator.values()) {
            assertThat(registry.strategyFor(op)).isInstanceOf(TracingOperatorStrategy.class);
        }

        registry.setTracing(false);
        for (Operator op : Operator.values()) {
            assertThat(registry.strategyFor(op)).isNotInstanceOf(TracingOperatorStrategy.class);
        }
    }
}
