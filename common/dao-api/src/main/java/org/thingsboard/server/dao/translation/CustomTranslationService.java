// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.translation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;

import java.util.Set;

public interface CustomTranslationService {

    JsonNode getCurrentCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    JsonNode getMergedTenantCustomTranslation(TenantId tenantId, String localeCode);

    JsonNode getMergedCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    void saveCustomTranslation(CustomTranslation customTranslation);

    void patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode customTranslation);

    void deleteCustomTranslationKeyByPath(TenantId tenantId, CustomerId customerId, String localeCode, String key);

    void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    Set<String> getCurrentCustomizedLocales(TenantId tenantId, CustomerId customerId);

    Set<String> getMergedTenantCustomizedLocales(TenantId tenantId);

    Set<String> getMergedCustomerCustomizedLocales(TenantId tenantId, CustomerId customerId);

    void deleteCustomTranslationByTenantId(TenantId tenantId);

}
