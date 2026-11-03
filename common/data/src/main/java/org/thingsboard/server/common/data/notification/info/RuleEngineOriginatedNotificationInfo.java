// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
