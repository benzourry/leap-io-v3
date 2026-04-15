package com.benzourry.leap.repository;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class EntryBatchRepository {

    public record EntryUpdateDto(Long entryId, String path, String value) {}
    private final JdbcTemplate jdbcTemplate;

    public EntryBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchUpdateDataFields(List<EntryUpdateDto> updates) {
        String sql = "update entry set data = json_set(data,?,JSON_EXTRACT(?,'$[0]')) where entry.id = ?";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EntryUpdateDto update = updates.get(i);
                ps.setString(1, update.path());
                // 2. The Value
                if (update.value() == null) {
                    ps.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    // CRITICAL: Wrap the GraalVM JSON string in an array bracket!
                    // - 123 becomes "[123]"
                    // - "hello" becomes "[\"hello\"]"
                    // - true becomes "[true]"
                    // - {"a": 1} becomes "[{\"a\": 1}]"
                    ps.setString(2, "[" + update.value() + "]");
                }
                ps.setLong(3, update.entryId());
            }

            @Override
            public int getBatchSize() {
                return updates.size();
            }
        });
    }
}