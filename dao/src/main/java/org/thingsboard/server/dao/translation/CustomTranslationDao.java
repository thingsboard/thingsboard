// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.translation;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CustomTranslationDao {

    CustomTranslation save(TenantId tenantId, CustomTranslation customTranslation);

    CustomTranslation findById(TenantId tenantId, CustomTranslationCompositeKey key);

    void removeById(TenantId tenantId, CustomTranslationCompositeKey key);

    Set<String> findLocalesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    List<CustomTranslationCompositeKey> findCustomTranslationByTenantId(UUID tenantId);
}
