// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("install")
@Slf4j
public class SqlEntityDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements EntityDatabaseSchemaService {

    public static final String SCHEMA_ENTITIES_SQL = "schema-entities.sql";
    public static final String SCHEMA_ENTITIES_IDX_SQL = "schema-entities-idx.sql";
    public static final String SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL = "schema-entities-idx-psql-addon.sql";
    public static final String SCHEMA_VIEWS_SQL = "schema-views.sql";
    public static final String SCHEMA_FUNCTIONS_SQL = "schema-functions.sql";

    public SqlEntityDatabaseSchemaService() {
        super(SCHEMA_ENTITIES_SQL, SCHEMA_ENTITIES_IDX_SQL);
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
        super.createDatabaseIndexes();
        log.info("Installing SQL DataBase schema PostgreSQL specific indexes part: " + SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
        executeQueryFromFile(SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
    }

    @Override
    public void createOrUpdateDeviceInfoView(boolean activityStateInTelemetry) {
        String sourceViewName = activityStateInTelemetry ? "device_info_active_ts_view" : "device_info_active_attribute_view";
        executeQuery("DROP VIEW IF EXISTS device_info_view CASCADE;");
        executeQuery("CREATE OR REPLACE VIEW device_info_view AS SELECT * FROM " + sourceViewName + ";");
    }

    @Override
    public void createOrUpdateViewsAndFunctions() throws Exception {
        log.info("Installing SQL DataBase schema views: " + SCHEMA_VIEWS_SQL);
        executeQueryFromFile(SCHEMA_VIEWS_SQL);
        log.info("Installing SQL DataBase schema functions: " + SCHEMA_FUNCTIONS_SQL);
        executeQueryFromFile(SCHEMA_FUNCTIONS_SQL);
    }

}
