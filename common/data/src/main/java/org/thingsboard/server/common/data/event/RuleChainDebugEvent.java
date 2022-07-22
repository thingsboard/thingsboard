/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class RuleChainDebugEvent extends Event {

    private static final long serialVersionUID = -386392236201116767L;

    @Builder
    private RuleChainDebugEvent(TenantId tenantId, EntityId entityId, String serviceId, String message, String error) {
        super(tenantId, entityId, serviceId);
        this.message = message;
        this.error = error;
    }

    @Getter @Setter
    private String message;
    @Getter @Setter
    private String error;

    @Override
    public EventType getType() {
        return EventType.DEBUG_RULE_CHAIN;
    }
}
