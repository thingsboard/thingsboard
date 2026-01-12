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

import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SqlEntityDatabaseSchemaServiceTest {

    @Test
    public void givenPsqlDbSchemaService_whenCreateDatabaseSchema_thenVerifyPsqlIndexSpecificCall() throws Exception {
        SqlEntityDatabaseSchemaService service = spy(new SqlEntityDatabaseSchemaService());
        willDoNothing().given(service).executeQueryFromFile(anyString());

        service.createDatabaseSchema();

        verify(service, times(1)).createDatabaseIndexes();
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_SQL);
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_SQL);
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
        verify(service, times(3)).executeQueryFromFile(anyString());
    }

    @Test
    public void givenPsqlDbSchemaService_whenCreateDatabaseIndexes_thenVerifyPsqlIndexSpecificCall() throws Exception {
        SqlEntityDatabaseSchemaService service = spy(new SqlEntityDatabaseSchemaService());
        willDoNothing().given(service).executeQueryFromFile(anyString());

        service.createDatabaseIndexes();

        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_SQL);
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
        verify(service, times(2)).executeQueryFromFile(anyString());
    }

}
