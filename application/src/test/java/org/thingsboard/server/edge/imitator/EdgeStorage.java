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
package org.thingsboard.server.edge.imitator;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class EdgeStorage {

    private EdgeConfiguration configuration;

    private Map<UUID, EntityType> entities;
    private Map<String, AlarmStatus> alarms;
    private List<EntityRelation> relations;

    public EdgeStorage() {
        entities = new HashMap<>();
        alarms = new HashMap<>();
        relations = new ArrayList<>();
    }

    public ListenableFuture<Void> processEntity(UpdateMsgType msgType, EntityType type, UUID uuid) {
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                entities.put(uuid, type);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                entities.remove(uuid);
                break;
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processRelation(RelationUpdateMsg relationMsg) {
        EntityRelation relation = new EntityRelation();
        relation.setType(relationMsg.getType());
        relation.setTypeGroup(RelationTypeGroup.valueOf(relationMsg.getTypeGroup()));
        relation.setTo(EntityIdFactory.getByTypeAndUuid(relationMsg.getToEntityType(), new UUID(relationMsg.getToIdMSB(), relationMsg.getToIdLSB())));
        relation.setFrom(EntityIdFactory.getByTypeAndUuid(relationMsg.getFromEntityType(), new UUID(relationMsg.getFromIdMSB(), relationMsg.getFromIdLSB())));
        switch (relationMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                relations.add(relation);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                relations.remove(relation);
                break;
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processAlarm(AlarmUpdateMsg alarmMsg) {
        switch (alarmMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
            case ALARM_ACK_RPC_MESSAGE:
            case ALARM_CLEAR_RPC_MESSAGE:
                alarms.put(alarmMsg.getType(), AlarmStatus.valueOf(alarmMsg.getStatus()));
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                alarms.remove(alarmMsg.getName());
                break;
        }
        return Futures.immediateFuture(null);
    }

    public Set<UUID> getEntitiesByType(EntityType type) {
        return entities.entrySet().stream()
                .filter(entry -> entry.getValue().equals(type))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
    }

}
