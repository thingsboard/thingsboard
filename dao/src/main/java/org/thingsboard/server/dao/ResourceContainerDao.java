// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public interface ResourceContainerDao<T extends HasId<?>> {

    List<EntityInfo> findByTenantIdAndResource(TenantId tenantId, String reference, int limit);

    List<EntityInfo> findByResource(String reference, int limit);

}
