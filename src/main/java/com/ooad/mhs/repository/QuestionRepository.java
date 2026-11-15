package com.ooad.mhs.repository;

import com.ooad.mhs.domain.Question;

import java.util.List;
import java.util.Optional;

/**
 * Loads the screening questions and calculated fields.
 *
 * Repository pattern + Dependency Inversion: services depend on this interface;
 * the SQLite implementation is {@link JdbcQuestionRepository}.
 */
public interface QuestionRepository {

    /** All 151 rows: 84 real questions + 67 calculated fields, in display order. */
    List<Question> findAll();

    /** Only the 84 questions shown to the user, in display order. */
    List<Question> findAllReal();

    /** One question or calculated field by its field key, e.g. "phq9_q1". */
    Optional<Question> findByFieldKey(String fieldKey);
}
