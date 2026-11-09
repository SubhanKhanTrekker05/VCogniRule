package com.ooad.mhs.service;

import com.ooad.mhs.domain.Finding;
import com.ooad.mhs.domain.Question;
import com.ooad.mhs.engine.RuleEngine;
import com.ooad.mhs.repository.QuestionRepository;
import com.ooad.mhs.util.ActionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the report model that the Thymeleaf template renders.
 * 
 * Takes the RuleEngineResult and adds:
 * - Field labels (field_key → human-readable text) for trace display
 * - Crisis flag (whether any crisis rule fired)
 * - Overall severity string
 * 
 * The ScreeningReport record is passed to the controller, which adds it
 * to the Spring Model for the report.html template.
 */
@Service
public class ReportService {

    private final QuestionRepository questionRepository;

    public ReportService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Builds the complete report data for the Thymeleaf template.
     */
    public ScreeningReport buildReport(RuleEngine.RuleEngineResult engineResult) {
        List<Finding> findings = engineResult.findings();
        String severity = engineResult.overallSeverity();

        // Check if any crisis rule fired
        boolean hasCrisis = findings.stream()
            .anyMatch(f -> f.actionType() == ActionType.CRISIS_PROTOCOL);

        // Build field labels map for trace display (field_key → human text)
        Map<String, String> fieldLabels = questionRepository.findAll().stream()
            .collect(Collectors.toMap(Question::fieldKey, Question::text, (a, b) -> a));

        return new ScreeningReport(findings, severity, hasCrisis, fieldLabels);
    }

    /**
     * The complete report data passed to the Thymeleaf template.
     */
    public record ScreeningReport(
        List<Finding> findings,
        String overallSeverity,
        boolean hasCrisis,
        Map<String, String> fieldLabels
    ) {}
}