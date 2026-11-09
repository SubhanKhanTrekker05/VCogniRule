package com.ooad.mhs.util;

/**
 * Represents the action the system recommends when a rule fires.
 * 
 * Ordered from least urgent (DISPLAY_INFO) to most urgent (CRISIS_PROTOCOL).
 * The ConflictResolver uses this to sort findings by urgency.
 * 
 * Used by: Rule (domain), FindingBuilder (engine), ReportService (service)
 * Stored in: rule.action_type column
 */
public enum ActionType {
    
    /** Informational message, no clinical concern. Lowest urgency. */
    DISPLAY_INFO,
    
    /** Monitor symptoms, consider self-care. Low urgency. */
    MONITOR,
    
    /** Suggest discussing with a professional. Moderate urgency. */
    SUGGEST_PROFESSIONAL,
    
    /** Professional assessment recommended. High urgency. */
    RECOMMEND_ASSESSMENT,
    
    /** Urgent assessment needed. Very high urgency. */
    URGENT_ASSESSMENT,
    
    /** Crisis protocol — immediate emergency response. Highest urgency. */
    CRISIS_PROTOCOL
}