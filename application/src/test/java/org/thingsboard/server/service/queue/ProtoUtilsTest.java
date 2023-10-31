/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import  java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProtoUtilsTest {

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("35e10f77-16e7-424d-ae46-ee780f87ac4f"));
    EntityId entityId = new RuleChainId(UUID.fromString("c640b635-4f0f-41e6-b10b-25a86003094e"));
    @Test
    void protoComponentLifecycleSerialization() {
        ComponentLifecycleMsg msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
        msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.STARTED);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoEntityTypeSerialization() {
        for(EntityType entityType : EntityType.values()){
            assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(entityType))).as(entityType.getNormalName()).isEqualTo(entityType);
        }
    }

}