// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface IntegrationService extends EntityDaoService {

    Integration saveIntegration(Integration integration);

    Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId);

    ListenableFuture<Integration> findIntegrationByIdAsync(TenantId tenantId, IntegrationId integrationId);

    ListenableFuture<List<Integration>> findIntegrationsByIdsAsync(TenantId tenantId, List<IntegrationId> integrationIds);

    Optional<Integration> findIntegrationByRoutingKey(TenantId tenantId, String routingKey);

    List<Integration> findAllIntegrations(TenantId tenantId);

    List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId);

    PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink);

    List<Integration> findTenantIntegrationsByName(TenantId tenantId, String name);

    PageData<Integration> findTenantEdgeTemplateIntegrations(TenantId tenantId, PageLink pageLink);

    PageData<IntegrationInfo> findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate);

    PageData<IntegrationInfo> findTenantIntegrationInfosWithStats(TenantId tenantId, boolean isEdgeTemplate, PageLink pageLink);

    void deleteIntegration(TenantId tenantId, IntegrationId integrationId);

    void deleteIntegrationsByTenantId(TenantId tenantId);

    Long countCoreIntegrations();

    List<Integration> findAllCoreIntegrations(IntegrationType integrationType, boolean remote, boolean enabled);

    Integration assignIntegrationToEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId);

    Integration unassignIntegrationFromEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId, boolean remove);

    PageData<Integration> findIntegrationsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    PageData<IntegrationInfo> findIntegrationInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

}
