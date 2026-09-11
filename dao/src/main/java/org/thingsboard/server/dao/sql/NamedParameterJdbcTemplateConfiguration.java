// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
