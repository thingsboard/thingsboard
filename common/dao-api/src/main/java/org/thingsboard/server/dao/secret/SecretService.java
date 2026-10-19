// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.secret;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;

public interface SecretService extends EntityDaoService {

    Secret saveSecret(TenantId tenantId, Secret secret);

    TbSecretDeleteResult deleteSecret(TenantId tenantId, SecretInfo secretInfo);

    SecretInfo findSecretInfoById(TenantId tenantId, SecretId secretId);

    Secret findSecretById(TenantId tenantId, SecretId secretId);

    Secret findSecretByName(TenantId tenantId, String name);

    SecretInfo findSecretInfoByName(TenantId tenantId, String name);

    List<String> findSecretNamesByTenantId(TenantId tenantId);

    PageData<Secret> findSecretsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<SecretInfo> findSecretInfosByTenantId(TenantId tenantId, PageLink pageLink);

    Map<EntityType, List<EntityInfo>> findEntitiesBySecret(TenantId tenantId, SecretInfo secretInfo);

}
