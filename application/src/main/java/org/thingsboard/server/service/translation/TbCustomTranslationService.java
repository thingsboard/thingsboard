// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.service.custommenu.EtagCacheService;

public interface TbCustomTranslationService extends EtagCacheService<TranslationCacheKey> {

    void saveCustomTranslation(CustomTranslation customTranslation);

    void patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode customTranslation);

    void deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String key);

    void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

}
