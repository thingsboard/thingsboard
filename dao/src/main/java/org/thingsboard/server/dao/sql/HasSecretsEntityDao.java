// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public interface HasSecretsEntityDao {

    List<EntityInfo> findByTenantIdAndSecretPlaceholder(TenantId tenantId, String placeholder);

    EntityType getEntityType();

}
