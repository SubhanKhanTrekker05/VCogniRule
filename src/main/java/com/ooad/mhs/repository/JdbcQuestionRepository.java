package com.ooad.mhs.repository;

import com.ooad.mhs.domain.Question;
import com.ooad.mhs.util.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SQLite (JDBC) implementation of {@link QuestionRepository}: loads questions from the question table.
 * 
 * 151 rows total: 84 real (shown to user) + 67 derived/conclusion (internal).
 * 
 * findAllReal() returns only the 84 user-facing questions (used to build the
 * screening form and to check which sections were completed).
 * findAll() also returns the calculated fields; ReportService uses it for the
 * labels in "Why this appears", and the admin condition form lists them as fields.
 */
@Repository
public class JdbcQuestionRepository implements QuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Question> MAPPER = (rs, rowNum) -> new Question(
        rs.getLong("id"),
        rs.getString("field_key"),
        rs.getString("text"),
        Category.valueOf(rs.getString("category")),
        rs.getInt("display_order"),
        rs.getString("instrument"),
        rs.getString("scale_type")
    );

    public JdbcQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Returns all 151 questions (real + derived + conclusion keys). */
    @Override
    public List<Question> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM question ORDER BY display_order", MAPPER);
    }

    /** Returns only the 84 real questions (excludes CALCULATED entries). */
    @Override
    public List<Question> findAllReal() {
        return jdbcTemplate.query(
            "SELECT * FROM question WHERE instrument != 'CALCULATED' ORDER BY display_order",
            MAPPER);
    }
}