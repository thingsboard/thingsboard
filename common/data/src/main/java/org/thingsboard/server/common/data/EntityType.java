// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.thingsboard.server.common.data.StringUtils.removeStart;

public enum EntityType {

    TENANT(1),
    CUSTOMER(2, true),
    USER(3, "tb_user", true),
    DASHBOARD(4, true),
    ASSET(5, true),
    DEVICE(6, true),
    ALARM(7),
    ENTITY_GROUP(100) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName() {
            return "Entity Group";
        }
    },
    CONVERTER(101),
    INTEGRATION(102),
    RULE_CHAIN(11),
    RULE_NODE(12),
    SCHEDULER_EVENT(103),
    BLOB_ENTITY(104),
    REPORT_TEMPLATE(108),
    REPORT(109),
    ENTITY_VIEW(15, true) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName() {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE(16),
    WIDGET_TYPE(17),
    ROLE(105),
    GROUP_PERMISSION(106),
    TENANT_PROFILE(20),
    DEVICE_PROFILE(21),
    ASSET_PROFILE(22),
    API_USAGE_STATE(23),
    TB_RESOURCE(24, "resource"),
    OTA_PACKAGE(25),
    EDGE(26, true),
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
    SECRET(107),
    ADMIN_SETTINGS(42),
    AI_MODEL(43, "ai_model", false) {
        @Override
        public String getNormalName() {
            return "AI model";
        }
    },
    API_KEY(44),
    AGENT(110, true),
    AGENT_APPLICATION(111),
    AGENT_APP_EVENT(112),
    AGENT_APP_UNIT(113),
    AGENT_APP_PROFILE(114),
    AGENT_PROFILE(115),
    AGENT_BULK_ACTION(116);

    // TODO DON'T FORGET TO ADD NEW ENTITY TYPES TO THE END OF THE LIST NOT TO BREAK ORDINALS

    @Getter
    private final int protoNumber; // Corresponds to EntityTypeProto
    @Getter
    private final String tableName;
    @Getter
    private final boolean groupEntityType;
    @Getter
    private final String normalName = StringUtils.capitalize(removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

    public static final List<EntityType> GROUP_ENTITY_TYPES = EnumSet.allOf(EntityType.class).stream()
            .filter(EntityType::isGroupEntityType)
            .toList();

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName)
            .toList();

    private static final EntityType[] BY_PROTO;

    static {
        BY_PROTO = new EntityType[Arrays.stream(values()).mapToInt(EntityType::getProtoNumber).max().orElse(0) + 1];
        for (EntityType entityType : values()) {
            BY_PROTO[entityType.getProtoNumber()] = entityType;
        }
    }

    EntityType(int protoNumber) {
        this(protoNumber, false);
    }

    EntityType(int protoNumber, boolean groupEntityType) {
        this.protoNumber = protoNumber;
        this.groupEntityType = groupEntityType;
        this.tableName = name().toLowerCase();
    }

    EntityType(int protoNumber, String tableName) {
        this(protoNumber, tableName, false);
    }

    EntityType(int protoNumber, String tableName, boolean groupEntityType) {
        this.protoNumber = protoNumber;
        this.tableName = tableName;
        this.groupEntityType = groupEntityType;
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
        EntityType entityType = protoNumber < 0 || protoNumber >= BY_PROTO.length ? null : BY_PROTO[protoNumber];
        if (entityType == null) {
            throw new IllegalArgumentException("Invalid EntityType proto number " + protoNumber);
        }
        return entityType;
    }

}
