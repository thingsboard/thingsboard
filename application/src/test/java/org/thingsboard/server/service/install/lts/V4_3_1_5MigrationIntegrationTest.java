// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.install.lts.V4_3_1_5Migration.SolutionTemplateMove;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class V4_3_1_5MigrationIntegrationTest extends AbstractControllerTest {

    private static final String DESCRIPTOR = "{\"createdEntityIds\":[{\"entityType\":\"DASHBOARD\",\"id\":\"c1b3c3b0-0000-11f1-0000-000000000001\"}]}";

    // CE "Temperature & Humidity sensors": the first move in the table.
    private static final SolutionTemplateMove TEMPERATURE = V4_3_1_5Migration.SOLUTION_TEMPLATE_MOVES.get(0);

    @Autowired
    private V4_3_1_5Migration migration;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM iot_hub_installed_item WHERE tenant_id = ?", tenantId.getId());
    }

    @Test
    public void rePointsInstalledTemplateToItsSuccessorAndKeepsTheRest() {
        UUID installedVersionId = UUID.randomUUID();
        UUID rowId = insertRow("SOLUTION_TEMPLATE", TEMPERATURE.fromItemId(), installedVersionId, "Temperature & Humidity sensors", "1.0.0");

        migration.apply();

        Map<String, Object> row = row(rowId);
        assertEquals(TEMPERATURE.toItemId(), row.get("item_id"));
        assertEquals(TEMPERATURE.toItemName(), row.get("item_name"));
        // what is really installed is still the 4.2 package: version pointer, type, tenant and descriptor stay
        assertEquals(installedVersionId, row.get("item_version_id"));
        assertEquals("1.0.0", row.get("version"));
        assertEquals("SOLUTION_TEMPLATE", row.get("item_type"));
        assertEquals(tenantId.getId(), row.get("tenant_id"));
        assertEquals(JacksonUtil.toJsonNode(DESCRIPTOR), JacksonUtil.toJsonNode(row.get("descriptor").toString()));
    }

    @Test
    public void keepsOldRowWhenSuccessorIsAlreadyInstalled() {
        UUID oldRowId = insertRow("SOLUTION_TEMPLATE", TEMPERATURE.fromItemId(), UUID.randomUUID(), "Temperature & Humidity sensors", "1.0.0");
        UUID newRowId = insertRow("SOLUTION_TEMPLATE", TEMPERATURE.toItemId(), UUID.randomUUID(), TEMPERATURE.toItemName(), "1.0.0");

        migration.apply();

        assertEquals(TEMPERATURE.fromItemId(), row(oldRowId).get("item_id"));
        assertEquals(TEMPERATURE.toItemId(), row(newRowId).get("item_id"));
        assertEquals(2, countRows(TEMPERATURE.toItemId()) + countRows(TEMPERATURE.fromItemId()));
    }

    @Test
    public void leavesOtherItemTypesAndUnknownItemsAlone() {
        UUID widgetRowId = insertRow("WIDGET", TEMPERATURE.fromItemId(), UUID.randomUUID(), "Some widget", "1.0.0");
        UUID unknownItemId = UUID.randomUUID();
        UUID unknownRowId = insertRow("SOLUTION_TEMPLATE", unknownItemId, UUID.randomUUID(), "Custom template", "2.0.0");

        migration.apply();

        assertEquals(TEMPERATURE.fromItemId(), row(widgetRowId).get("item_id"));
        assertEquals(unknownItemId, row(unknownRowId).get("item_id"));
    }

    @Test
    public void secondRunChangesNothing() {
        UUID rowId = insertRow("SOLUTION_TEMPLATE", TEMPERATURE.fromItemId(), UUID.randomUUID(), "Temperature & Humidity sensors", "1.0.0");

        migration.apply();
        Map<String, Object> afterFirstRun = row(rowId);
        migration.apply();

        assertEquals(afterFirstRun, row(rowId));
        assertEquals(1, countRows(TEMPERATURE.toItemId()));
    }

    private UUID insertRow(String itemType, UUID itemId, UUID itemVersionId, String itemName, String version) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO iot_hub_installed_item (id, created_time, tenant_id, item_id, item_version_id, item_name, item_type, version, descriptor)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)""",
                id, System.currentTimeMillis(), tenantId.getId(), itemId, itemVersionId, itemName, itemType, version, DESCRIPTOR);
        return id;
    }

    private Map<String, Object> row(UUID id) {
        return jdbcTemplate.queryForMap("SELECT * FROM iot_hub_installed_item WHERE id = ?", id);
    }

    private int countRows(UUID itemId) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM iot_hub_installed_item WHERE tenant_id = ? AND item_id = ?",
                Integer.class, tenantId.getId(), itemId);
        return count == null ? 0 : count;
    }

}
