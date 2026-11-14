package com.ooad.mhs.controller;

import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.service.AssessmentService;
import com.ooad.mhs.service.AssessmentService.PreparedAssessment;
import com.ooad.mhs.service.QuestionnaireService;
import com.ooad.mhs.service.ReportService;
import com.ooad.mhs.service.ScreeningService;
import com.ooad.mhs.util.Section;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the screening flow: show form → process submission → show report.
 *
 * GET  /screening              → fresh assessment (clears any saved answers)
 * GET  /screening?resume=CODE  → assessment with the previous answers restored,
 *                                opened on the given section (e.g. resume=GAD-7);
 *                                resume with an empty value opens the first unfinished section
 * POST /screening              → evaluates the completed sections, shows the report
 *
 * The last submitted answers are kept in the HTTP session so the user can
 * return from the report to finish a section without re-entering anything.
 */
@Controller
public class ScreeningController {

    static final String ANSWERS_SESSION_KEY = "screeningAnswers";

    private final QuestionnaireService questionnaireService;
    private final AssessmentService assessmentService;
    private final ScreeningService screeningService;
    private final ReportService reportService;

    public ScreeningController(QuestionnaireService questionnaireService,
                               AssessmentService assessmentService,
                               ScreeningService screeningService,
                               ReportService reportService) {
        this.questionnaireService = questionnaireService;
        this.assessmentService = assessmentService;
        this.screeningService = screeningService;
        this.reportService = reportService;
    }

    /**
     * Shows the tabbed assessment. With ?resume, previous answers are restored.
     */
    @GetMapping("/screening")
    public String showAssessmentForm(@RequestParam(required = false) String resume,
                                     @RequestParam(required = false) String error,
                                     HttpSession session, Model model) {
        Map<String, String> answers = Map.of();
        String resumeSection = null;

        if (resume != null) {
            Object saved = session.getAttribute(ANSWERS_SESSION_KEY);
            if (saved instanceof Map<?, ?> map) {
                Map<String, String> restored = new HashMap<>();
                map.forEach((k, v) -> restored.put(String.valueOf(k), String.valueOf(v)));
                answers = restored;
            }
            // Only accept known section codes (the value is echoed into the page)
            resumeSection = Section.fromCode(resume).map(Section::getCode).orElse(null);
        } else {
            session.removeAttribute(ANSWERS_SESSION_KEY);
        }

        model.addAttribute("sections", questionnaireService.getSections());
        model.addAttribute("answers", answers);
        model.addAttribute("resumeSection", resumeSection);
        model.addAttribute("safetyRequired", "safety".equals(error));
        return "assessment";
    }

    /**
     * Processes the form submission through the full pipeline:
     * Form data → AssessmentService → ScreeningService → ReportService → report view
     */
    @PostMapping("/screening")
    public String submitScreening(@RequestParam Map<String, String> formData,
                                  HttpSession session, Model model) {
        // 0. Keep only real, answered questions and remember them for "complete this section"
        Map<String, String> answers = assessmentService.filterAnswers(formData);
        session.setAttribute(ANSWERS_SESSION_KEY, new HashMap<>(answers));

        // 1. Work out completed sections, convert answers, compute derived fields
        PreparedAssessment prepared = assessmentService.prepare(answers);

        // The Safety Check is mandatory (also enforced in the page)
        if (!prepared.isComplete(Section.CSSRS)) {
            return "redirect:/screening?resume=" + Section.CSSRS.getCode() + "&error=safety";
        }

        // 2. Evaluate all active rules (4-pass forward chaining DAG)
        RuleEngine.RuleEngineResult result = screeningService.evaluate(prepared.workingFacts());

        // 3. Build report with findings, trace, severity, crisis flag, section coverage
        ReportService.ScreeningReport report = reportService.buildReport(result, prepared, answers);

        // 4. Pass to Thymeleaf template
        model.addAttribute("report", report);
        return "report";
    }
}
