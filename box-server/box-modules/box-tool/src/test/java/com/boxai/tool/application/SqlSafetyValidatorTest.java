package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSafetyValidatorTest {

    @Test
    void acceptsSelectQuery() {
        String sql = "SELECT id, name FROM users WHERE id = 1";
        assertEquals(sql, SqlSafetyValidator.validateSelectOnly(sql));
    }

    @Test
    void rejectsInsertStatement() {
        assertThrows(BusinessException.class,
                () -> SqlSafetyValidator.validateSelectOnly("INSERT INTO users(name) VALUES ('a')"));
    }

    @Test
    void rejectsMultiStatement() {
        assertThrows(BusinessException.class,
                () -> SqlSafetyValidator.validateSelectOnly("SELECT 1; DROP TABLE users"));
    }
}
