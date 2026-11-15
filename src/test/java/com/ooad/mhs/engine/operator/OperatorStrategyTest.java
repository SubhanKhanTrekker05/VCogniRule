package com.ooad.mhs.engine.operator;

import com.ooad.mhs.domain.Condition;
import com.ooad.mhs.util.Operator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Unit tests for the 8 operator strategies (Strategy pattern) and their factory. */
class OperatorStrategyTest {

    private static Condition single(Operator op, Integer value) {
        return new Condition(1L, 1L, "phq9_total", op, value, null, null, null);
    }

    private static Condition between(Integer min, Integer max) {
        return new Condition(1L, 1L, "phq9_total", Operator.BETWEEN, null, null, min, max);
    }

    private static Condition in(String values) {
        return new Condition(1L, 1L, "phq9_total", Operator.IN, null, values, null, null);
    }

    private static boolean test(Condition c, int actual) {
        return OperatorStrategyFactory.forOperator(c.operator()).test(actual, c);
    }

    // ------------------------------------------------------------------ six single-value operators

    @DisplayName("EQ, NE, GT, GTE, LT, LTE compare the answer with one number")
    @ParameterizedTest(name = "{1} {0} {2} -> {3}")
    @CsvSource({
        "EQ,  5, 5, true",   "EQ,  4, 5, false",
        "NE,  4, 5, true",   "NE,  5, 5, false",
        "GT,  6, 5, true",   "GT,  5, 5, false",
        "GTE, 5, 5, true",   "GTE, 4, 5, false",
        "LT,  4, 5, true",   "LT,  5, 5, false",
        "LTE, 5, 5, true",   "LTE, 6, 5, false",
    })
    void singleValueOperators(Operator op, int actual, int required, boolean expected) {
        assertThat(test(single(op, required), actual)).isEqualTo(expected);
    }

    @DisplayName("A single-value operator with no value never matches")
    @ParameterizedTest
    @EnumSource(value = Operator.class, names = {"EQ", "NE", "GT", "GTE", "LT", "LTE"})
    void singleValueWithoutValueIsFalse(Operator op) {
        assertThat(test(single(op, null), 5)).isFalse();
    }

    @DisplayName("describe() gives the text shown under 'Rule needs'")
    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({"EQ, == 10", "NE, != 10", "GT, > 10", "GTE, >= 10", "LT, < 10", "LTE, <= 10"})
    void singleValueDescribe(Operator op, String expected) {
        assertThat(OperatorStrategyFactory.forOperator(op).describe(single(op, 10))).isEqualTo(expected);
    }

    @Test
    @DisplayName("The six single-value strategies share the abstract base class SingleValueStrategy")
    void singleValueStrategiesExtendBaseClass() {
        for (Operator op : new Operator[]{Operator.EQ, Operator.NE, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE}) {
            assertThat(OperatorStrategyFactory.forOperator(op)).isInstanceOf(SingleValueStrategy.class);
        }
        assertThat(OperatorStrategyFactory.forOperator(Operator.BETWEEN)).isNotInstanceOf(SingleValueStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.IN)).isNotInstanceOf(SingleValueStrategy.class);
    }

    // ------------------------------------------------------------------ BETWEEN

    @DisplayName("BETWEEN includes both ends")
    @ParameterizedTest(name = "{0} in 10-14 -> {1}")
    @CsvSource({"9, false", "10, true", "12, true", "14, true", "15, false"})
    void betweenIncludesBothEnds(int actual, boolean expected) {
        assertThat(test(between(10, 14), actual)).isEqualTo(expected);
    }

    @Test
    @DisplayName("BETWEEN with a missing min or max never matches")
    void betweenWithMissingBoundIsFalse() {
        assertThat(test(between(null, 14), 12)).isFalse();
        assertThat(test(between(10, null), 12)).isFalse();
    }

    @Test
    void betweenDescribe() {
        assertThat(new BetweenStrategy().describe(between(10, 14))).isEqualTo("10 - 14");
    }

    // ------------------------------------------------------------------ IN

    @DisplayName("IN matches any number in the list")
    @ParameterizedTest(name = "{0} in [1,2,3] -> {1}")
    @CsvSource({"1, true", "2, true", "3, true", "0, false", "4, false"})
    void inMatchesListedNumbers(int actual, boolean expected) {
        assertThat(test(in("1,2,3"), actual)).isEqualTo(expected);
    }

    @Test
    @DisplayName("IN ignores spaces around the numbers")
    void inIgnoresSpaces() {
        assertThat(test(in(" 1 , 2 "), 2)).isTrue();
    }

    @Test
    @DisplayName("IN with no list never matches")
    void inWithoutListIsFalse() {
        assertThat(test(in(null), 1)).isFalse();
    }

    @Test
    @DisplayName("IN with a non-number (old bad data) does not crash; that entry just never matches")
    void inWithBadEntryDoesNotCrash() {
        assertThatCode(() -> test(in("a,b"), 1)).doesNotThrowAnyException();
        assertThat(test(in("a,b"), 1)).isFalse();
        assertThat(test(in("a,2"), 2)).isTrue();
    }

    @Test
    void inDescribe() {
        assertThat(new InStrategy().describe(in("1,2"))).isEqualTo("in [1,2]");
    }

    // ------------------------------------------------------------------ factory

    @DisplayName("The factory has a strategy for every operator")
    @ParameterizedTest
    @EnumSource(Operator.class)
    void factoryCoversEveryOperator(Operator op) {
        assertThat(OperatorStrategyFactory.forOperator(op)).isNotNull();
    }

    @Test
    @DisplayName("The factory returns the right class and reuses one shared object per operator")
    void factoryReturnsSharedInstances() {
        assertThat(OperatorStrategyFactory.forOperator(Operator.EQ)).isInstanceOf(EqualsStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.NE)).isInstanceOf(NotEqualsStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.GT)).isInstanceOf(GreaterThanStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.GTE)).isInstanceOf(GreaterOrEqualStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.LT)).isInstanceOf(LessThanStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.LTE)).isInstanceOf(LessOrEqualStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.BETWEEN)).isInstanceOf(BetweenStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.IN)).isInstanceOf(InStrategy.class);
        assertThat(OperatorStrategyFactory.forOperator(Operator.GTE)).isSameAs(OperatorStrategyFactory.forOperator(Operator.GTE));
    }
}
