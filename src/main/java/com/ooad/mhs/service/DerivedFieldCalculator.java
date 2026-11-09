package com.ooad.mhs.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes all 34 derived fields from raw user responses BEFORE rule evaluation.
 * 
 * This is the pre-processor for the rule engine. It takes the raw answers
 * (Map<String, Integer>) and computes instrument totals, subscale scores,
 * PSQI components, and boolean flags (crisis_detected, depression_any, etc.).
 * 
 * The output map (working_facts) contains BOTH the raw responses AND the
 * derived fields — the engine reads from this single map.
 * 
 * Called by: ScreeningService, before RuleEngine.evaluate()
 */
@Service
public class DerivedFieldCalculator {

    public Map<String, Integer> calculate(Map<String, Integer> responses) {
        Map<String, Integer> facts = new HashMap<>(responses);

        calculatePhq9(facts);
        calculateGad7(facts);
        calculatePhq15(facts);
        calculateCssrs(facts);
        calculateWho5(facts);
        calculatePss10(facts);
        calculatePsqi(facts);
        calculateWsas(facts);
        calculateAsrs(facts);
        calculatePcptsd(facts);
        calculateDerivedFlags(facts);

        return facts;
    }

    private void calculatePhq9(Map<String, Integer> f) {
        int total = 0;
        for (int i = 1; i <= 9; i++)
            total += f.getOrDefault("phq9_q" + i, 0);
        f.put("phq9_total", total);

        f.put("phq9_core_symptom",
            (f.getOrDefault("phq9_q1", 0) >= 2 || f.getOrDefault("phq9_q2", 0) >= 2) ? 1 : 0);

        int count = 0;
        for (int i = 1; i <= 9; i++)
            if (f.getOrDefault("phq9_q" + i, 0) >= 2) count++;
        f.put("phq9_symptom_count", count);
    }

    private void calculateGad7(Map<String, Integer> f) {
        int total = 0;
        for (int i = 1; i <= 7; i++)
            total += f.getOrDefault("gad7_q" + i, 0);
        f.put("gad7_total", total);

        int count = 0;
        for (int i = 2; i <= 7; i++)
            if (f.getOrDefault("gad7_q" + i, 0) >= 2) count++;
        f.put("gad7_symptom_count", count);
    }

    private void calculatePhq15(Map<String, Integer> f) {
        String[] keys = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o"};
        int total = 0, severe = 0;
        for (String k : keys) {
            int v = f.getOrDefault("phq15_" + k, 0);
            total += v;
            if (v == 2) severe++;
        }
        f.put("phq15_total", total);
        f.put("phq15_severe_count", severe);
    }

    private void calculateCssrs(Map<String, Integer> f) {
        int q1 = f.getOrDefault("cssrs_q1", 0);
        int q2 = f.getOrDefault("cssrs_q2", 0);
        int q3 = f.getOrDefault("cssrs_q3", 0);
        int q4 = f.getOrDefault("cssrs_q4", 0);
        int q5 = f.getOrDefault("cssrs_q5", 0);
        int q6 = f.getOrDefault("cssrs_q6", 0);

        f.put("cssrs_passive_ideation", (q1 == 1 || q2 == 1) ? 1 : 0);
        f.put("cssrs_any_positive", (q1==1||q2==1||q3==1||q4==1||q5==1||q6==1) ? 1 : 0);
        f.put("crisis_detected", (q4==1||q5==1||q6==1) ? 1 : 0);
    }

    private void calculateWho5(Map<String, Integer> f) {
        int total = 0;
        for (int i = 1; i <= 5; i++)
            total += f.getOrDefault("who5_q" + i, 5);
        f.put("who5_raw", total);
    }

    private void calculatePss10(Map<String, Integer> f) {
        // Helplessness: q1,q2,q3,q6,q9,q10 (sum directly)
        int helpless = f.getOrDefault("pss10_q1",0) + f.getOrDefault("pss10_q2",0)
            + f.getOrDefault("pss10_q3",0) + f.getOrDefault("pss10_q6",0)
            + f.getOrDefault("pss10_q9",0) + f.getOrDefault("pss10_q10",0);
        f.put("pss10_helplessness", helpless);

        // Efficacy: q4,q5,q7,q8 (reverse-scored: 4-value)
        int efficacy = (4-f.getOrDefault("pss10_q4",0)) + (4-f.getOrDefault("pss10_q5",0))
            + (4-f.getOrDefault("pss10_q7",0)) + (4-f.getOrDefault("pss10_q8",0));
        f.put("pss10_efficacy", efficacy);

        f.put("pss10_total", helpless + efficacy);
    }

    private void calculatePsqi(Map<String, Integer> f) {
        // Q1=bedtime (minutes), Q2=minutes to fall asleep, Q3=wake time (minutes), Q4=hours slept
        int bedMin = f.getOrDefault("psqi_q1", 1380);   // default 23:00
        int wakeMin = f.getOrDefault("psqi_q3", 420);    // default 07:00
        int sleepLatency = f.getOrDefault("psqi_q2", 15);
        int hoursSlept = f.getOrDefault("psqi_q4", 7);

        // Component 1: Subjective sleep quality (Q6, direct)
        f.put("psqi_c1", f.getOrDefault("psqi_q6", 0));

        // Component 2: Sleep latency (Q2 score + Q5a)
        int q2Score = sleepLatency <= 15 ? 0 : sleepLatency <= 30 ? 1 : sleepLatency <= 60 ? 2 : 3;
        int q5a = f.getOrDefault("psqi_q5a", 0);
        int c2Sum = q2Score + q5a;
        f.put("psqi_c2", c2Sum == 0 ? 0 : c2Sum <= 2 ? 1 : c2Sum <= 4 ? 2 : 3);

        // Component 3: Sleep duration (Q4 hours)
        f.put("psqi_c3", hoursSlept > 7 ? 0 : hoursSlept >= 6 ? 1 : hoursSlept >= 5 ? 2 : 3);

        // Component 4: Sleep efficiency
        int bedDuration = wakeMin > bedMin ? (wakeMin - bedMin) : (1440 - bedMin + wakeMin);
        double eff = bedDuration > 0 ? (hoursSlept * 60.0 / bedDuration) * 100 : 0;
        f.put("psqi_c4", eff >= 85 ? 0 : eff >= 75 ? 1 : eff >= 65 ? 2 : 3);

        // Component 5: Sleep disturbances (sum of Q5b-Q5i)
        int distSum = 0;
        for (char c : new char[]{'b','c','d','e','f','g','h','i'})
            distSum += f.getOrDefault("psqi_q5" + c, 0);
        f.put("psqi_c5", distSum == 0 ? 0 : distSum <= 9 ? 1 : distSum <= 18 ? 2 : 3);

        // Component 6: Sleeping medication (Q7, direct)
        f.put("psqi_c6", f.getOrDefault("psqi_q7", 0));

        // Component 7: Daytime dysfunction (Q8 + Q9)
        int daySum = f.getOrDefault("psqi_q8", 0) + f.getOrDefault("psqi_q9", 0);
        f.put("psqi_c7", daySum == 0 ? 0 : daySum <= 2 ? 1 : daySum <= 4 ? 2 : 3);

        // Global score
        int global = f.get("psqi_c1") + f.get("psqi_c2") + f.get("psqi_c3")
            + f.get("psqi_c4") + f.get("psqi_c5") + f.get("psqi_c6") + f.get("psqi_c7");
        f.put("psqi_global", global);
    }

    private void calculateWsas(Map<String, Integer> f) {
        int total = 0;
        for (int i = 1; i <= 5; i++)
            total += f.getOrDefault("wsas_q" + i, 0);
        f.put("wsas_total", total);
    }

    private void calculateAsrs(Map<String, Integer> f) {
        // Inattentive: Q1-Q3, shaded if >= 3
        int inatt = 0;
        for (int i = 1; i <= 3; i++)
            if (f.getOrDefault("asrs_q" + i, 0) >= 3) inatt++;
        f.put("asrs_inattentive_count", inatt);

        // Hyperactive: Q4-Q6, shaded if >= 2
        int hyper = 0;
        for (int i = 4; i <= 6; i++)
            if (f.getOrDefault("asrs_q" + i, 0) >= 2) hyper++;
        f.put("asrs_hyperactive_count", hyper);

        f.put("asrs_shaded_count", inatt + hyper);
    }

    private void calculatePcptsd(Map<String, Integer> f) {
        int total = 0;
        for (int i = 1; i <= 4; i++)
            total += f.getOrDefault("pcptsd_q" + i, 0);
        f.put("pcptsd_total", total);
    }

    private void calculateDerivedFlags(Map<String, Integer> f) {
        // Panic: heart racing OR shortness of breath
        f.put("panic_physical_symptom",
            (f.getOrDefault("phq15_i", 0) >= 1 || f.getOrDefault("phq15_j", 0) >= 1) ? 1 : 0);

        // Broad category flags for Level 2 composites
        f.put("depression_any", f.getOrDefault("phq9_total", 0) >= 5 ? 1 : 0);
        f.put("depression_moderate_plus", f.getOrDefault("phq9_total", 0) >= 10 ? 1 : 0);
        f.put("anxiety_any", f.getOrDefault("gad7_total", 0) >= 5 ? 1 : 0);
        f.put("anxiety_moderate_plus", f.getOrDefault("gad7_total", 0) >= 10 ? 1 : 0);
        f.put("sleep_poor_any", f.getOrDefault("psqi_global", 0) > 5 ? 1 : 0);
    }
}