// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.secret;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.TenantId;

public interface SecretConfigurationService {

    void replaceSecretUsages(TenantId tenantId, JsonNode config);

    <T> T replaceSecretUsages(TenantId tenantId, T entity, Class<T> clazz);

    String replaceSecretUsage(TenantId tenantId, String value);

}
