package com.ooad.mhs.engine;

import com.ooad.mhs.domain.*;
import com.ooad.mhs.util.ActionType;

import java.util.*;

/**
 * The inference engine — multi-pass forward chaining DAG.
 * 
 * Evaluates 150 rules across 4 levels:
 * Pass 1: Level 1 rules (evaluate user responses + derived fields)
 * Pass 2: Level 2 rules (read Level 1 conclusions — composites, hedged, gating, NICE)
 * Pass 3: Level 3 rules (read Level 2 conclusions — higher-order patterns)
 * Pass 4: Level 4 rules (synthesis — overall severity)
 * 
 * Between passes, conclusions from fired rules are added to working_facts
 * so downstream rules can read them. This is forward chaining.
 * 
 * After Pass 1, the engine computes any_treatment_recommendation flag
 * for NICE monitoring rules (125/126).
 * 
 * Pure logic, no Spring annotations. The ScreeningService creates an
 * instance and calls evaluate().
 */
public class RuleEngine {

    /**
     * Result of the evaluation — the findings list + overall severity
     * (null when no overall_severity rule fired).
     */
    public record RuleEngineResult(List<Finding> findings, String overallSeverity) {}

    /**
     * @param rules        All 150 active rules (from RuleRepository cache)
     * @param workingFacts Responses + derived fields (from DerivedFieldCalculator)
     * @return RuleEngineResult with sorted findings + severity
     */
    public RuleEngineResult evaluate(List<Rule> rules, Map<String, Integer> workingFacts) {
        List<Finding> findings = new ArrayList<>();
        // null = no overall-severity rule fired; the caller decides how to present that
        String overallSeverity = null;

        // Pass 1: Level 1 rules
        overallSeverity = evaluatePass(rules, workingFacts, findings, 1, overallSeverity);

        // Compute any_treatment_recommendation after Level 1
        boolean anyTreatment = findings.stream()
            .anyMatch(f -> f.actionType() != ActionType.DISPLAY_INFO);
        workingFacts.put("any_treatment_recommendation", anyTreatment ? 1 : 0);

        // Pass 2: Level 2 rules
        overallSeverity = evaluatePass(rules, workingFacts, findings, 2, overallSeverity);

        // Pass 3: Level 3 rules
        overallSeverity = evaluatePass(rules, workingFacts, findings, 3, overallSeverity);

        // Pass 4: Level 4 rules
        overallSeverity = evaluatePass(rules, workingFacts, findings, 4, overallSeverity);

        // Conflict resolution: sort by priority
        List<Finding> resolved = ConflictResolver.resolve(findings);

        return new RuleEngineResult(resolved, overallSeverity);
    }

    private String evaluatePass(List<Rule> rules, Map<String, Integer> workingFacts,
                                 List<Finding> findings, int level, String currentSeverity) {

        // Filter rules by level, sorted by priority descending
        List<Rule> levelRules = rules.stream()
            .filter(r -> r.level() == level)
            .sorted(Comparator.comparingInt(Rule::priority).reversed())
            .toList();

        String severity = currentSeverity;

        for (Rule rule : levelRules) {
            boolean fired = false;
            List<Condition> matchedConditions = new ArrayList<>();

            // Evaluate each condition group (OR between groups)
            for (ConditionGroup group : rule.groups()) {
                if (group.conditions().isEmpty()) continue;

                boolean groupPassed = true;
                List<Condition> groupMatched = new ArrayList<>();

                // Evaluate each condition (AND within group)
                for (Condition condition : group.conditions()) {
                    if (ConditionEvaluator.evaluate(condition, workingFacts)) {
                        groupMatched.add(condition);
                    } else {
                        groupPassed = false;
                        break; // short-circuit AND
                    }
                }

                if (groupPassed) {
                    fired = true;
                    matchedConditions = groupMatched;
                    break; // short-circuit OR between groups
                }
            }

            if (fired) {
                // Set conclusion in working_facts (for downstream rules)
                if (rule.conclusionKey() != null && rule.conclusionValue() != null) {
                    try {
                        workingFacts.put(rule.conclusionKey(), Integer.parseInt(rule.conclusionValue()));
                    } catch (NumberFormatException e) {
                        // Non-integer conclusion (e.g., "critical") — store as 1
                        workingFacts.put(rule.conclusionKey(), 1);
                    }
                }

                // Capture overall severity from the Level 4 overall_severity rules.
                // Rules are visited highest priority first, so the first one that
                // fires wins (a later "mild" must not overwrite an earlier "critical").
                // Other Level 4 rules (e.g. NICE-OVERALL) do not set the severity.
                if (rule.level() == 4 && severity == null
                        && "overall_severity".equals(rule.conclusionKey())) {
                    severity = rule.conclusionValue();
                }

                // Build finding with trace
                Finding finding = FindingBuilder.build(rule, matchedConditions, workingFacts);
                findings.add(finding);
            }
        }

        return severity;
    }
}