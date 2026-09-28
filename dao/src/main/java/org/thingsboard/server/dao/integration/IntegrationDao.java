// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationDao extends Dao<Integration>, TenantEntityDao<Integration>, ExportableEntityDao<IntegrationId, Integration> {

    PageData<Integration> findByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Integration> findCoreIntegrationsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Integration> findEdgeTemplateIntegrationsByTenantId(UUID tenantId, PageLink pageLink);

    Optional<Integration> findByRoutingKey(UUID tenantId, String routingKey);

    List<Integration> findByConverterId(UUID tenantId, UUID converterId);

    ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> integrationIds);

    List<Integration> findTenantIntegrationsByName(UUID tenantId, String name);

    PageData<Integration> findIntegrationsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    Long countCoreIntegrations();

    Map<String, Long> countIntegrationsPerType();

    List<Integration> findAllCoreIntegrations(IntegrationType integrationType, boolean remote, boolean enabled);

}
