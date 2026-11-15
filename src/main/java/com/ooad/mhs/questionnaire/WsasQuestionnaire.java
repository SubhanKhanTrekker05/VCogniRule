package com.ooad.mhs.questionnaire;

import com.ooad.mhs.util.Section;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WSAS: work and social adjustment (5 items, each 0-8, total 0-40).
 * Derived field: wsas_total.
 */
@Component
public class WsasQuestionnaire extends Questionnaire {

    public WsasQuestionnaire() {
        super(Section.WSAS);
    }

    @Override
    protected void computeScores(Map<String, Integer> f) {
        f.put("wsas_total", sum(f, "wsas_q", 1, 5));
    }

    @Override
    public String summary(Map<String, Integer> f, Map<String, String> answers) {
        return "Total score " + f.get("wsas_total") + " / 40";
    }
}
