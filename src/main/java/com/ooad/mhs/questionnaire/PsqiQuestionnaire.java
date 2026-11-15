package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PSQI: sleep quality. Seven component scores (0-3 each) and a global score (0-21).
 * Lower is better; a global score above 5 means poor sleep.
 * Derived fields: psqi_c1 ... psqi_c7, psqi_global, sleep_poor_any.
 */
@Component
public class PsqiQuestionnaire extends Questionnaire {

    public PsqiQuestionnaire() {
        super(Section.PSQI);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        // Q1=bedtime (minutes), Q2=minutes to fall asleep, Q3=wake time (minutes), Q4=hours slept
        int bedMin = f.getOrDefault("psqi_q1", 1380);   // default 23:00
        int wakeMin = f.getOrDefault("psqi_q3", 420);    // default 07:00
        int sleepLatency = f.getOrDefault("psqi_q2", 15);
        int hoursSlept = f.getOrDefault("psqi_q4", 7);

        // Component 1: Subjective sleep quality (Q6, direct)
        f.put("psqi_c1", value(f, "psqi_q6"));

        // Component 2: Sleep latency (Q2 score + Q5a)
        int q2Score = sleepLatency <= 15 ? 0 : sleepLatency <= 30 ? 1 : sleepLatency <= 60 ? 2 : 3;
        int c2Sum = q2Score + value(f, "psqi_q5a");
        f.put("psqi_c2", c2Sum == 0 ? 0 : c2Sum <= 2 ? 1 : c2Sum <= 4 ? 2 : 3);

        // Component 3: Sleep duration (Q4 hours)
        f.put("psqi_c3", hoursSlept > 7 ? 0 : hoursSlept >= 6 ? 1 : hoursSlept >= 5 ? 2 : 3);

        // Component 4: Sleep efficiency (hours asleep / hours in bed)
        int bedDuration = wakeMin > bedMin ? (wakeMin - bedMin) : (1440 - bedMin + wakeMin);
        double eff = bedDuration > 0 ? (hoursSlept * 60.0 / bedDuration) * 100 : 0;
        f.put("psqi_c4", eff >= 85 ? 0 : eff >= 75 ? 1 : eff >= 65 ? 2 : 3);

        // Component 5: Sleep disturbances (sum of Q5b-Q5i)
        int distSum = 0;
        for (char c : new char[]{'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'}) distSum += value(f, "psqi_q5" + c);
        f.put("psqi_c5", distSum == 0 ? 0 : distSum <= 9 ? 1 : distSum <= 18 ? 2 : 3);

        // Component 6: Sleeping medication (Q7, direct)
        f.put("psqi_c6", value(f, "psqi_q7"));

        // Component 7: Daytime dysfunction (Q8 + Q9)
        int daySum = value(f, "psqi_q8") + value(f, "psqi_q9");
        f.put("psqi_c7", daySum == 0 ? 0 : daySum <= 2 ? 1 : daySum <= 4 ? 2 : 3);

        // Global score
        int global = f.get("psqi_c1") + f.get("psqi_c2") + f.get("psqi_c3")
            + f.get("psqi_c4") + f.get("psqi_c5") + f.get("psqi_c6") + f.get("psqi_c7");
        f.put("psqi_global", global);
    }

    @Override
    protected void computeFlags(Map<String, Integer> f) {
        f.put("sleep_poor_any", flag(value(f, "psqi_global") > 5));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Global score " + f.get("psqi_global") + " / 21";
    }

    @Override
    public String scoreNote() {
        return "Lower is better. Above 5 suggests poor sleep.";
    }
}
