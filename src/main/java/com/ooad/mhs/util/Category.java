package com.ooad.mhs.util;

/**
 * Represents the 7 clinical screening domains plus cross-cutting.
 * 
 * Each question and rule belongs to one category. The report
 * groups findings by category so the user sees related symptoms together.
 * 
 * Used by: Question, Rule (domain)
 * Stored in: question.category and rule.category columns
 */
public enum Category {
    
    /** Depression, low mood, hopelessness, worthlessness */
    MOOD,
    
    /** Loss of interest, pleasure, motivation */
    ANHEDONIA,
    
    /** Sleep quality, insomnia, sleep disturbances */
    SLEEP,
    
    /** Worry, tension, panic, PTSD, stress */
    ANXIETY,
    
    /** Concentration, attention, ADHD symptoms */
    COGNITION,
    
    /** Work, home, social, relationship impairment, somatic symptoms */
    FUNCTIONING,
    
    /** Suicide risk, self-harm, crisis indicators */
    RISK,
    
    /** Spans multiple domains — used by NICE NG222 clinical rules */
    CROSS_CUTTING
}