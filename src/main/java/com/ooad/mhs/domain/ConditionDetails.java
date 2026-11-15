package com.ooad.mhs.domain;

import com.ooad.mhs.util.Operator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.regex.Pattern;

/**
 * The editable details of one rule condition, as entered in the admin condition form.
 *
 * Replaces the list of 7 separate parameters that used to be passed through the controller,
 * service and repository. Which value fields are needed depends on the operator:
 * BETWEEN needs valueMin and valueMax, IN needs valueText (whole numbers such as "1,2,3"),
 * every other operator needs valueInt. {@link #isValueSuitableForOperator()} checks this.
 */
public record ConditionDetails(

    @NotBlank(message = "Field is required.")
    String fieldKey,

    @NotNull(message = "Operator is required.")
    Operator operator,

    Integer valueInt,
    String valueText,
    Integer valueMin,
    Integer valueMax
) {

    /** Comma-separated whole numbers, e.g. "1,2,3" or "0, 2". */
    private static final Pattern INT_LIST =
        Pattern.compile("^\\s*-?\\d+(\\s*,\\s*-?\\d+)*\\s*$");

    /** Checks that the right value fields are filled in for the chosen operator. */
    @AssertTrue(message = "The values do not fit the operator: BETWEEN needs Min and Max (Min not above Max), "
        + "IN needs whole numbers separated by commas (e.g. 1,2,3), all other operators need Value.")
    public boolean isValueSuitableForOperator() {
        if (operator == null) return true;   // reported by @NotNull instead
        return switch (operator) {
            case BETWEEN -> valueMin != null && valueMax != null && valueMin <= valueMax;
            case IN -> valueText != null && INT_LIST.matcher(valueText).matches();
            default -> valueInt != null;
        };
    }

    /**
     * Same condition keeping only the values its operator uses (others set to null),
     * and an IN list without spaces, e.g. " 1, 2 " → "1,2".
     */
    public ConditionDetails normalized() {
        return switch (operator) {
            case BETWEEN -> new ConditionDetails(fieldKey, operator, null, null, valueMin, valueMax);
            case IN -> new ConditionDetails(fieldKey, operator, null, valueText.replaceAll("\\s", ""), null, null);
            default -> new ConditionDetails(fieldKey, operator, valueInt, null, null, null);
        };
    }
}
