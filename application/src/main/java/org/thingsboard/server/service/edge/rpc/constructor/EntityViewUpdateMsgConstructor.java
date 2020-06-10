/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.gen.edge.EdgeEntityType;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

@Component
@Slf4j
public class EntityViewUpdateMsgConstructor {

    public EntityViewUpdateMsg constructEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView) {
        EdgeEntityType entityType;
        switch (entityView.getEntityId().getEntityType()) {
            case DEVICE:
                entityType = EdgeEntityType.DEVICE;
                break;
            case ASSET:
                entityType = EdgeEntityType.ASSET;
                break;
            default:
                throw new RuntimeException("Unsupported entity type [" + entityView.getEntityId().getEntityType() + "]");
        }
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(entityView.getId().getId().getMostSignificantBits())
                .setIdLSB(entityView.getId().getId().getLeastSignificantBits())
                .setName(entityView.getName())
                .setType(entityView.getType())
                .setIdMSB(entityView.getEntityId().getId().getMostSignificantBits())
                .setIdLSB(entityView.getEntityId().getId().getLeastSignificantBits())
                .setEntityType(entityType);
        return builder.build();
    }

}
