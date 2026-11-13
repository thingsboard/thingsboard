// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

/**
 * Creates {@code tb_cluster} and mints the cluster id on the no-downtime patch path, which runs neither
 * {@code schema-entities.sql} nor {@code generateClusterIdIfNotExist()}. Both steps are idempotent.
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

    // Must run in the migration transaction: tb_cluster is created there and is not yet visible to other connections.
    @Override
    public void apply() {
        UUID clusterId = UUID.randomUUID();
        int insertedRows = jdbcTemplate.update(
                "INSERT INTO tb_cluster (cluster_id) SELECT ?::uuid WHERE NOT EXISTS (SELECT 1 FROM tb_cluster) ON CONFLICT DO NOTHING",
                clusterId.toString());
        if (insertedRows > 0) {
            log.info("Generated Cluster Id: {}", clusterId);
        } else {
            log.info("Cluster Id already exists, keeping it");
        }
    }
}
