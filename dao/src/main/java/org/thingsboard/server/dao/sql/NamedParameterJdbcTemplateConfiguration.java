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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NamedParameterJdbcTemplateConfiguration {

    @Value("${spring.jpa.properties.javax.persistence.query.timeout:30000}")
    private int queryTimeout;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @PostConstruct
    private void init() {
        int timeout = Math.max(1, (int) TimeUnit.MILLISECONDS.toSeconds(queryTimeout));
        log.info("Set jdbcTemplate query timeout [{}] second(s)", timeout);
        namedParameterJdbcTemplate.getJdbcTemplate().setQueryTimeout(timeout);
    }
}
