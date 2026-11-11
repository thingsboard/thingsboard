// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface IntegrationInfoDao extends Dao<IntegrationInfo> {

    PageData<IntegrationInfo> findByTenantIdAndIsEdgeTemplate(UUID tenantId, PageLink pageLink, boolean isEdgeTemplate);

    PageData<IntegrationInfo> findIntegrationsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    PageData<IntegrationInfo> findAllIntegrationInfosWithStats(UUID tenantId, boolean isEdgeTemplate, PageLink pageLink);

}
