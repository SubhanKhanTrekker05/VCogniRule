package com.ooad.mhs.controller;

import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final QuestionRepository questionRepository;

    public AdminController(AdminService adminService, QuestionRepository questionRepository) {
        this.adminService = adminService;
        this.questionRepository = questionRepository;
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("admin") != null;
    }

    @GetMapping
    public String showLoginPage(HttpSession session) {
        if (isLoggedIn(session)) return "redirect:/admin/dashboard";
        return "admin-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, Model model) {
        if (adminService.authenticate(username, password)) {
            session.setAttribute("admin", username);
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
    public String dashboard(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        model.addAttribute("rules", adminService.getAllRules());
        return "admin-rule-list";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, @RequestParam boolean active, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.toggleRuleActive(id, active);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rule/new")
    public String newRuleForm(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        return "admin-rule-new";
    }

    @PostMapping("/rule/new")
    public String createRule(@RequestParam String ruleId, @RequestParam String name,
                             @RequestParam int level, @RequestParam String category,
                             @RequestParam int priority, @RequestParam String actionType,
                             @RequestParam(required = false) String explanation,
                             @RequestParam(required = false) String sourceRef,
                             @RequestParam(required = false) String conclusionKey,
                             @RequestParam(required = false) String conclusionValue,
                             HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.createRule(ruleId, name, level, category, priority, actionType,
            explanation, sourceRef, conclusionKey, conclusionValue);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rule/{id}")
    public String ruleDetail(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        model.addAttribute("rule", adminService.getRuleById(id).orElse(null));
        model.addAttribute("availableFields", questionRepository.findAll());
        return "admin-rule-detail";
    }

    @PostMapping("/rule/{id}")
    public String updateRule(@PathVariable Long id, @RequestParam String name,
                             @RequestParam int level, @RequestParam String category,
                             @RequestParam int priority, @RequestParam String actionType,
                             @RequestParam(required = false) String explanation,
                             @RequestParam(required = false) String sourceRef,
                             @RequestParam(required = false) String conclusionKey,
                             @RequestParam(required = false) String conclusionValue,
                             HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.updateRule(id, name, level, category, priority, actionType,
            explanation, sourceRef, conclusionKey, conclusionValue);
        return "redirect:/admin/rule/" + id;
    }

    @PostMapping("/rule/{id}/delete")
    public String deleteRule(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.deleteRule(id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rule/{id}/condition/new")
    public String newConditionForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        model.addAttribute("ruleId", id);
        model.addAttribute("availableFields", questionRepository.findAll());
        model.addAttribute("isNew", true);
        return "admin-condition-form";
    }

    @PostMapping("/rule/{id}/condition/new")
    public String createCondition(@PathVariable Long id, @RequestParam String fieldKey,
                                  @RequestParam String operator,
                                  @RequestParam(required = false) Integer valueInt,
                                  @RequestParam(required = false) String valueText,
                                  @RequestParam(required = false) Integer valueMin,
                                  @RequestParam(required = false) Integer valueMax,
                                  HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.addCondition(id, fieldKey, operator, valueInt, valueText, valueMin, valueMax);
        return "redirect:/admin/rule/" + id;
    }

    @GetMapping("/rule/{id}/condition/{cid}/edit")
    public String editConditionForm(@PathVariable Long id, @PathVariable Long cid,
                                    HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        model.addAttribute("ruleId", id);
        model.addAttribute("conditionId", cid);
        model.addAttribute("availableFields", questionRepository.findAll());
        model.addAttribute("isNew", false);
        return "admin-condition-form";
    }

    @PostMapping("/rule/{id}/condition/{cid}/edit")
    public String updateCondition(@PathVariable Long id, @PathVariable Long cid,
                                  @RequestParam String fieldKey, @RequestParam String operator,
                                  @RequestParam(required = false) Integer valueInt,
                                  @RequestParam(required = false) String valueText,
                                  @RequestParam(required = false) Integer valueMin,
                                  @RequestParam(required = false) Integer valueMax,
                                  HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.updateCondition(cid, fieldKey, operator, valueInt, valueText, valueMin, valueMax);
        return "redirect:/admin/rule/" + id;
    }

    @PostMapping("/rule/{id}/condition/{cid}/delete")
    public String deleteCondition(@PathVariable Long id, @PathVariable Long cid, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        adminService.deleteCondition(cid);
        return "redirect:/admin/rule/" + id;
    }

    /**
     * Factory reset: restores all 150 rules + 150 groups + 205 conditions to their original state.
     * Undoes every CRUD change made via the admin panel - the "ultimate Ctrl+Z" for viva demos
     * when you've experimented with rules during the demo and want to restore the factory state.
     *
     * Post/Redirect/Get pattern: flashes a toast message and redirects back to the dashboard
     * so the user sees the restored rule list. The actual reset logic lives in AdminService
     * -> RuleRepository.resetKnowledgeBase().
     */
    @PostMapping("/reset")
    public String resetKnowledgeBase(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        int ruleCount = adminService.resetKnowledgeBase();
        redirectAttributes.addFlashAttribute("toast",
            "Knowledge base restored to factory defaults. " + ruleCount + " rules reloaded.");
        return "redirect:/admin/dashboard";
    }
}