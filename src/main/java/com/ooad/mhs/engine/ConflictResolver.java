package com.ooad.mhs.engine;

import com.ooad.mhs.domain.Finding;

import java.util.Comparator;
import java.util.List;

/**
 * Sorts findings by priority (highest first) and applies conflict resolution.
 * 
 * Crisis findings (priority >= 90) are always shown first — they naturally
 * appear at the top due to priority sorting. Other findings are shown BELOW
 * (NOT suppressed) per clinical safety feedback.
 * 
 * Pure logic, no Spring annotations.
 */
public class ConflictResolver {

    /**
     * @param findings  All fired findings (unsorted)
     * @return Findings sorted by priority descending (crisis first, info last)
     */
    public static List<Finding> resolve(List<Finding> findings) {
        return findings.stream()
            .sorted(Comparator.comparingInt(Finding::priority).reversed())
            .toList();
    }
}