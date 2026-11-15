package com.ooad.mhs.questionnaire;

import com.ooad.mhs.service.DerivedFieldCalculator;
import com.ooad.mhs.util.Section;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Before/after test for the Questionnaire refactor (M1).
 *
 * Runs the ORIGINAL scoring code ({@link LegacyDerivedFieldCalculator}, a frozen copy) and the
 * CURRENT {@link DerivedFieldCalculator} on the same inputs and requires exactly the same
 * working facts: same field names, same values, nothing extra, nothing missing.
 * Any difference would change which rules fire, so this guards the rule engine.
 */
class QuestionnaireRefactorEquivalenceTest {

    /** Number of random answer sets. Fixed seed, so every run uses the same inputs. */
    private static final int RANDOM_CASES = 10_000;
    private static final long SEED = 42L;

    /** Every real question key with its valid range, after AssessmentService conversion. */
    private static final Map<Section, Map<String, int[]>> FIELDS = new EnumMap<>(Section.class);

    static {
        add(Section.PHQ9, range("phq9_q", 1, 9), 0, 3);
        add(Section.GAD7, range("gad7_q", 1, 7), 0, 3);
        add(Section.PHQ15, letters("phq15_", 'a', 'o'), 0, 2);
        add(Section.CSSRS, range("cssrs_q", 1, 6), 0, 1);     // graded answers are converted to 0/1
        add(Section.WHO5, range("who5_q", 1, 5), 0, 5);
        add(Section.PSS10, range("pss10_q", 1, 10), 0, 4);
        add(Section.PSQI, List.of("psqi_q1", "psqi_q3"), 0, 1439); // clock time in minutes
        add(Section.PSQI, List.of("psqi_q2"), 0, 180);             // minutes to fall asleep
        add(Section.PSQI, List.of("psqi_q4"), 0, 24);              // hours slept (rounded)
        add(Section.PSQI, letters("psqi_q5", 'a', 'i'), 0, 3);
        add(Section.PSQI, List.of("psqi_q6", "psqi_q7", "psqi_q8", "psqi_q9"), 0, 3);
        add(Section.WSAS, range("wsas_q", 1, 5), 0, 8);
        add(Section.ASRS, range("asrs_q", 1, 6), 0, 4);
        add(Section.PCPTSD, range("pcptsd_q", 1, 4), 0, 1);
    }

    private final LegacyDerivedFieldCalculator legacy = new LegacyDerivedFieldCalculator();
    private final DerivedFieldCalculator current = newCalculator();

    /** Builds the calculator under test (the only line that depends on its constructor). */
    private static DerivedFieldCalculator newCalculator() {
        return new DerivedFieldCalculator(QuestionnaireRegistry.createDefault());
    }

    @Test
    void allSectionsAllZero() {
        assertSame("all zero", answers(false), EnumSet.allOf(Section.class));
    }

    @Test
    void allSectionsAllMaximum() {
        assertSame("all maximum", answers(true), EnumSet.allOf(Section.class));
    }

    @Test
    void noAnswersButSectionsMarkedComplete() {
        // exercises every default value used by the scoring (e.g. WHO-5 default 5, PSQI 23:00/07:00)
        assertSame("empty answers", new HashMap<>(), EnumSet.allOf(Section.class));
    }

    @Test
    void noSectionsComplete() {
        assertSame("nothing complete", answers(true), EnumSet.noneOf(Section.class));
    }

    @Test
    void eachSectionOnItsOwn() {
        for (Section s : Section.values()) {
            assertSame("only " + s, answers(true), EnumSet.of(s));
            assertSame("only " + s + " (zeros)", answers(false), EnumSet.of(s));
        }
    }

    @Test
    void sameBedAndWakeTime() {
        Map<String, Integer> a = answers(false);
        a.put("psqi_q1", 600);
        a.put("psqi_q3", 600);
        assertSame("bed == wake", a, EnumSet.of(Section.PSQI));
    }

    @Test
    void randomAnswerSets() {
        Random rnd = new Random(SEED);
        for (int i = 0; i < RANDOM_CASES; i++) {
            Set<Section> completed = EnumSet.noneOf(Section.class);
            Map<String, Integer> a = new HashMap<>();
            for (Section s : Section.values()) {
                boolean complete = rnd.nextDouble() < 0.7;
                if (complete) completed.add(s);
                for (Map.Entry<String, int[]> f : FIELDS.get(s).entrySet()) {
                    // answers of incomplete sections are sometimes present too (like the PHQ-9 item 9 safety item)
                    if (!complete && rnd.nextDouble() > 0.3) continue;
                    if (rnd.nextDouble() < 0.05) continue;                  // a missing answer → default used
                    int lo = f.getValue()[0], hi = f.getValue()[1];
                    int v = rnd.nextDouble() < 0.02
                        ? rnd.nextInt(41) - 10                              // occasional out-of-range value
                        : lo + rnd.nextInt(hi - lo + 1);
                    a.put(f.getKey(), v);
                }
            }
            assertSame("random case " + i, a, completed);
        }
    }

    /**
     * The score line and note on the report ("Total score 14 / 27", "Higher is better...")
     * must be the same as the old ReportService.scoreText / scoreNote for every section.
     */
    @Test
    void reportScoreTextAndNotesUnchanged() {
        QuestionnaireRegistry registry = QuestionnaireRegistry.createDefault();
        Random rnd = new Random(SEED);
        for (int i = 0; i < 2_000; i++) {
            Map<String, Integer> a = new HashMap<>();
            Map<String, String> raw = new HashMap<>();   // answers as submitted by the form
            for (Section s : Section.values()) {
                for (Map.Entry<String, int[]> f : FIELDS.get(s).entrySet()) {
                    int lo = f.getValue()[0], hi = f.getValue()[1];
                    int v = lo + rnd.nextInt(hi - lo + 1);
                    // Safety Check is submitted graded (0-3) and converted to 0/1
                    String submitted = s == Section.CSSRS ? String.valueOf(v == 0 ? 0 : 1 + rnd.nextInt(3)) : String.valueOf(v);
                    a.put(f.getKey(), v);
                    raw.put(f.getKey(), submitted);
                }
            }
            Map<String, Integer> facts = legacy.calculate(a, EnumSet.allOf(Section.class));
            for (Section s : Section.values()) {
                Questionnaire q = registry.get(s);
                assertEquals(LegacyReportText.scoreText(s, facts, raw), q.summary(facts, raw), "summary of " + s + " in case " + i);
                assertEquals(LegacyReportText.scoreNote(s), q.scoreNote(), "note of " + s);
            }
        }
    }

    @Test
    void registryHasExactlyOneQuestionnairePerSection() {
        QuestionnaireRegistry registry = QuestionnaireRegistry.createDefault();
        assertEquals(Section.values().length, registry.all().size());
        for (Section s : Section.values()) assertEquals(s, registry.get(s).getSection());
    }

    // ------------------------------------------------------------------ helpers

    private void assertSame(String label, Map<String, Integer> answers, Set<Section> completed) {
        Map<String, Integer> expected = new TreeMap<>(legacy.calculate(new HashMap<>(answers), completed));
        Map<String, Integer> actual = new TreeMap<>(current.calculate(new HashMap<>(answers), completed));
        assertEquals(expected, actual, () -> label + " differs. completed=" + completed + " answers=" + new TreeMap<>(answers));
    }

    /** All answers of all sections at their minimum (or maximum). */
    private static Map<String, Integer> answers(boolean max) {
        Map<String, Integer> a = new HashMap<>();
        FIELDS.values().forEach(m -> m.forEach((k, r) -> a.put(k, max ? r[1] : r[0])));
        return a;
    }

    private static void add(Section s, List<String> keys, int lo, int hi) {
        Map<String, int[]> m = FIELDS.computeIfAbsent(s, k -> new LinkedHashMap<>());
        for (String k : keys) m.put(k, new int[]{lo, hi});
    }

    private static List<String> range(String prefix, int from, int to) {
        List<String> keys = new ArrayList<>();
        for (int i = from; i <= to; i++) keys.add(prefix + i);
        return keys;
    }

    private static List<String> letters(String prefix, char from, char to) {
        List<String> keys = new ArrayList<>();
        for (char c = from; c <= to; c++) keys.add(prefix + c);
        return keys;
    }
}
