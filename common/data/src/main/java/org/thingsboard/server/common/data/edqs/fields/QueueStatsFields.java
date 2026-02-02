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
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
public class QueueStatsFields extends AbstractEntityFields {

    private String queueName;
    private String serviceId;

    @Override
    public String getName() {
        return queueName + '_' + serviceId;
    }

    public QueueStatsFields(UUID id, long createdTime, UUID tenantId, String queueName, String serviceId) {
        super(id, createdTime, tenantId);
        this.queueName = queueName;
        this.serviceId = serviceId;
    }
}
