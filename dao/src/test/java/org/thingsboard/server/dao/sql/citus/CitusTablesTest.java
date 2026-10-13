// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitusTablesTest {

    @Test
    void entityTablesDistributedOnId() {
        assertThat(CitusTables.DISTRIBUTED_TABLES)
                .containsExactly("attribute_kv", "ts_kv_latest", "device", "asset", "entity_view", "alarm", "entity_alarm");
        assertThat(CitusTables.distributionColumn("attribute_kv")).isEqualTo("entity_id");
        assertThat(CitusTables.distributionColumn("ts_kv_latest")).isEqualTo("entity_id");
        assertThat(CitusTables.distributionColumn("device")).isEqualTo("id");
        assertThat(CitusTables.distributionColumn("asset")).isEqualTo("id");
        assertThat(CitusTables.distributionColumn("entity_view")).isEqualTo("id");
    }

    @Test
    void alarmTablesDistributedOnOriginatorId() {
        // alarm and entity_alarm are distributed on originator_id (co-located with the anchor group), so an
        // alarm shares a shard with its originator's attribute_kv/ts_kv_latest/device/asset/entity_view rows.
        assertThat(CitusTables.DISTRIBUTED_TABLES).contains("alarm", "entity_alarm");
        assertThat(CitusTables.distributionColumn("alarm")).isEqualTo("originator_id");
        assertThat(CitusTables.distributionColumn("entity_alarm")).isEqualTo("originator_id");
        // they must come AFTER the entity tables they co-locate with, and never become the anchor (index 0).
        assertThat(CitusTables.DISTRIBUTED_TABLES.indexOf("alarm"))
                .isGreaterThan(CitusTables.DISTRIBUTED_TABLES.indexOf("entity_view"));
        assertThat(CitusTables.DISTRIBUTED_TABLES.indexOf("entity_alarm"))
                .isGreaterThan(CitusTables.DISTRIBUTED_TABLES.indexOf("alarm"));
    }

    @Test
    void anchorIsAttributeKvFirst() {
        // Anchor-first IS load-bearing (unlike REFERENCE_TABLES order): DefaultCitusSchemaService establishes
        // the colocation group from the first not-yet-distributed table, so attribute_kv (which carries
        // shard_count) must stay first.
        assertThat(CitusTables.DISTRIBUTED_TABLES.get(0)).isEqualTo("attribute_kv");
    }

    @Test
    void dimensionTablesAreReference() {
        // REFERENCE_TABLES order is membership-only / not load-bearing (DefaultCitusSchemaService drops all
        // managed FKs before conversion and re-adds them after), so we assert membership of the load-bearing
        // entries, NOT a full list/order snapshot (that would be a change-detector).
        assertThat(CitusTables.REFERENCE_TABLES).contains(
                "key_dictionary", "tenant_profile", "converter", "rule_chain", "ota_package", "dashboard",
                "device_profile", "asset_profile", "report_template", "entity_group",
                "tenant", "customer", "tb_user", "edge",
                "integration", "scheduler_event", "role",
                "api_usage_state", "relation",
                "user_auth_settings", "user_settings", "alarm_types", "device_group_ota_package",
                "device_credentials");
    }

    @Test
    void distributedAndPartitionedTablesAreNotReference() {
        // alarm/entity_alarm are now distributed on originator_id (not reference); alarm_comment stays a plain
        // coordinator-local partitioned table (never reference).
        assertThat(CitusTables.REFERENCE_TABLES)
                .doesNotContain("device", "asset", "entity_view", "alarm", "entity_alarm",
                        "alarm_comment", "blob_entity", "report");
    }

    @Test
    void deviceCredentialsIsReferenceNotDistributed() {
        assertThat(CitusTables.REFERENCE_TABLES).contains("device_credentials");
        assertThat(CitusTables.DISTRIBUTED_TABLES).doesNotContain("device_credentials");
    }

    @Test
    void distributionColumnForUnknownTableThrows() {
        // A table missing from all three DISTRIBUTED_ON_* sets must fail loudly instead of silently
        // defaulting to a plausible-looking wrong distribution column.
        assertThatThrownBy(() -> CitusTables.distributionColumn("some_future_table"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("some_future_table");
    }
}
