// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
