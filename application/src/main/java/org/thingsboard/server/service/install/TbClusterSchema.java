// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

/**
 * The statements that reach {@code tb_cluster} from Java. The DDL is a hand-kept copy of the block in
 * {@code schema-entities.sql}; {@code LtsMigrationIntegrationTest} pins the copies against it.
 */
public final class TbClusterSchema {

    // ORDER BY ... LIMIT 1 so the read stays single-valued on a table that somehow holds more than one row, which
    // no retry could repair.
    public static final String SELECT_CLUSTER_ID_QUERY = "SELECT cluster_id FROM tb_cluster ORDER BY cluster_id LIMIT 1";

    // The NOT EXISTS guard keeps a re-run from minting a second identity; ON CONFLICT DO NOTHING covers another node
    // inserting between this statement's read and its write, and needs the tb_cluster_single_row index to conflict
    // on - the primary key alone cannot collide, since each racing node offers a cluster id of its own.
    public static final String INSERT_CLUSTER_ID_QUERY =
            "INSERT INTO tb_cluster (cluster_id) SELECT ?::uuid WHERE NOT EXISTS (SELECT 1 FROM tb_cluster) ON CONFLICT DO NOTHING";

    public static final String CREATE_CLUSTER_TABLE_QUERY = """
            CREATE TABLE IF NOT EXISTS tb_cluster (
                cluster_id uuid NOT NULL,
                license_secret varchar,
                license_claim_token varchar,
                non_production_uptime_ms bigint NOT NULL DEFAULT 0,
                non_production_last_tick bigint,
                non_production_confirmed_ts bigint,
                CONSTRAINT tb_cluster_pkey PRIMARY KEY (cluster_id)
            );""";

    // Allows at most one row in the table, no matter how many nodes race to insert.
    public static final String CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY =
            "CREATE UNIQUE INDEX IF NOT EXISTS tb_cluster_single_row ON tb_cluster ((true));";

    private TbClusterSchema() {
    }

}
