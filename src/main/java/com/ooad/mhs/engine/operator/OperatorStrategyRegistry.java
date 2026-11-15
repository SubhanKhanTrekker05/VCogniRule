package com.ooad.mhs.engine.operator;

import com.ooad.mhs.util.Operator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Singleton pattern: the one shared set of operator strategies.
 *
 * The strategies hold no data, so one object per operator is enough for the whole app.
 * The private constructor creates them once, through the Factory Method
 * {@link Operator#createStrategy()}; everyone else uses {@link #getInstance()}.
 */
public final class OperatorStrategyRegistry {

    /** The single instance, created when the class is first used. */
    private static final OperatorStrategyRegistry INSTANCE = new OperatorStrategyRegistry();

    private final Map<Operator, OperatorStrategy> strategies = new EnumMap<>(Operator.class);

    /** Private: no other class can create a second registry. */
    private OperatorStrategyRegistry() {
        for (Operator op : Operator.values()) {
            strategies.put(op, op.createStrategy());   // Factory Method
        }
    }

    /** @return the single shared registry */
    public static OperatorStrategyRegistry getInstance() {
        return INSTANCE;
    }

    /** @return the shared strategy for the operator (never null) */
    public OperatorStrategy strategyFor(Operator operator) {
        return strategies.get(operator);
    }
}
