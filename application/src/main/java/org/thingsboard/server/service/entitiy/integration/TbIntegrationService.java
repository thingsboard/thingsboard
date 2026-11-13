// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.integration;

import org.thingsboard.server.common.data.IntegrationConvertersInfo;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface TbIntegrationService {

    PageData<IntegrationInfo> findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate);

    PageData<IntegrationInfo> findIntegrationInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    Map<IntegrationType, IntegrationConvertersInfo> getIntegrationsConvertersInfo(TenantId tenantId);

    /**
     * The check before an integration is saved: its configuration, with the secrets it uses, is validated by its type.
     * An edge template isn't validated. An invalid configuration fails with an {@link ExecutionException}.
     */
    void validateIntegrationConfiguration(TenantId tenantId, Integration integration) throws ExecutionException, InterruptedException, TimeoutException;
}
