package com.ooad.mhs.controller;

import com.ooad.mhs.domain.Question;
import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.service.AssessmentService;
import com.ooad.mhs.service.ReportService;
import com.ooad.mhs.service.ScreeningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles the screening flow: show form → process submission → show report.
 * 
 * GET  /screening  → shows the 84-question assessment form (collapsible sections)
 * POST /screening  → evaluates responses through the 4-pass DAG engine, shows report
 */
@Controller
public class ScreeningController {

    private final QuestionRepository questionRepository;
    private final AssessmentService assessmentService;
    private final ScreeningService screeningService;
    private final ReportService reportService;

    public ScreeningController(QuestionRepository questionRepository,
                                AssessmentService assessmentService,
                                ScreeningService screeningService,
                                ReportService reportService) {
        this.questionRepository = questionRepository;
        this.assessmentService = assessmentService;
        this.screeningService = screeningService;
        this.reportService = reportService;
    }

    /**
     * Shows the assessment form with 84 questions grouped by instrument.
     * Each instrument is a collapsible <details> section.
     */
    @GetMapping("/screening")
    public String showAssessmentForm(Model model) {
        List<Question> questions = questionRepository.findAllReal();

        // Group by instrument (preserves order: PHQ-9, GAD-7, PHQ-15, etc.)
        Map<String, List<Question>> byInstrument = questions.stream()
            .collect(Collectors.groupingBy(
                Question::instrument,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        model.addAttribute("questionsByInstrument", byInstrument);
        return "assessment";
    }

    /**
     * Processes the form submission through the full pipeline:
     * Form data → AssessmentService → ScreeningService → ReportService → report view
     */
    @PostMapping("/screening")
    public String submitScreening(@RequestParam Map<String, String> formData, Model model) {
        // 1. Convert form data + compute derived fields
        Map<String, Integer> workingFacts = assessmentService.prepareWorkingFacts(formData);

        // 2. Evaluate all 150 rules (4-pass forward chaining DAG)
        RuleEngine.RuleEngineResult result = screeningService.evaluate(workingFacts);

        // 3. Build report with findings, trace, severity, crisis flag
        ReportService.ScreeningReport report = reportService.buildReport(result);

        // 4. Pass to Thymeleaf template
        model.addAttribute("report", report);
        return "report";
    }
}