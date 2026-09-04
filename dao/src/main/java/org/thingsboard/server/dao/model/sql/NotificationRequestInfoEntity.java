// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestInfoEntity extends NotificationRequestEntity {

    private String templateName;
    private JsonNode templateConfig;

    public NotificationRequestInfoEntity(NotificationRequestEntity requestEntity, String templateName, Object templateConfig) {
        super(requestEntity);
        this.templateName = templateName;
        this.templateConfig = (JsonNode) templateConfig;
    }

    @Override
    public NotificationRequestInfo toData() {
        NotificationRequest request = super.toData();
        List<NotificationDeliveryMethod> deliveryMethods;

        if (request.getStats() != null) {
            Set<NotificationDeliveryMethod> methods = new HashSet<>(request.getStats().getSent().keySet());
            methods.addAll(request.getStats().getErrors().keySet());
            deliveryMethods = new ArrayList<>(methods);
        } else {
            NotificationTemplateConfig templateConfig = fromJson(this.templateConfig, NotificationTemplateConfig.class);
            if (templateConfig == null && request.getTemplate() != null) {
                templateConfig = request.getTemplate().getConfiguration();
            }
            if (templateConfig != null) {
                deliveryMethods = templateConfig.getDeliveryMethodsTemplates().entrySet().stream()
                        .filter(entry -> entry.getValue().isEnabled())
                        .map(Map.Entry::getKey).collect(Collectors.toList());
            } else {
                deliveryMethods = Collections.emptyList();
            }
        }
        return new NotificationRequestInfo(request, templateName, deliveryMethods);
    }

}
