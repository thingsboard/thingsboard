// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultDatabaseSchemaSettingsServiceTest {

    @Mock
    private ProjectInfo projectInfo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DefaultDatabaseSchemaSettingsService service;

    @Test
    void updateSchemaVersionWithExplicitVersionEncodesAsLong() {
        service.updateSchemaVersion("4.2.2.3");
        verify(jdbcTemplate).execute("UPDATE tb_schema_settings SET schema_version = 4002002003");
    }

    @Test
    void updateSchemaVersionWithShortVersionPadsMissingComponents() {
        service.updateSchemaVersion("4.3");
        verify(jdbcTemplate).execute("UPDATE tb_schema_settings SET schema_version = 4003000000");
    }
}
