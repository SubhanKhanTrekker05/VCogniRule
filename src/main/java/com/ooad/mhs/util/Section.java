package com.ooad.mhs.util;

import java.util.Optional;

/**
 * The ten screening sections shown as tabs on the assessment page.
 *
 * Maps each instrument code stored in the question table (e.g. "PHQ-9")
 * to a plain-language name for users. The code itself is never changed
 * in the database, because rule sources and explanations refer to it.
 *
 * Declaration order = tab order.
 */
public enum Section {

    CSSRS("C-SSRS", "Safety Check",
        "Thoughts of death or self-harm",
        "In the past month:",
        true),
    PHQ9("PHQ-9", "Depression & Mood",
        "Low mood, loss of interest and related symptoms",
        "Over the last 2 weeks, how often have you been bothered by the following?",
        false),
    GAD7("GAD-7", "Anxiety",
        "Worry, nervousness and restlessness",
        "Over the last 2 weeks, how often have you been bothered by the following?",
        false),
    WHO5("WHO-5", "Wellbeing",
        "Your general mood and quality of life",
        "Over the last 2 weeks:",
        false),
    PSS10("PSS-10", "Stress",
        "How stressful and manageable life has felt",
        null,
        false),
    PSQI("PSQI", "Sleep Quality",
        "Your sleep habits and how well you sleep",
        "The following questions relate to your usual sleep habits during the past month.",
        false),
    PHQ15("PHQ-15", "Physical Symptoms",
        "Bodily symptoms such as pain, dizziness or fatigue",
        "During the past 4 weeks, how much have you been bothered by the following?",
        false),
    PCPTSD("PC-PTSD", "Trauma Response",
        "Effects of a frightening or upsetting experience",
        null,
        false),
    ASRS("ASRS", "Attention & Focus",
        "Concentration, organisation and restlessness",
        "Over the past 6 months:",
        false),
    WSAS("WSAS", "Daily Functioning",
        "How your difficulties affect work, home and social life",
        "Rate how much your current difficulties affect each area, from 0 (not at all) to 8 (very severely).",
        false);

    private final String code;
    private final String label;
    private final String description;
    private final String prompt;
    private final boolean mandatory;

    Section(String code, String label, String description, String prompt, boolean mandatory) {
        this.code = code;
        this.label = label;
        this.description = description;
        this.prompt = prompt;
        this.mandatory = mandatory;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getPrompt() { return prompt; }
    public boolean isMandatory() { return mandatory; }

    /** Finds the section for an instrument code such as "PHQ-9". */
    public static Optional<Section> fromCode(String code) {
        for (Section s : values()) {
            if (s.code.equals(code)) return Optional.of(s);
        }
        return Optional.empty();
    }
}
