// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Set;

public interface TbResourceInfoDao extends Dao<TbResourceInfo> {

    PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink);

    PageData<TbResourceInfo> findTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink);

    TbResourceInfo findByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey);

    boolean existsByTenantIdAndResourceTypeAndResourceKey(TenantId tenantId, ResourceType resourceType, String resourceKey);

    Set<String> findKeysByTenantIdAndResourceTypeAndResourceKeyPrefix(TenantId tenantId, ResourceType resourceType, String prefix);

    List<TbResourceInfo> findByTenantIdAndEtagAndKeyStartingWith(TenantId tenantId, String etag, String query);

    TbResourceInfo findSystemOrTenantResourceByEtag(TenantId tenantId, ResourceType resourceType, String etag);

    boolean existsByPublicResourceKey(ResourceType resourceType, String publicResourceKey);

    TbResourceInfo findPublicResourceByKey(ResourceType resourceType, String publicResourceKey);

    List<TbResourceInfo> findSystemOrTenantResourcesByIds(TenantId tenantId, List<TbResourceId> resourceIds);
}
