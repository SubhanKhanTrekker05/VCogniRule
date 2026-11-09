package com.ooad.mhs.domain;

import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;

import java.util.List;

/**
 * The output of a fired rule. This is what the report displays.
 * 
 * When the RuleEngine evaluates a rule and it fires, the FindingBuilder
 * assembles a Finding from:
 * - The rule's metadata (id, name, category, priority, action, explanation, source)
 * - The matched conditions (as ConditionTrace objects)
 * 
 * The report template iterates over List<Finding> and renders each as:
 * 
 *   ✓ PHQ-9 Moderate Depression (PHQ9-MODERATE)
 *     Matched conditions:
 *       • PHQ-9 Total Score: 14 (threshold: ≥ 10)
 *     → Your responses suggest moderate depressive symptoms...
 *     Source: Kroenke 2001
 * 
 * Findings are NOT persisted. They exist only in memory during the
 * HTTP request and are discarded when the response is sent.
 */
public record Finding(
    String ruleId,                        // e.g., "PHQ9-MODERATE"
    String name,                          // e.g., "Moderate depression"
    Category category,                    // for grouping in report
    Integer priority,                     // for sorting (100=crisis first)
    ActionType actionType,                // for styling (crisis=red, info=blue)
    String explanation,                   // the finding text
    String sourceRef,                     // citation
    Integer level,                        // DAG level (1-4) for report grouping
    List<ConditionTrace> trace            // the matched conditions (explanation facility)
) {}