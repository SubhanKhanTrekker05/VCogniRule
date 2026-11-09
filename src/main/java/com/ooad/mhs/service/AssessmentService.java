package com.ooad.mhs.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts raw form responses to a working_facts map with derived fields.
 * 
 * Flow: HTML form (Map<String, String>) → convertResponses → DerivedFieldCalculator → working_facts
 * 
 * The form sends all values as strings. This service converts them to integers:
 * - Radio buttons: direct parseInt (e.g., "2" → 2)
 * - PSQI time inputs: "23:00" → 1380 (minutes from midnight)
 * - PSQI hours: "7.5" → 8 (rounded to integer)
 * - Missing values: defaults to 0 (or 5 for WHO-5, handled by DerivedFieldCalculator)
 */
@Service
public class AssessmentService {

    private final DerivedFieldCalculator derivedFieldCalculator;

    public AssessmentService(DerivedFieldCalculator derivedFieldCalculator) {
        this.derivedFieldCalculator = derivedFieldCalculator;
    }

    /**
     * Takes raw form data and returns working_facts ready for the RuleEngine.
     */
    public Map<String, Integer> prepareWorkingFacts(Map<String, String> formData) {
        Map<String, Integer> responses = convertResponses(formData);
        return derivedFieldCalculator.calculate(responses);
    }

    private Map<String, Integer> convertResponses(Map<String, String> formData) {
        Map<String, Integer> responses = new HashMap<>();

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Skip non-question params (submit button, CSRF tokens, etc.)
            if (key.equals("submit") || key.startsWith("_") || value == null || value.isEmpty()) {
                continue;
            }

            // PSQI time inputs: "23:00" → 1380 minutes
            if (key.equals("psqi_q1") || key.equals("psqi_q3")) {
                responses.put(key, parseTimeToMinutes(value));
            }
            // PSQI hours: "7.5" → 8 (rounded)
            else if (key.equals("psqi_q4")) {
                try {
                    responses.put(key, (int) Math.round(Double.parseDouble(value)));
                } catch (NumberFormatException e) {
                    responses.put(key, 7);
                }
            }
            // Regular integer (radio buttons, number inputs)
            else {
                try {
                    responses.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    responses.put(key, 0);
                }
            }
        }

        return responses;
    }

    private int parseTimeToMinutes(String time) {
        if (time == null || time.isEmpty()) return 1380; // default 23:00
        String[] parts = time.split(":");
        if (parts.length != 2) return 1380;
        try {
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return 1380;
        }
    }
}