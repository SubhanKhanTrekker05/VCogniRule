package com.ooad.mhs.repository;

import com.ooad.mhs.domain.Question;

import java.util.List;

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
}
