// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.TranslationInfo;

import java.util.List;
import java.util.Set;

public interface TranslationService {

    List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId);

    Set<String> getAvailableLocaleCodes(TenantId tenantId, CustomerId customerId);

    JsonNode getLoginTranslation(String localeCode, String domainName);

    JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    JsonNode getTranslationForBasicEdit(TenantId tenantId, CustomerId customerId, String localeCode);

}
