// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SqlTraceDatabaseSchemaServiceTest {

    @Test
    public void givenTraceDbSchemaService_whenCreateDatabaseSchemaWithoutIndexes_thenOnlyTraceSchemaIsExecuted() throws Exception {
        SqlTraceDatabaseSchemaService service = spy(new SqlTraceDatabaseSchemaService());
        willDoNothing().given(service).executeQueryFromFile(anyString());

        // The upgrade path calls this overload; the trace script carries its own indexes.
        service.createDatabaseSchema(false);

        verify(service, times(1)).executeQueryFromFile(SqlTraceDatabaseSchemaService.SCHEMA_TS_TRACE_SQL);
        verify(service, times(1)).executeQueryFromFile(anyString());
    }

    @Test
    public void givenTraceDbSchemaService_whenCreateDatabaseSchema_thenNoSeparateIndexScriptIsExecuted() throws Exception {
        SqlTraceDatabaseSchemaService service = spy(new SqlTraceDatabaseSchemaService());
        willDoNothing().given(service).executeQueryFromFile(anyString());

        service.createDatabaseSchema();

        verify(service, times(1)).executeQueryFromFile(SqlTraceDatabaseSchemaService.SCHEMA_TS_TRACE_SQL);
        verify(service, times(1)).executeQueryFromFile(anyString());
    }

    @Test
    public void givenDedicatedEventsDataSourceDisabled_whenResolvingConnection_thenMainDataSourceIsUsed() {
        SqlTraceDatabaseSchemaService service = newServiceWithDataSources(false);

        assertEquals("jdbc:postgresql://main:5432/thingsboard", service.getDbUrl());
        assertEquals("main-user", service.getDbUserName());
        assertEquals("main-pass", service.getDbPassword());
    }

    @Test
    public void givenDedicatedEventsDataSourceEnabled_whenResolvingConnection_thenEventsDataSourceIsUsed() {
        SqlTraceDatabaseSchemaService service = newServiceWithDataSources(true);

        assertEquals("jdbc:postgresql://events:5432/thingsboard_events", service.getDbUrl());
        assertEquals("events-user", service.getDbUserName());
        assertEquals("events-pass", service.getDbPassword());
    }

    private SqlTraceDatabaseSchemaService newServiceWithDataSources(boolean dedicatedEventsDataSourceEnabled) {
        SqlTraceDatabaseSchemaService service = new SqlTraceDatabaseSchemaService();
        ReflectionTestUtils.setField(service, "dbUrl", "jdbc:postgresql://main:5432/thingsboard");
        ReflectionTestUtils.setField(service, "dbUserName", "main-user");
        ReflectionTestUtils.setField(service, "dbPassword", "main-pass");
        ReflectionTestUtils.setField(service, "dedicatedEventsDataSourceEnabled", dedicatedEventsDataSourceEnabled);
        ReflectionTestUtils.setField(service, "eventsDbUrl", "jdbc:postgresql://events:5432/thingsboard_events");
        ReflectionTestUtils.setField(service, "eventsDbUserName", "events-user");
        ReflectionTestUtils.setField(service, "eventsDbPassword", "events-pass");
        return service;
    }

}
