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
