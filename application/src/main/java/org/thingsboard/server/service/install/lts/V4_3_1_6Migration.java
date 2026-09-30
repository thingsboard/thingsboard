// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.TbClusterSchema;

import java.util.UUID;

/**
 * Cluster identity. Its DDL has a version of its own so that the {@code CREATE TABLE} race against the licence
 * context's self-heal can only ever roll back idempotent {@code tb_cluster} statements.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_3_1_6Migration implements LtsMigration {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getVersion() {
        return "4.3.1.6";
    }

    /**
     * Issued through the injected {@link JdbcTemplate} rather than a fresh connection: on the patch path this runs
     * inside the transaction that just created the table, which a separate connection would not see yet.
     */
    @Override
    public void apply() {
        UUID clusterId = UUID.randomUUID();
        int insertedRows = jdbcTemplate.update(TbClusterSchema.INSERT_CLUSTER_ID_QUERY, clusterId.toString());
        if (insertedRows > 0) {
            log.info("Generated Cluster Id: {}", clusterId);
        } else {
            log.info("Cluster Id already exists, keeping it");
        }
    }
}
