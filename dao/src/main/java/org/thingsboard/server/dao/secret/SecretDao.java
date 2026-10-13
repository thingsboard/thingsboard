// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.secret;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.Dao;

import java.util.Map;

public interface SecretDao extends Dao<Secret> {

    PageData<Secret> findByTenantId(TenantId tenantId, PageLink pageLink);

    Secret findByName(TenantId tenantId, String key);

    void deleteByTenantId(TenantId tenantId);

    Map<String, Long> countSecretsPerType();

}
