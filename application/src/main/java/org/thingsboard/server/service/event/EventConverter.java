/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.event.*;

/**
 * 事件转换器
 *
 * @author baigod
 * @version : EventConverter.java, v 0.1 2024年05月27日 17:04 baigod Exp $
 */
public class EventConverter {
    /**
     * 事件转为json
     *
     * @param event
     * @return
     */
    public static JsonNode convert(Event event) {
        EventType eventType = event.getType();
        switch (eventType) {
            case ERROR:
                return convertErrorEvent(event);
            case LC_EVENT:
                return convertLcEvent(event);
            case STATS:
                return convertStatsEvent(event);
            case DEBUG_RULE_NODE:
                return convertRuleNodeEvent(event);
            case DEBUG_RULE_CHAIN:
                return convertRuleChainEvent(event);
            default:
                throw new RuntimeException(eventType + " support is not implemented!");
        }
    }

    public static JsonNode convertErrorEvent(Event event) {
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        setCommonEventFields(event, objectNode);

        ErrorEvent e = (ErrorEvent) event;
        objectNode.put("e_method", e.getMethod());
        objectNode.put("e_error", e.getError());

        return objectNode;
    }

    public static JsonNode convertLcEvent(Event event) {
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        setCommonEventFields(event, objectNode);

        LifecycleEvent e = (LifecycleEvent) event;
        objectNode.put("e_type", e.getLcEventType());
        objectNode.put("e_success", e.isSuccess());
        objectNode.put("e_error", e.getError());

        return objectNode;
    }

    public static JsonNode convertStatsEvent(Event event) {
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        setCommonEventFields(event, objectNode);

        StatisticsEvent e = (StatisticsEvent) event;
        objectNode.put("e_messages_processed", e.getMessagesProcessed());
        objectNode.put("e_errors_occurred", e.getErrorsOccurred());

        return objectNode;
    }

    public static JsonNode convertRuleNodeEvent(Event event) {
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        setCommonEventFields(event, objectNode);

        RuleNodeDebugEvent e = (RuleNodeDebugEvent) event;
        objectNode.put("e_type", e.getEventType());
        objectNode.put("e_entity_id", e.getEventEntity() != null ? e.getEventEntity().getId().toString() : null);
        objectNode.put("e_entity_type", e.getEventEntity() != null ? e.getEventEntity().getEntityType().name() : null);
        objectNode.put("e_msg_id", e.getMsgId().toString());
        objectNode.put("e_msg_type", e.getMsgType());
        objectNode.put("e_data_type", e.getDataType());
        objectNode.put("e_relation_type", e.getRelationType());
        objectNode.put("e_data", e.getData());
        objectNode.put("e_metadata", e.getMetadata());
        objectNode.put("e_error", e.getError());

        return objectNode;
    }

    public static JsonNode convertRuleChainEvent(Event event) {
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        setCommonEventFields(event, objectNode);

        RuleChainDebugEvent e = (RuleChainDebugEvent) event;
        objectNode.put("e_message", e.getMessage());
        objectNode.put("e_error", e.getError());

        return objectNode;
    }

    public static void setCommonEventFields(Event event, ObjectNode objectNode) {
        objectNode.put("event_type", event.getType().getTable());
        objectNode.put("id", event.getUuidId().toString());
        objectNode.put("tenant_id", event.getTenantId().toString());
        objectNode.put("ts", event.getCreatedTime());
        objectNode.put("entity_id", event.getEntityId().toString());
        objectNode.put("service_id", event.getServiceId());
    }
}
