package com.ooad.mhs.domain;

import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;

import java.util.List;

/**
 * Represents one rule in the knowledge base.
 * 
 * A Rule is the atomic unit of the inference engine. When its conditions
 * are satisfied, it "fires" — producing a Finding and optionally setting
 * a conclusion that downstream rules can read.
 * 
 * Rules are organized into 4 DAG levels:
 * - Level 1: Evaluate user responses directly (78 rules)
 * - Level 2: Read Level 1 conclusions — composites, hedged, gating, NICE (57 rules)
 * - Level 3: Read Level 2 conclusions — higher-order patterns (12 rules)
 * - Level 4: Synthesis — overall severity (6 rules, includes NICE-OVERALL)
 * 
 * The engine evaluates rules in level order (multi-pass forward chaining):
 * Pass 1 evaluates Level 1, Pass 2 evaluates Level 2, etc.
 * Conclusions set by earlier passes are available to later passes.
 * 
 * Stored in: rule table (150 rows)
 * Groups loaded from: rule_condition_group table (150 rows)
 * Conditions loaded from: rule_condition table (205 rows)
 */
public record Rule(
    Long id,
    String ruleId,                       // e.g., "PHQ9-MODERATE"
    String name,                         // e.g., "Moderate depression"
    Integer level,                       // 1, 2, 3, or 4 (DAG level)
    Category category,                   // MOOD, ANXIETY, RISK, CROSS_CUTTING, etc.
    Integer priority,                    // 0-100 (higher = more urgent, sorted desc)
    ActionType actionType,               // DISPLAY_INFO, MONITOR, CRISIS_PROTOCOL, etc.
    String explanation,                  // Human-readable finding text
    String sourceRef,                    // Citation (e.g., "Kroenke 2001")
    String conclusionKey,                // What fact to set when fired (e.g., "depression_moderate")
    String conclusionValue,              // Value to set (e.g., "1" or "critical")
    boolean isActive,                    // Admin can toggle without deleting
    List<ConditionGroup> groups          // The condition groups (OR between groups, AND within)
) {}