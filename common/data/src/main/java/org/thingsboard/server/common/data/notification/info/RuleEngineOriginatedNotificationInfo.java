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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleEngineOriginatedNotificationInfo implements RuleOriginatedNotificationInfo {

    private EntityId msgOriginator;
    private CustomerId msgCustomerId;
    private String msgType;
    private Map<String, String> msgMetadata;
    private Map<String, String> msgData;

    @Override
    public Map<String, String> getTemplateData() {
        Map<String, String> templateData = new HashMap<>();
        templateData.putAll(msgMetadata);
        templateData.putAll(msgData);
        templateData.put("originatorType", msgOriginator.getEntityType().getNormalName());
        templateData.put("originatorId", msgOriginator.getId().toString());
        templateData.put("msgType", msgType);
        templateData.put("customerId", msgCustomerId != null ? msgCustomerId.getId().toString() : "");
        return templateData;
    }

    @Override
    public EntityId getStateEntityId() {
        return msgOriginator;
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return msgCustomerId;
    }

}
