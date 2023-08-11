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
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
public enum EntityType {
    TENANT(EdgeEventType.TENANT),
    CUSTOMER(EdgeEventType.CUSTOMER),
    USER(EdgeEventType.USER),
    DASHBOARD(EdgeEventType.DASHBOARD),
    ASSET(EdgeEventType.ASSET),
    DEVICE(EdgeEventType.DEVICE),
    ALARM(EdgeEventType.ALARM),
    RULE_CHAIN(EdgeEventType.RULE_CHAIN),
    RULE_NODE(null),
    ENTITY_VIEW(EdgeEventType.ENTITY_VIEW) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName() {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE(EdgeEventType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EdgeEventType.WIDGET_TYPE),
    TENANT_PROFILE(EdgeEventType.TENANT_PROFILE),
    DEVICE_PROFILE(EdgeEventType.DEVICE_PROFILE),
    ASSET_PROFILE(EdgeEventType.ASSET_PROFILE),
    API_USAGE_STATE(null),
    TB_RESOURCE(null),
    OTA_PACKAGE(EdgeEventType.OTA_PACKAGE),
    EDGE(EdgeEventType.EDGE),
    RPC(null),
    QUEUE(EdgeEventType.QUEUE),
    NOTIFICATION_TARGET(null),
    NOTIFICATION_TEMPLATE(null),
    NOTIFICATION_REQUEST(null),
    NOTIFICATION(null),
    NOTIFICATION_RULE(null);

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).collect(Collectors.toUnmodifiableList());

    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

    @Getter
    private final EdgeEventType edgeEventType;

    EntityType(EdgeEventType edgeEventType) {
        this.edgeEventType = edgeEventType;
    }

}
