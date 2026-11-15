package com.ooad.mhs.config;

import com.ooad.mhs.engine.operator.OperatorStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Demo switch for the Decorator pattern. Reads {@code vcognirule.trace-operators} from
 * application.properties (false by default) and, when true, makes the operator registry
 * wrap every strategy in a TracingOperatorStrategy, so each condition check is logged.
 */
@Component
public class OperatorTracingSwitch {

    private static final Logger log = LoggerFactory.getLogger(OperatorTracingSwitch.class);

    public OperatorTracingSwitch(@Value("${vcognirule.trace-operators:false}") boolean traceOperators) {
        OperatorStrategyRegistry.getInstance().setTracing(traceOperators);
        if (traceOperators) log.info("Operator tracing is ON (Decorator demo)");
    }
}
