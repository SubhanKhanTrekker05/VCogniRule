package com.ooad.mhs.util;

/**
 * Represents the comparison operators used in rule conditions.
 * 
 * Each operator defines how a field's actual value is compared
 * against the rule's required value.
 * 
 * Used by: ConditionEvaluator (engine layer)
 * Stored in: rule_condition.operator column
 */
public enum Operator {
    
    /** Equals: actual == required */
    EQ,
    
    /** Not equals: actual != required */
    NE,
    
    /** Greater than: actual > required */
    GT,
    
    /** Greater than or equal: actual >= required */
    GTE,
    
    /** Less than: actual < required */
    LT,
    
    /** Less than or equal: actual <= required */
    LTE,
    
    /** Between: value_min <= actual <= value_max */
    BETWEEN,
    
    /** In: actual is one of the values in value_text (comma-separated) */
    IN
}