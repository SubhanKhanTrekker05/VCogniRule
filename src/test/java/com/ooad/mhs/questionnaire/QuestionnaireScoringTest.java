package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the scoring of each of the 10 questionnaires, with hand-calculated examples
 * (including the cut-off boundaries the rules depend on).
 */
class QuestionnaireScoringTest {

    /** Answers prefix1..prefixN with the given values. */
    private static Map<String, Integer> items(String prefix, int... values) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < values.length; i++) m.put(prefix + (i + 1), values[i]);
        return m;
    }

    private static Map<String, Integer> score(Questionnaire q, Map<String, Integer> answers) {
        Map<String, Integer> facts = new HashMap<>(answers);
        q.score(facts);
        return facts;
    }

    @Nested
    @DisplayName("PHQ-9 (depression)")
    class Phq9 {
        private final Questionnaire q = new Phq9Questionnaire();

        @Test
        void totalCoreSymptomAndCount() {
            Map<String, Integer> f = score(q, items("phq9_q", 2, 1, 0, 0, 0, 0, 0, 0, 1));
            assertThat(f).containsEntry("phq9_total", 4).containsEntry("phq9_core_symptom", 1)
                .containsEntry("phq9_symptom_count", 1).containsEntry("depression_any", 0)
                .containsEntry("depression_moderate_plus", 0);
            assertThat(q.summary(f, Map.of())).isEqualTo("Total score 4 / 27");
        }

        @Test
        @DisplayName("Cut-offs: 5 = at least mild, 10 = moderate or higher")
        void cutOffs() {
            assertThat(score(q, items("phq9_q", 1, 1, 1, 1, 1, 0, 0, 0, 0))).containsEntry("depression_any", 1)
                .containsEntry("depression_moderate_plus", 0);
            assertThat(score(q, items("phq9_q", 2, 2, 2, 2, 2, 0, 0, 0, 0))).containsEntry("phq9_total", 10)
                .containsEntry("depression_moderate_plus", 1);
        }

        @Test
        void maximumScore() {
            assertThat(score(q, items("phq9_q", 3, 3, 3, 3, 3, 3, 3, 3, 3)))
                .containsEntry("phq9_total", 27).containsEntry("phq9_symptom_count", 9);
        }
    }

    @Nested
    @DisplayName("GAD-7 (anxiety)")
    class Gad7 {
        private final Questionnaire q = new Gad7Questionnaire();

        @Test
        @DisplayName("Symptom count uses items 2-7 only")
        void totalAndCount() {
            Map<String, Integer> f = score(q, items("gad7_q", 2, 2, 0, 0, 0, 0, 0));
            assertThat(f).containsEntry("gad7_total", 4).containsEntry("gad7_symptom_count", 1)
                .containsEntry("anxiety_any", 0);
            assertThat(q.summary(f, Map.of())).isEqualTo("Total score 4 / 21");
        }

        @Test
        void moderateCutOff() {
            assertThat(score(q, items("gad7_q", 2, 2, 2, 2, 2, 0, 0)))
                .containsEntry("gad7_total", 10).containsEntry("anxiety_any", 1).containsEntry("anxiety_moderate_plus", 1);
        }
    }

    @Nested
    @DisplayName("PHQ-15 (physical symptoms)")
    class Phq15 {
        private final Questionnaire q = new Phq15Questionnaire();

        @Test
        void totalSevereCountAndPanicFlag() {
            Map<String, Integer> a = new HashMap<>(Map.of("phq15_a", 2, "phq15_b", 2, "phq15_i", 1));
            Map<String, Integer> f = score(q, a);
            assertThat(f).containsEntry("phq15_total", 5).containsEntry("phq15_severe_count", 2)
                .containsEntry("panic_physical_symptom", 1);
            assertThat(q.summary(f, Map.of())).isEqualTo("Total score 5 / 30");
        }

        @Test
        @DisplayName("No racing heart (i) or breathlessness (j) → no panic flag")
        void noPanic() {
            assertThat(score(q, new HashMap<>(Map.of("phq15_a", 2)))).containsEntry("panic_physical_symptom", 0);
        }
    }

    @Nested
    @DisplayName("C-SSRS (Safety Check)")
    class Cssrs {
        private final Questionnaire q = new CssrsQuestionnaire();

        @Test
        void allNo() {
            assertThat(score(q, items("cssrs_q", 0, 0, 0, 0, 0, 0))).containsEntry("cssrs_passive_ideation", 0)
                .containsEntry("cssrs_any_positive", 0).containsEntry("crisis_detected", 0);
        }

        @Test
        @DisplayName("Item 1 (wish to be dead) = passive ideation, not yet crisis")
        void passive() {
            assertThat(score(q, items("cssrs_q", 1, 0, 0, 0, 0, 0))).containsEntry("cssrs_passive_ideation", 1)
                .containsEntry("cssrs_any_positive", 1).containsEntry("crisis_detected", 0);
        }

        @Test
        @DisplayName("Item 4, 5 or 6 (intent, plan, behaviour) = crisis")
        void crisis() {
            assertThat(score(q, items("cssrs_q", 0, 0, 0, 1, 0, 0))).containsEntry("crisis_detected", 1);
            assertThat(score(q, items("cssrs_q", 0, 0, 0, 0, 1, 0))).containsEntry("crisis_detected", 1);
            assertThat(score(q, items("cssrs_q", 0, 0, 0, 0, 0, 1))).containsEntry("crisis_detected", 1);
        }

        @Test
        @DisplayName("The summary counts graded Yes answers as submitted (1, 2 and 3 are all Yes)")
        void summaryCountsGradedYes() {
            Map<String, String> raw = Map.of("cssrs_q1", "3", "cssrs_q2", "1", "cssrs_q3", "0",
                "cssrs_q4", "0", "cssrs_q5", "0", "cssrs_q6", "0");
            assertThat(q.summary(Map.of(), raw)).isEqualTo("2 of 6 answered Yes");
            assertThat(q.scoreNote()).isEqualTo("Any Yes answer is followed up in the findings.");
        }
    }

    @Nested
    @DisplayName("WHO-5 (wellbeing, higher is better)")
    class Who5 {
        private final Questionnaire q = new Who5Questionnaire();

        @Test
        void rawScoreAndPercentage() {
            Map<String, Integer> f = score(q, items("who5_q", 5, 4, 3, 2, 1));
            assertThat(f).containsEntry("who5_raw", 15);
            assertThat(q.summary(f, Map.of())).isEqualTo("Wellbeing score 15 / 25 (60%)");
            assertThat(q.scoreNote()).startsWith("Higher is better");
        }

        @Test
        @DisplayName("A missing item counts as 5, as in the original scoring")
        void missingItemCountsAsFive() {
            assertThat(score(q, items("who5_q", 1, 1, 1, 1))).containsEntry("who5_raw", 9);
        }
    }

    @Nested
    @DisplayName("PSS-10 (stress, items 4, 5, 7, 8 reverse-scored)")
    class Pss10 {
        private final Questionnaire q = new Pss10Questionnaire();

        @Test
        @DisplayName("All 'Never': positive items reversed → efficacy 16, total 16")
        void allNever() {
            assertThat(score(q, items("pss10_q", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))
                .containsEntry("pss10_helplessness", 0).containsEntry("pss10_efficacy", 16).containsEntry("pss10_total", 16);
        }

        @Test
        void allVeryOften() {
            assertThat(score(q, items("pss10_q", 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)))
                .containsEntry("pss10_helplessness", 24).containsEntry("pss10_efficacy", 0).containsEntry("pss10_total", 24);
        }

        @Test
        @DisplayName("Lowest stress: negative items 'Never', positive items 'Very often' → 0")
        void lowestStress() {
            Map<String, Integer> f = score(q, items("pss10_q", 0, 0, 0, 4, 4, 0, 4, 4, 0, 0));
            assertThat(f).containsEntry("pss10_total", 0);
            assertThat(q.summary(f, Map.of())).isEqualTo("Total score 0 / 40");
        }
    }

    @Nested
    @DisplayName("PSQI (sleep, global 0-21, above 5 = poor)")
    class Psqi {
        private final Questionnaire q = new PsqiQuestionnaire();

        private Map<String, Integer> psqi(int bed, int latency, int wake, int hours, int q5a, int disturbEach,
                                          int quality, int meds, int q8, int q9) {
            Map<String, Integer> a = new HashMap<>();
            a.put("psqi_q1", bed); a.put("psqi_q2", latency); a.put("psqi_q3", wake); a.put("psqi_q4", hours);
            a.put("psqi_q5a", q5a);
            for (char c = 'b'; c <= 'i'; c++) a.put("psqi_q5" + c, disturbEach);
            a.put("psqi_q6", quality); a.put("psqi_q7", meds); a.put("psqi_q8", q8); a.put("psqi_q9", q9);
            return a;
        }

        @Test
        @DisplayName("Worked example: global 5 (boundary, not yet poor sleep)")
        void globalFive() {
            // 23:00 to 07:00 = 480 min in bed, 7 h asleep = 87.5% efficiency
            Map<String, Integer> f = score(q, psqi(23 * 60, 20, 7 * 60, 7, 1, 1, 1, 0, 1, 1));
            assertThat(f).containsEntry("psqi_c1", 1).containsEntry("psqi_c2", 1).containsEntry("psqi_c3", 1)
                .containsEntry("psqi_c4", 0).containsEntry("psqi_c5", 1).containsEntry("psqi_c6", 0)
                .containsEntry("psqi_c7", 1).containsEntry("psqi_global", 5).containsEntry("sleep_poor_any", 0);
            assertThat(q.summary(f, Map.of())).isEqualTo("Global score 5 / 21");
        }

        @Test
        @DisplayName("Worked example: global 6 (poor sleep)")
        void globalSix() {
            // 23:00 to 04:00 = 300 min in bed, 4 h asleep = 80%; 90 min to fall asleep
            Map<String, Integer> f = score(q, psqi(23 * 60, 90, 4 * 60, 4, 0, 0, 0, 0, 0, 0));
            assertThat(f).containsEntry("psqi_c2", 2).containsEntry("psqi_c3", 3).containsEntry("psqi_c4", 1)
                .containsEntry("psqi_global", 6).containsEntry("sleep_poor_any", 1);
        }
    }

    @Nested
    @DisplayName("WSAS, ASRS and PC-PTSD")
    class Others {
        @Test
        void wsasTotal() {
            Questionnaire q = new WsasQuestionnaire();
            Map<String, Integer> f = score(q, items("wsas_q", 1, 2, 3, 4, 5));
            assertThat(f).containsEntry("wsas_total", 15);
            assertThat(q.summary(f, Map.of())).isEqualTo("Total score 15 / 40");
        }

        @Test
        @DisplayName("ASRS: inattention items count from 3, hyperactivity items from 2")
        void asrsShadedCounts() {
            Questionnaire q = new AsrsQuestionnaire();
            Map<String, Integer> f = score(q, items("asrs_q", 3, 2, 4, 2, 1, 4));
            assertThat(f).containsEntry("asrs_inattentive_count", 2).containsEntry("asrs_hyperactive_count", 2)
                .containsEntry("asrs_shaded_count", 4);
            assertThat(q.summary(f, Map.of())).isEqualTo("4 of 6 answers in the screening range");
        }

        @Test
        void pcPtsdTotal() {
            Questionnaire q = new PcPtsdQuestionnaire();
            Map<String, Integer> f = score(q, items("pcptsd_q", 1, 0, 1, 1));
            assertThat(f).containsEntry("pcptsd_total", 3);
            Map<String, String> raw = Map.of("pcptsd_q1", "1", "pcptsd_q2", "0", "pcptsd_q3", "1", "pcptsd_q4", "1");
            assertThat(q.summary(f, raw)).isEqualTo("3 of 4 answered Yes");
        }
    }

    // ------------------------------------------------------------------ inheritance / registry

    @Test
    @DisplayName("score() adds both the totals and the flags")
    void scoreAddsTotalsAndFlags() {
        Map<String, Integer> f = score(new Phq9Questionnaire(), items("phq9_q", 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(f).containsKeys("phq9_total", "depression_any");
    }

    @Test
    @DisplayName("Polymorphism: every subclass is a Questionnaire for its own section")
    void eachSubclassHasItsSection() {
        QuestionnaireRegistry registry = QuestionnaireRegistry.createDefault();
        assertThat(registry.all()).hasSize(10);
        for (Section s : Section.values()) {
            Questionnaire q = registry.get(s);
            assertThat(q).isInstanceOf(Questionnaire.class);
            assertThat(q.getSection()).isEqualTo(s);
            assertThat(q.scoreNote()).isNotBlank();
        }
    }

    @Test
    @DisplayName("The registry refuses a missing questionnaire")
    void registryRejectsMissingSection() {
        List<Questionnaire> nine = new ArrayList<>(QuestionnaireRegistry.createDefault().all());
        nine.removeIf(q -> q.getSection() == Section.WSAS);
        assertThatThrownBy(() -> new QuestionnaireRegistry(nine)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("WSAS");
    }

    @Test
    @DisplayName("The registry refuses two questionnaires for one section")
    void registryRejectsDuplicate() {
        List<Questionnaire> eleven = new ArrayList<>(QuestionnaireRegistry.createDefault().all());
        eleven.add(new Phq9Questionnaire());
        assertThatThrownBy(() -> new QuestionnaireRegistry(eleven)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PHQ9");
    }
}
