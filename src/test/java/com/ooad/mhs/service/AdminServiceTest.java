package com.ooad.mhs.service;

import com.ooad.mhs.domain.ConditionDetails;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.domain.RuleDetails;
import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;
import com.ooad.mhs.util.Operator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ooad.mhs.security.BCryptPasswordHasher;
import com.ooad.mhs.security.PasswordHasher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the admin business rules, using in-memory fake repositories. */
class AdminServiceTest {

    private Fakes.FakeRuleRepository rules;
    private AdminService service;
    private final List<String> changes = new ArrayList<>();   // a recording observer

    @BeforeEach
    void setUp() {
        Fakes.FakeAdminRepository admins = new Fakes.FakeAdminRepository();
        PasswordHasher hasher = new BCryptPasswordHasher();
        admins.save("admin", hasher.hash("admin123"));
        rules = new Fakes.FakeRuleRepository();
        service = new AdminService(admins, rules, new Fakes.FakeQuestionRepository(), hasher, List.of(changes::add));
    }

    private static RuleDetails details(String ruleId) {
        // name has spaces around it, explanation and conclusion are blank: createRule must clean them up
        return new RuleDetails(ruleId, " Test rule ", 1, Category.MOOD, 50, ActionType.MONITOR, "  ", "src", "", "");
    }

    private Rule onlyRule() {
        return rules.rules.get(0);
    }

    @Test
    @DisplayName("Login: right password works, wrong password or unknown user does not")
    void authenticate() {
        assertThat(service.authenticate("admin", "admin123")).isTrue();
        assertThat(service.authenticate("admin", "wrong")).isFalse();
        assertThat(service.authenticate("nobody", "admin123")).isFalse();
    }

    @Test
    @DisplayName("createRule saves the cleaned-up details (trimmed name, blank texts as null)")
    void createRuleNormalizes() {
        service.createRule(details("NEW-1"));
        Rule r = onlyRule();
        assertThat(r.ruleId()).isEqualTo("NEW-1");
        assertThat(r.name()).isEqualTo("Test rule");
        assertThat(r.explanation()).isNull();
        assertThat(r.conclusionKey()).isNull();
    }

    @Test
    @DisplayName("createRule refuses a rule ID that already exists")
    void duplicateRuleId() {
        service.createRule(details("NEW-1"));
        assertThatThrownBy(() -> service.createRule(details("NEW-1")))
            .isInstanceOf(InvalidInputException.class).hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createRule refuses an empty rule ID")
    void emptyRuleId() {
        assertThatThrownBy(() -> service.createRule(details("")))
            .isInstanceOf(InvalidInputException.class).hasMessage("Rule ID is required.");
    }

    @Test
    void updateUnknownRule() {
        assertThatThrownBy(() -> service.updateRule(999L, details("X")))
            .isInstanceOf(InvalidInputException.class).hasMessageContaining("not found");
    }

    @Test
    @DisplayName("addCondition refuses a field the rule engine does not know")
    void unknownField() {
        service.createRule(details("NEW-1"));
        ConditionDetails c = new ConditionDetails("xyz", Operator.EQ, 1, null, null, null);
        assertThatThrownBy(() -> service.addCondition(onlyRule().id(), c))
            .isInstanceOf(InvalidInputException.class).hasMessage("Unknown field: xyz.");
    }

    @Test
    @DisplayName("addCondition stores only the values the operator uses")
    void addConditionNormalizes() {
        service.createRule(details("NEW-1"));
        service.addCondition(onlyRule().id(), new ConditionDetails("phq9_total", Operator.GTE, 10, "junk", 1, 2));
        var stored = onlyRule().groups().get(0).conditions().get(0);
        assertThat(stored.valueInt()).isEqualTo(10);
        assertThat(stored.valueText()).isNull();
        assertThat(stored.valueMin()).isNull();
    }

    @Test
    @DisplayName("A condition can only be edited or deleted through the rule it belongs to")
    void conditionMustBelongToRule() {
        service.createRule(details("A"));
        service.createRule(details("B"));
        Rule a = rules.rules.get(0);
        Rule b = rules.rules.get(1);
        service.addCondition(a.id(), new ConditionDetails("phq9_total", Operator.GTE, 10, null, null, null));
        Long conditionOfA = a.groups().get(0).conditions().get(0).id();
        ConditionDetails edit = new ConditionDetails("phq9_total", Operator.GTE, 12, null, null, null);
        assertThatThrownBy(() -> service.updateCondition(b.id(), conditionOfA, edit))
            .isInstanceOf(InvalidInputException.class).hasMessageContaining("is not part of rule");
        assertThatThrownBy(() -> service.deleteCondition(b.id(), conditionOfA))
            .isInstanceOf(InvalidInputException.class);
        service.deleteCondition(a.id(), conditionOfA);
        assertThat(a.groups().get(0).conditions()).isEmpty();
    }

    @Test
    @DisplayName("Observer: every change notifies the listeners; a refused change does not")
    void changesNotifyListeners() {
        service.createRule(details("NEW-1"));
        Long id = onlyRule().id();
        service.toggleRuleActive(id, false);
        service.deleteRule(id);
        service.resetKnowledgeBase();
        assertThatThrownBy(() -> service.createRule(details(""))).isInstanceOf(InvalidInputException.class);

        assertThat(changes).containsExactly(
            "rule NEW-1 created", "rule " + id + " deactivated", "rule " + id + " deleted", "knowledge base reset");
    }

    @Test
    @DisplayName("Observer: RuleCacheRefresher reloads the engine's rules after a change")
    void cacheRefresherReloads() {
        int[] reloads = {0};
        Fakes.FakeRuleRepository repo = new Fakes.FakeRuleRepository() {
            @Override
            public void reloadActiveRules() { reloads[0]++; }
        };
        new RuleCacheRefresher(repo).onRulesChanged("rule 1 updated");
        assertThat(reloads[0]).isEqualTo(1);
    }
}
