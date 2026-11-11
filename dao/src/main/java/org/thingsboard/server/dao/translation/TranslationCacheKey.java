// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.translation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranslationCacheKey implements HasTenantId {

    private TenantId tenantId;
    private CustomerId customerId;
    private String localeCode;
    private String domain;

    public static TranslationCacheKey forTenant(TenantId tenantId) {
        return new TranslationCacheKey(tenantId, null, null, null);
    }

    public static TranslationCacheKey forFullTranslation(TenantId tenantId, CustomerId customerId, String locale) {
        return new TranslationCacheKey(tenantId, customerId, locale, null);
    }

    public static TranslationCacheKey forLoginTranslation(TenantId tenantId, CustomerId customerId, String locale, String domain) {
        return new TranslationCacheKey(tenantId, customerId, locale, domain);
    }

    public static TranslationCacheKey forLoginTranslation(String locale, String type) {
        return new TranslationCacheKey(null, null, locale, type);
    }

}
