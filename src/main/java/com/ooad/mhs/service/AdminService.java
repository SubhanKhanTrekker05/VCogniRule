package com.ooad.mhs.service;

import com.ooad.mhs.domain.Admin;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.repository.AdminRepository;
import com.ooad.mhs.repository.RuleRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final RuleRepository ruleRepository;

    public AdminService(AdminRepository adminRepository, RuleRepository ruleRepository) {
        this.adminRepository = adminRepository;
        this.ruleRepository = ruleRepository;
    }

    public boolean authenticate(String username, String password) {
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isEmpty()) return false;
        return BCrypt.checkpw(password, admin.get().passwordHash());
    }

    public List<Rule> getAllRules() { return ruleRepository.findAllIncludingInactive(); }

    public Optional<Rule> getRuleById(Long id) { return ruleRepository.findById(id); }

    public void createRule(String ruleId, String name, int level, String category, int priority,
                           String actionType, String explanation, String sourceRef,
                           String conclusionKey, String conclusionValue) {
        ruleRepository.save(ruleId, name, level, category, priority, actionType, explanation, sourceRef, conclusionKey, conclusionValue);
    }

    public void updateRule(Long id, String name, int level, String category, int priority,
                           String actionType, String explanation, String sourceRef,
                           String conclusionKey, String conclusionValue) {
        ruleRepository.updateRule(id, name, level, category, priority, actionType, explanation, sourceRef, conclusionKey, conclusionValue);
    }

    public void deleteRule(Long id) { ruleRepository.deleteById(id); }

    public void toggleRuleActive(Long id, boolean active) { ruleRepository.toggleActive(id, active); }

    public void addCondition(Long ruleId, String fieldKey, String operator,
                             Integer valueInt, String valueText, Integer valueMin, Integer valueMax) {
        ruleRepository.addCondition(ruleId, fieldKey, operator, valueInt, valueText, valueMin, valueMax);
    }

    public void updateCondition(Long conditionId, String fieldKey, String operator,
                                Integer valueInt, String valueText, Integer valueMin, Integer valueMax) {
        ruleRepository.updateCondition(conditionId, fieldKey, operator, valueInt, valueText, valueMin, valueMax);
    }
    public void deleteCondition(Long conditionId) { ruleRepository.deleteCondition(conditionId); }

    /**
     * Factory reset: restores all 150 rules + 150 groups + 205 conditions to their original state.
     * Undoes every CRUD change made via the admin panel (added/edited/deactivated/deleted rules).
     *
     * Used by the "Reset KB" button on the rule management dashboard - the ultimate Ctrl+Z
     * for viva demos when you've experimented with rules and want to restore the factory state.
     *
     * Delegates to RuleRepository which performs the actual DB wipe + re-seed + cache refresh.
     *
     * @return number of rules after reset (should always be 150)
     */
    public int resetKnowledgeBase() {
        return ruleRepository.resetKnowledgeBase();
    }
}