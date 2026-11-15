package com.ooad.mhs.domain;

import com.ooad.mhs.util.ActionType;
import com.ooad.mhs.util.Category;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The editable details of a rule, as entered in the admin "create rule" / "edit rule" forms.
 *
 * Replaces the list of 10 separate parameters that used to be passed through the controller,
 * service and repository. The annotations are Jakarta Bean Validation rules, checked by the
 * controller ({@code @Valid}) before anything is saved; they mirror the database CHECK
 * constraints, so a bad value gives a message instead of a database error.
 *
 * Spring fills this record from the submitted form fields of the same names.
 */
public record RuleDetails(

    @Size(max = 40, message = "Rule ID can be at most 40 characters.")
    @Pattern(regexp = "[A-Za-z0-9_-]*", message = "Rule ID may only contain letters, digits, - and _.")
    String ruleId,                       // only used (and required) when creating; a rule's ID never changes

    @NotBlank(message = "Name is required.")
    @Size(max = 200, message = "Name can be at most 200 characters.")
    String name,

    @NotNull(message = "Level is required.")
    @Min(value = 1, message = "Level must be between 1 and 4.")
    @Max(value = 4, message = "Level must be between 1 and 4.")
    Integer level,

    @NotNull(message = "Category is required.")
    Category category,

    @NotNull(message = "Priority is required.")
    @Min(value = 0, message = "Priority must be between 0 and 100.")
    @Max(value = 100, message = "Priority must be between 0 and 100.")
    Integer priority,

    @NotNull(message = "Action type is required.")
    ActionType actionType,

    @Size(max = 1000, message = "Explanation can be at most 1000 characters.")
    String explanation,

    @Size(max = 200, message = "Source reference can be at most 200 characters.")
    String sourceRef,

    @Size(max = 60, message = "Conclusion key can be at most 60 characters.")
    @Pattern(regexp = "[a-z0-9_]*", message = "Conclusion key may only contain lower-case letters, digits and _.")
    String conclusionKey,

    @Size(max = 50, message = "Conclusion value can be at most 50 characters.")
    String conclusionValue
) {

    /** A conclusion needs both a key and a value (or neither): the engine sets key = value when the rule fires. */
    @AssertTrue(message = "Conclusion key and conclusion value must be filled in together (or both left empty).")
    public boolean isConclusionComplete() {
        return isBlank(conclusionKey) == isBlank(conclusionValue);
    }

    /** Same details with blank optional texts stored as null (no conclusion = null in the database). */
    public RuleDetails normalized() {
        return new RuleDetails(ruleId == null ? null : ruleId.trim(), name.trim(), level, category, priority,
            actionType, blankToNull(explanation), blankToNull(sourceRef),
            blankToNull(conclusionKey), blankToNull(conclusionValue));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
