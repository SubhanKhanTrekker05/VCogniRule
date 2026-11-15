package com.ooad.mhs.service;

import com.ooad.mhs.domain.Admin;
import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.domain.ConditionDetails;
import com.ooad.mhs.domain.Question;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.domain.RuleDetails;
import com.ooad.mhs.repository.AdminRepository;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.repository.RuleRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Business operations of the admin panel.
 *
 * Facade: the controller talks only to this class, which hides the two repositories behind it
 * (AdminRepository for login, RuleRepository for the knowledge base). Passwords are checked with
 * BCrypt, so the plain password is never stored or compared directly.
 *
 * It also checks the business rules that need the database (form rules such as "priority 0-100"
 * are checked earlier by Bean Validation on RuleDetails / ConditionDetails):
 * a new rule ID must be unique, a condition field must exist, and a condition must belong to the
 * rule it is edited through. A broken rule throws {@link InvalidInputException}.
 */
@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final RuleRepository ruleRepository;
    private final QuestionRepository questionRepository;

    public AdminService(AdminRepository adminRepository, RuleRepository ruleRepository,
                        QuestionRepository questionRepository) {
        this.adminRepository = adminRepository;
        this.ruleRepository = ruleRepository;
        this.questionRepository = questionRepository;
    }

    public boolean authenticate(String username, String password) {
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isEmpty()) return false;
        return BCrypt.checkpw(password, admin.get().passwordHash());
    }

    public List<Rule> getAllRules() { return ruleRepository.findAllIncludingInactive(); }

    public Optional<Rule> getRuleById(Long id) { return ruleRepository.findById(id); }

    /** @throws InvalidInputException if the rule ID is missing or already used */
    public void createRule(RuleDetails details) {
        RuleDetails d = details.normalized();
        if (d.ruleId() == null || d.ruleId().isEmpty()) throw new InvalidInputException("Rule ID is required.");
        boolean taken = ruleRepository.findAllIncludingInactive().stream()
            .anyMatch(r -> r.ruleId().equals(d.ruleId()));
        if (taken) throw new InvalidInputException("Rule ID " + d.ruleId() + " already exists.");
        ruleRepository.save(d);
    }

    /** @throws InvalidInputException if the rule does not exist */
    public void updateRule(Long id, RuleDetails details) {
        requireRule(id);
        ruleRepository.updateRule(id, details.normalized());
    }

    public void deleteRule(Long id) { ruleRepository.deleteById(id); }

    public void toggleRuleActive(Long id, boolean active) { ruleRepository.toggleActive(id, active); }

    /** @throws InvalidInputException if the rule does not exist or the field is unknown */
    public void addCondition(Long ruleId, ConditionDetails condition) {
        requireRule(ruleId);
        requireKnownField(condition.fieldKey());
        ruleRepository.addCondition(ruleId, condition.normalized());
    }

    /** @throws InvalidInputException if the condition is not part of the rule, or the field is unknown */
    public void updateCondition(Long ruleId, Long conditionId, ConditionDetails condition) {
        requireConditionOfRule(ruleId, conditionId);
        requireKnownField(condition.fieldKey());
        ruleRepository.updateCondition(conditionId, condition.normalized());
    }

    /** @throws InvalidInputException if the condition is not part of the rule */
    public void deleteCondition(Long ruleId, Long conditionId) {
        requireConditionOfRule(ruleId, conditionId);
        ruleRepository.deleteCondition(conditionId);
    }

    // ------------------------------------------------------------------ business-rule checks

    private Rule requireRule(Long id) {
        return ruleRepository.findById(id)
            .orElseThrow(() -> new InvalidInputException("Rule " + id + " was not found."));
    }

    /** The field must be a question or a calculated field (the rule engine can only read those). */
    private void requireKnownField(String fieldKey) {
        boolean known = questionRepository.findAll().stream().map(Question::fieldKey).anyMatch(fieldKey::equals);
        if (!known) throw new InvalidInputException("Unknown field: " + fieldKey + ".");
    }

    private void requireConditionOfRule(Long ruleId, Long conditionId) {
        boolean belongs = requireRule(ruleId).groups().stream()
            .flatMap(g -> g.conditions().stream()).map(Condition::id).anyMatch(conditionId::equals);
        if (!belongs) throw new InvalidInputException("Condition " + conditionId + " is not part of rule " + ruleId + ".");
    }

    /**
     * Factory reset: restores all 150 rules + 150 groups + 209 conditions to their original state.
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