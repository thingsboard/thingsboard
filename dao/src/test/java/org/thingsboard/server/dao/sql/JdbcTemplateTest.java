/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
