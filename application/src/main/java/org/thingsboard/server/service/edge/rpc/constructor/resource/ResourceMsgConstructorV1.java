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
package org.thingsboard.server.service.edge.rpc.constructor.resource;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class ResourceMsgConstructorV1 extends BaseResourceMsgConstructor {

    @Override
    public ResourceUpdateMsg constructResourceUpdatedMsg(UpdateMsgType msgType, TbResource tbResource) {
        if (ResourceType.IMAGE.equals(tbResource.getResourceType())) {
            // Exclude support for a recently added resource type when dealing with older Edges
            // to maintain compatibility and avoid potential issues.
            return null;
        }
        ResourceUpdateMsg.Builder builder = ResourceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tbResource.getId().getId().getMostSignificantBits())
                .setIdLSB(tbResource.getId().getId().getLeastSignificantBits())
                .setTitle(tbResource.getTitle())
                .setResourceKey(tbResource.getResourceKey())
                .setResourceType(tbResource.getResourceType().name())
                .setFileName(tbResource.getFileName());
        if (tbResource.getData() != null) {
            builder.setData(tbResource.getEncodedData());
        }
        if (tbResource.getEtag() != null) {
            builder.setEtag(tbResource.getEtag());
        }
        if (TenantId.SYS_TENANT_ID.equals(tbResource.getTenantId())) {
            builder.setIsSystem(true);
        }
        return builder.build();
    }
}
