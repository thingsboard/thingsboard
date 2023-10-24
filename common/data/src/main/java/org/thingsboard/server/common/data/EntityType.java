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

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
public enum EntityType {
    TENANT(1),
    CUSTOMER(2),
    USER(3),
    DASHBOARD(4),
    ASSET(5),
    DEVICE(6),
    ALARM (7),
    RULE_CHAIN (10),
    RULE_NODE (11),

    ENTITY_VIEW (14) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName () {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE (15),
    WIDGET_TYPE (16),
    TENANT_PROFILE (19),
    DEVICE_PROFILE (20),
    ASSET_PROFILE (21),
    API_USAGE_STATE (22),
    TB_RESOURCE (23),
    OTA_PACKAGE (24),
    EDGE (25),
    RPC (26),
    QUEUE (27),
    NOTIFICATION_TARGET (28),
    NOTIFICATION_TEMPLATE (29),
    NOTIFICATION_REQUEST (30),
    NOTIFICATION (31),
    NOTIFICATION_RULE (32);

    @Getter
    private final int protoNumber; // Corresponds to EntityTypeProto

    private EntityType(int protoNumber) {
        this.protoNumber = protoNumber;
    }

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).collect(Collectors.toUnmodifiableList());

    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

}
