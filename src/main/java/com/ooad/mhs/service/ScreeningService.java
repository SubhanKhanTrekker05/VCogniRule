package com.ooad.mhs.service;

import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.repository.RuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the rule engine evaluation.
 * 
 * Takes the working_facts map (from AssessmentService) and passes it
 * to the RuleEngine along with all 150 cached rules.
 * 
 * Returns the RuleEngineResult containing sorted findings + overall severity.
 */
@Service
public class ScreeningService {

    private final RuleRepository ruleRepository;

    public ScreeningService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * Evaluates the screening responses against all 150 rules.
     * 
     * @param workingFacts  Responses + derived fields (from AssessmentService)
     * @return RuleEngineResult with findings sorted by priority + severity
     */
    public RuleEngine.RuleEngineResult evaluate(Map<String, Integer> workingFacts) {
        List<Rule> rules = ruleRepository.getActiveRules();
        RuleEngine engine = new RuleEngine();
        return engine.evaluate(rules, workingFacts);
    }
}