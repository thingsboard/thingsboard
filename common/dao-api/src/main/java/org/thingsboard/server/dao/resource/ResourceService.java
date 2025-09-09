/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.resource;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.TbResourceDeleteResult;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.List;

public interface ResourceService extends EntityDaoService {

    TbResource saveResource(TbResource resource);

    TbResource saveResource(TbResource resource, boolean doValidate);

    TbResource findResourceByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey);

    TbResource findResourceById(TenantId tenantId, TbResourceId resourceId);

    byte[] getResourceData(TenantId tenantId, TbResourceId resourceId);

    TbResourceDataInfo getResourceDataInfo(TenantId tenantId, TbResourceId resourceId);

    ResourceExportData exportResource(TbResourceInfo resourceInfo);

    List<ResourceExportData> exportResources(TenantId tenantId, Collection<TbResourceInfo> resources);

    TbResource toResource(TenantId tenantId, ResourceExportData exportData);

    void importResources(TenantId tenantId, List<ResourceExportData> resources);

    TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId);

    TbResourceInfo findResourceInfoByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey);

    PageData<TbResource> findAllTenantResources(TenantId tenantId, PageLink pageLink);

    ListenableFuture<TbResourceInfo> findResourceInfoByIdAsync(TenantId tenantId, TbResourceId resourceId);

    PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink);

    PageData<TbResourceInfo> findTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink);

    List<TbResource> findTenantResourcesByResourceTypeAndObjectIds(TenantId tenantId, ResourceType lwm2mModel, String[] objectIds);

    PageData<TbResource> findTenantResourcesByResourceTypeAndPageLink(TenantId tenantId, ResourceType lwm2mModel, PageLink pageLink);

    TbResourceDeleteResult deleteResource(TenantId tenantId, TbResourceId resourceId, boolean force);

    void deleteResourcesByTenantId(TenantId tenantId);

    long sumDataSizeByTenantId(TenantId tenantId);

    String calculateEtag(byte[] data);

    TbResourceInfo findSystemOrTenantResourceByEtag(TenantId tenantId, ResourceType resourceType, String etag);

    boolean updateResourcesUsage(TenantId tenantId, Dashboard dashboard);

    boolean updateResourcesUsage(TenantId tenantId, WidgetTypeDetails widgetTypeDetails);

    Collection<TbResourceInfo> getUsedResources(TenantId tenantId, Dashboard dashboard);

    Collection<TbResourceInfo> getUsedResources(TenantId tenantId, WidgetTypeDetails widgetTypeDetails);

    TbResource createOrUpdateSystemResource(ResourceType resourceType, ResourceSubType resourceSubType, String resourceKey, byte[] data);

    List<TbResourceInfo> findSystemOrTenantResourcesByIds(TenantId tenantId, List<TbResourceId> resourceIds);

}
