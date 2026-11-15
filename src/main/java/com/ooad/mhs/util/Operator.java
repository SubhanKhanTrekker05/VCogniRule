package com.ooad.mhs.util;

/**
 * Represents the comparison operators used in rule conditions.
 * 
 * Each operator defines how a field's actual value is compared
 * against the rule's required value.
 * 
 * Each value has its own strategy class (engine.operator package), looked up
 * through OperatorStrategyFactory.
 *
 * Used by: Condition (domain), OperatorStrategyFactory (engine layer)
 * Stored in: rule_condition.operator column
 */
public enum Operator {
    
    /** Equals: {@code actual == required} */
    EQ,
    
    /** Not equals: {@code actual != required} */
    NE,
    
    /** Greater than: {@code actual > required} */
    GT,
    
    /** Greater than or equal: {@code actual >= required} */
    GTE,
    
    /** Less than: {@code actual < required} */
    LT,
    
    /** Less than or equal: {@code actual <= required} */
    LTE,
    
    /** Between: {@code value_min <= actual <= value_max} */
    BETWEEN,
    
    /** In: {@code actual} is one of the values in {@code value_text} (comma-separated) */
    IN
}