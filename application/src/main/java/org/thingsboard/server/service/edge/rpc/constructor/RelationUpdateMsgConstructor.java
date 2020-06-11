/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

@Component
@Slf4j
public class RelationUpdateMsgConstructor {

    public RelationUpdateMsg constructRelationUpdatedMsg(UpdateMsgType msgType, EntityRelation entityRelation) {
        RelationUpdateMsg.Builder builder = RelationUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setFromIdMSB(entityRelation.getFrom().getId().getMostSignificantBits())
                .setFromIdLSB(entityRelation.getFrom().getId().getLeastSignificantBits())
                .setFromEntityType(entityRelation.getFrom().getEntityType().name())
                .setToIdMSB(entityRelation.getTo().getId().getMostSignificantBits())
                .setToIdLSB(entityRelation.getTo().getId().getLeastSignificantBits())
                .setToEntityType(entityRelation.getTo().getEntityType().name())
                .setType(entityRelation.getType())
                .setAdditionalInfo(JacksonUtil.toString(entityRelation.getAdditionalInfo()));
        if (entityRelation.getTypeGroup() != null) {
            builder.setTypeGroup(entityRelation.getTypeGroup().name());
        }
        return builder.build();
    }
}
