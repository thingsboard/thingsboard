// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("install")
@Slf4j
public class SqlTraceDatabaseSchemaService extends SqlAbstractDatabaseSchemaService {
    public static final String SCHEMA_TS_TRACE_SQL = "schema-trace.sql";

    @Value("${spring.datasource.events.enabled:false}")
    private boolean dedicatedEventsDataSourceEnabled;

    @Value("${spring.datasource.events.url:}")
    private String eventsDbUrl;

    @Value("${spring.datasource.events.username:}")
    private String eventsDbUserName;

    @Value("${spring.datasource.events.password:}")
    private String eventsDbPassword;

    public SqlTraceDatabaseSchemaService() {
        super(SCHEMA_TS_TRACE_SQL, null);
    }

    @Override
    protected String getDbUrl() {
        return dedicatedEventsDataSourceEnabled ? eventsDbUrl : super.getDbUrl();
    }

    @Override
    protected String getDbUserName() {
        return dedicatedEventsDataSourceEnabled ? eventsDbUserName : super.getDbUserName();
    }

    @Override
    protected String getDbPassword() {
        return dedicatedEventsDataSourceEnabled ? eventsDbPassword : super.getDbPassword();
    }
}
