package com.ooad.mhs.controller;

import com.ooad.mhs.config.AdminAuthInterceptor;
import com.ooad.mhs.domain.ConditionDetails;
import com.ooad.mhs.domain.Rule;
import com.ooad.mhs.domain.RuleDetails;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.service.AdminService;
import com.ooad.mhs.service.InvalidInputException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The admin panel (knowledge acquisition): log in, list rules, create / edit / delete / switch rules
 * and their conditions, and factory-reset the knowledge base.
 *
 * Every URL under /admin except login and logout is protected by AdminAuthInterceptor, so the
 * methods here can assume an admin is logged in. Each change is passed to AdminService and then
 * redirected, so refreshing the page never repeats the change.
 *
 * Form input arrives as a RuleDetails / ConditionDetails object. {@code @Valid} checks its
 * validation rules first; a broken rule (or an InvalidInputException from the service) sends the
 * admin back to the same form with the message in "formError" instead of an error page.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final QuestionRepository questionRepository;

    public AdminController(AdminService adminService, QuestionRepository questionRepository) {
        this.adminService = adminService;
        this.questionRepository = questionRepository;
    }

    @GetMapping
    public String showLoginPage(HttpSession session) {
        if (AdminAuthInterceptor.isLoggedIn(session)) return "redirect:/admin/dashboard";
        return "admin-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, Model model) {
        if (adminService.authenticate(username, password)) {
            session.setAttribute(AdminAuthInterceptor.SESSION_ATTRIBUTE, username);
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("error", "Invalid username or password");
        return "admin-login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("rules", adminService.getAllRules());
        return "admin-rule-list";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, @RequestParam boolean active) {
        adminService.toggleRuleActive(id, active);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rule/new")
    public String newRuleForm(Model model) {
        return "admin-rule-new";
    }

    @PostMapping("/rule/new")
    public String createRule(@Valid @ModelAttribute("form") RuleDetails form, BindingResult errors,
                             RedirectAttributes redirectAttributes) {
        String back = "redirect:/admin/rule/new";
        if (errors.hasErrors()) return withError(redirectAttributes, back, messages(errors), form);
        try {
            adminService.createRule(form);
        } catch (InvalidInputException e) {
            return withError(redirectAttributes, back, e.getMessage(), form);
        }
        redirectAttributes.addFlashAttribute("toast", "Rule " + form.ruleId().trim() + " created.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rule/{id}")
    public String ruleDetail(@PathVariable Long id, Model model,
                             RedirectAttributes redirectAttributes) {
        Optional<Rule> rule = adminService.getRuleById(id);
        if (rule.isEmpty()) {
            redirectAttributes.addFlashAttribute("toast", "Rule " + id + " was not found.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("rule", rule.get());
        model.addAttribute("availableFields", questionRepository.findAll());
        return "admin-rule-detail";
    }

    @PostMapping("/rule/{id}")
    public String updateRule(@PathVariable Long id, @Valid @ModelAttribute("form") RuleDetails form,
                             BindingResult errors, RedirectAttributes redirectAttributes) {
        String back = "redirect:/admin/rule/" + id;
        if (errors.hasErrors()) return withError(redirectAttributes, back, messages(errors), form);
        try {
            adminService.updateRule(id, form);
        } catch (InvalidInputException e) {
            return withError(redirectAttributes, back, e.getMessage(), form);
        }
        return back;
    }

    @PostMapping("/rule/{id}/delete")
    public String deleteRule(@PathVariable Long id) {
        adminService.deleteRule(id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rule/{id}/condition/new")
    public String newConditionForm(@PathVariable Long id, Model model) {
        model.addAttribute("ruleId", id);
        model.addAttribute("availableFields", questionRepository.findAll());
        model.addAttribute("isNew", true);
        return "admin-condition-form";
    }

    @PostMapping("/rule/{id}/condition/new")
    public String createCondition(@PathVariable Long id, @Valid @ModelAttribute("form") ConditionDetails form,
                                  BindingResult errors, RedirectAttributes redirectAttributes) {
        String back = "redirect:/admin/rule/" + id + "/condition/new";
        if (errors.hasErrors()) return withError(redirectAttributes, back, messages(errors), form);
        try {
            adminService.addCondition(id, form);
        } catch (InvalidInputException e) {
            return withError(redirectAttributes, back, e.getMessage(), form);
        }
        return "redirect:/admin/rule/" + id;
    }

    @GetMapping("/rule/{id}/condition/{cid}/edit")
    public String editConditionForm(@PathVariable Long id, @PathVariable Long cid, Model model) {
        model.addAttribute("ruleId", id);
        model.addAttribute("conditionId", cid);
        model.addAttribute("availableFields", questionRepository.findAll());
        model.addAttribute("isNew", false);
        // Pre-fill the form with the saved condition, unless the admin is coming back from a
        // failed save (then "form" already holds what they typed).
        if (!model.containsAttribute("form")) {
            adminService.getRuleById(id).ifPresent(rule -> rule.groups().stream()
                .flatMap(group -> group.conditions().stream())
                .filter(c -> c.id().equals(cid))
                .findFirst()
                .ifPresent(c -> model.addAttribute("form", new ConditionDetails(
                    c.fieldKey(), c.operator(), c.valueInt(), c.valueText(), c.valueMin(), c.valueMax()))));
        }
        return "admin-condition-form";
    }

    @PostMapping("/rule/{id}/condition/{cid}/edit")
    public String updateCondition(@PathVariable Long id, @PathVariable Long cid,
                                  @Valid @ModelAttribute("form") ConditionDetails form, BindingResult errors,
                                  RedirectAttributes redirectAttributes) {
        String back = "redirect:/admin/rule/" + id + "/condition/" + cid + "/edit";
        if (errors.hasErrors()) return withError(redirectAttributes, back, messages(errors), form);
        try {
            adminService.updateCondition(id, cid, form);
        } catch (InvalidInputException e) {
            return withError(redirectAttributes, back, e.getMessage(), form);
        }
        return "redirect:/admin/rule/" + id;
    }

    @PostMapping("/rule/{id}/condition/{cid}/delete")
    public String deleteCondition(@PathVariable Long id, @PathVariable Long cid,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteCondition(id, cid);
        } catch (InvalidInputException e) {
            redirectAttributes.addFlashAttribute("formError", e.getMessage());
        }
        return "redirect:/admin/rule/" + id;
    }

    /**
     * Factory reset: restores all 150 rules + 150 groups + 209 conditions to their original state.
     * Undoes every CRUD change made via the admin panel - the "ultimate Ctrl+Z" for viva demos
     * when you've experimented with rules during the demo and want to restore the factory state.
     *
     * Flashes a toast message and redirects back to the dashboard
     * so the user sees the restored rule list. The actual reset logic lives in AdminService
     * -> RuleRepository.resetKnowledgeBase().
     */
    @PostMapping("/reset")
    public String resetKnowledgeBase(RedirectAttributes redirectAttributes) {
        int ruleCount = adminService.resetKnowledgeBase();
        redirectAttributes.addFlashAttribute("toast",
            "Knowledge base restored to factory defaults. " + ruleCount + " rules reloaded.");
        return "redirect:/admin/dashboard";
    }

    // ------------------------------------------------------------------ form error helpers

    /** Sends the admin back to a form with a message and the values they typed, so nothing has to be re-entered. */
    private static String withError(RedirectAttributes redirectAttributes, String back, String message, Object form) {
        redirectAttributes.addFlashAttribute("formError", message);
        if (form != null) redirectAttributes.addFlashAttribute("form", form);
        return back;
    }

    /** All validation messages as one line. A value that could not be read (e.g. "abc" as a number) names its field. */
    private static String messages(BindingResult errors) {
        return errors.getAllErrors().stream()
            .map(e -> e instanceof FieldError fe && fe.isBindingFailure()
                ? "Invalid value for " + fe.getField() + ": " + fe.getRejectedValue() + "."
                : e.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining(" "));
    }
}
