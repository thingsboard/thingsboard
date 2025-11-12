/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public enum EntityType {

    TENANT(1),
    CUSTOMER(2),
    USER(3, "tb_user"),
    DASHBOARD(4),
    ASSET(5),
    DEVICE(6),
    ALARM(7),
    RULE_CHAIN(11),
    RULE_NODE(12),
    ENTITY_VIEW(15) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName() {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE(16),
    WIDGET_TYPE(17),
    TENANT_PROFILE(20),
    DEVICE_PROFILE(21),
    ASSET_PROFILE(22),
    API_USAGE_STATE(23),
    TB_RESOURCE(24, "resource"),
    OTA_PACKAGE(25),
    EDGE(26),
    RPC(27),
    QUEUE(28),
    NOTIFICATION_TARGET(29),
    NOTIFICATION_TEMPLATE(30),
    NOTIFICATION_REQUEST(31),
    NOTIFICATION(32),
    NOTIFICATION_RULE(33),
    QUEUE_STATS(34),
    OAUTH2_CLIENT(35),
    DOMAIN(36),
    MOBILE_APP(37),
    MOBILE_APP_BUNDLE(38),
    CALCULATED_FIELD(39),
    // CALCULATED_FIELD_LINK(40), - was removed in 4.3
    JOB(41),
    ADMIN_SETTINGS(42),
    AI_MODEL(43, "ai_model") {
        @Override
        public String getNormalName() {
            return "AI model";
        }
    };

    @Getter
    private final int protoNumber; // Corresponds to EntityTypeProto
    @Getter
    private final String tableName;
    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).toList();

    private static final EntityType[] BY_PROTO;

    static {
        BY_PROTO = new EntityType[Arrays.stream(values()).mapToInt(EntityType::getProtoNumber).max().orElse(0) + 1];
        for (EntityType entityType : values()) {
            BY_PROTO[entityType.getProtoNumber()] = entityType;
        }
    }

    EntityType(int protoNumber) {
        this.protoNumber = protoNumber;
        this.tableName = name().toLowerCase();
    }

    EntityType(int protoNumber, String tableName) {
        this.protoNumber = protoNumber;
        this.tableName = tableName;
    }

    public boolean isOneOf(EntityType... types) {
        if (types == null) {
            return false;
        }
        for (EntityType type : types) {
            if (this == type) {
                return true;
            }
        }
        return false;
    }

    public static EntityType forProtoNumber(int protoNumber) {
        if (protoNumber < 0 || protoNumber >= BY_PROTO.length) {
            throw new IllegalArgumentException("Invalid EntityType proto number " + protoNumber);
        }
        return BY_PROTO[protoNumber];
    }

}
