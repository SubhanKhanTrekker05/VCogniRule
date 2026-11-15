package com.ooad.mhs.service;

/**
 * Observer pattern: the observer interface. {@link AdminService} (the subject) calls every
 * listener after each change to the knowledge base. Observers: {@link RuleCacheRefresher}
 * and {@link AuditLogListener}. A new reaction to rule changes = a new listener class,
 * with no change to AdminService (Open/Closed Principle).
 */
public interface RuleChangeListener {

    /** @param change what changed, e.g. "rule 12 updated" */
    void onRulesChanged(String change);
}
