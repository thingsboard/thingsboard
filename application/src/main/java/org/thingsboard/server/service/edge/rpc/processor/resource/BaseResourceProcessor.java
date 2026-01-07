/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.resource;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseResourceProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<TbResource> resourceValidator;

    protected boolean saveOrUpdateTbResource(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg) {
        boolean resourceKeyUpdated = false;
        try {
            TbResource resource = JacksonUtil.fromString(resourceUpdateMsg.getEntity(), TbResource.class, true);
            if (resource == null) {
                throw new RuntimeException("[{" + tenantId + "}] resourceUpdateMsg {" + resourceUpdateMsg + " } cannot be converted to resource");
            }
            boolean created = false;
            TbResource resourceById = edgeCtx.getResourceService().findResourceById(tenantId, tbResourceId);
            if (resourceById == null) {
                resource.setCreatedTime(Uuids.unixTimestamp(tbResourceId.getId()));
                created = true;
                resource.setId(null);
            } else {
                resource.setId(tbResourceId);
            }
            String resourceKey = resource.getResourceKey();
            ResourceType resourceType = resource.getResourceType();
            if (!created && !resourceType.isUpdatable()) {
                resource.setData(null);
            }
            PageDataIterable<TbResource> resourcesIterable = new PageDataIterable<>(
                    link -> edgeCtx.getResourceService().findTenantResourcesByResourceTypeAndPageLink(tenantId, resourceType, link), 1024);
            for (TbResource tbResource : resourcesIterable) {
                if (tbResource.getResourceKey().equals(resourceKey) && !tbResourceId.equals(tbResource.getId())) {
                    resourceKey = StringUtils.randomAlphabetic(15) + "_" + resourceKey;
                    log.warn("[{}] Resource with resource type {} and key {} already exists. Renaming resource key to {}",
                            tenantId, resourceType, resource.getResourceKey(), resourceKey);
                    resourceKeyUpdated = true;
                }
            }
            resource.setResourceKey(resourceKey);
            resourceValidator.validate(resource, TbResourceInfo::getTenantId);
            if (created) {
                resource.setId(tbResourceId);
            }
            edgeCtx.getResourceService().saveResource(resource, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process resource update msg [{}]", tenantId, resourceUpdateMsg, e);
            throw e;
        }
        return resourceKeyUpdated;
    }

}
