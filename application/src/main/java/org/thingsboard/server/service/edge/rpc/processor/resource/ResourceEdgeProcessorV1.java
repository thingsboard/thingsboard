/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class ResourceEdgeProcessorV1 extends ResourceEdgeProcessor {

    @Override
    protected TbResource constructResourceFromUpdateMsg(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg) {
        TbResource resource = new TbResource();
        if (resourceUpdateMsg.getIsSystem()) {
            resource.setTenantId(TenantId.SYS_TENANT_ID);
        } else {
            resource.setTenantId(tenantId);
        }
        resource.setCreatedTime(Uuids.unixTimestamp(tbResourceId.getId()));
        resource.setTitle(resourceUpdateMsg.getTitle());
        resource.setResourceKey(resourceUpdateMsg.getResourceKey());
        resource.setResourceType(ResourceType.valueOf(resourceUpdateMsg.getResourceType()));
        resource.setFileName(resourceUpdateMsg.getFileName());
        resource.setEncodedData(resourceUpdateMsg.hasData() ? resourceUpdateMsg.getData() : null);
        resource.setEtag(resourceUpdateMsg.hasEtag() ? resourceUpdateMsg.getEtag() : null);
        return resource;
    }

}
