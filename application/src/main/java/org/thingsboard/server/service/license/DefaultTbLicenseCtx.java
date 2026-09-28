// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.license.client.InstanceRegistry;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.server.dao.instance.registry.InstanceRegistryService;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.install.TbClusterSchema;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTbLicenseCtx implements TbLicenseCtx {

    private final InstanceRegistryService instanceRegistryService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final DiscoveryService discoveryService;
    private final JdbcTemplate jdbcTemplate;

    private UUID clusterId;

    @Value("${TB_LICENSE_CLUSTER_ID:}")
    public void setClusterId(String value) {
        if (value != null && !value.isBlank()) {
            try {
                this.clusterId = UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid TB_LICENSE_CLUSTER_ID: " + value, e);
            }
        }
    }

    @Override
    public InstanceRegistry save(InstanceRegistry instanceRegistry) {
        return instanceRegistryService.save(instanceRegistry);
    }

    @Override
    public InstanceRegistry findByServiceId(String serviceId) {
        return instanceRegistryService.findByServiceId(serviceId);
    }

    @Override
    public List<InstanceRegistry> findAll() {
        return instanceRegistryService.findAll();
    }

    @Override
    public void deleteByServiceId(String serviceId) {
        instanceRegistryService.deleteByServiceId(serviceId);
    }

    @Override
    public String getServiceId() {
        return serviceInfoProvider.getServiceId();
    }

    /**
     * The deployment identity, self-healing {@code tb_cluster} when the no-downtime patch path has not created it
     * yet: {@code SystemPatchApplier} does that on a background thread nothing joins, so this read - made while the
     * licence client bean initialises - can reach the database first, and throwing would leave the node booted
     * un-activated until {@code BasicLicenseActivationService} retries.
     */
    @Override
    public UUID getClusterId() {
        if (clusterId != null) {
            return clusterId;
        }
        try {
            return jdbcTemplate.queryForObject(TbClusterSchema.SELECT_CLUSTER_ID_QUERY, UUID.class);
        } catch (BadSqlGrammarException e) {
            return selfHealClusterId(true);
        } catch (EmptyResultDataAccessException e) {
            return selfHealClusterId(false);
        }
    }

    /**
     * @param createTable whether {@code tb_cluster} itself is missing; false means mint the row only.
     */
    private UUID selfHealClusterId(boolean createTable) {
        if (createTable) {
            // Each statement is attempted on its own: a collision on one must not skip the ones after it.
            runQuietly("create table tb_cluster", () -> jdbcTemplate.execute(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY));
        }
        // Before the mint in both branches: without this index the INSERT's ON CONFLICT DO NOTHING has nothing
        // to conflict on, so two racing nodes each insert a row carrying a different cluster id.
        runQuietly("create index tb_cluster_single_row", () -> jdbcTemplate.execute(TbClusterSchema.CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY));
        runQuietly("mint the cluster id", () ->
                jdbcTemplate.update(TbClusterSchema.INSERT_CLUSTER_ID_QUERY, UUID.randomUUID().toString()));
        return jdbcTemplate.queryForObject(TbClusterSchema.SELECT_CLUSTER_ID_QUERY, UUID.class);
    }

    private void runQuietly(String description, Runnable statement) {
        try {
            statement.run();
        } catch (DataAccessException e) {
            // Not rethrown: the node racing this one may have done the work already, and the read that follows tells
            // that apart from a genuine failure. Message only - the collision this tolerates is expected, and its
            // stack trace would print on every upgrade.
            log.warn("Failed to {}: {}", description, e.getMessage());
        }
    }

    @Override
    public boolean isServiceAvailable(String serviceId) {
        return discoveryService.getOtherServers().stream().anyMatch(serviceInfo -> serviceInfo.getServiceId().equals(serviceId));
    }
}
