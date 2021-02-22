/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@ToString
@EqualsAndHashCode
public class AttributesCacheUpdatedMsg implements TenantAwareMsg, ToAllNodesMsg {
    public static final AttributesCacheUpdatedMsg INVALIDATE_ALL_CACHE_MSG = new AttributesCacheUpdatedMsg(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID,
            null, Collections.emptyList());
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final String scope;
    @Getter
    private final List<String> attributeKeys;

    public AttributesCacheUpdatedMsg(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.scope = scope;
        this.attributeKeys = attributeKeys;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.ATTRIBUTES_CACHE_UPDATED_MSG;
    }
}
