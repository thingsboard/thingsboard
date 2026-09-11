// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.AbstractJpaDaoTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
        "spring.jpa.properties.javax.persistence.query.timeout=500"
})
public class JdbcTemplateTest extends AbstractJpaDaoTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void queryTimeoutTest() {
        assertThrows(QueryTimeoutException.class, () -> jdbcTemplate.query("SELECT pg_sleep(10)", rs -> {}));
    }
}
