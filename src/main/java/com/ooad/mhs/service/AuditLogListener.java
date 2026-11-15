package com.ooad.mhs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Observer pattern: a concrete observer. Writes each rule change to the console log. */
@Component
public class AuditLogListener implements RuleChangeListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);

    @Override
    public void onRulesChanged(String change) {
        log.info("Knowledge base changed: {}", change);
    }
}
