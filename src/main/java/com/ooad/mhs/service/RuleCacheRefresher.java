package com.ooad.mhs.service;

import com.ooad.mhs.repository.RuleRepository;
import org.springframework.stereotype.Component;

/**
 * Observer pattern: a concrete observer. After any rule change it reloads the active rules
 * kept in memory, so the next screening uses the changed knowledge base.
 */
@Component
public class RuleCacheRefresher implements RuleChangeListener {

    private final RuleRepository ruleRepository;

    public RuleCacheRefresher(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void onRulesChanged(String change) {
        ruleRepository.reloadActiveRules();
    }
}
