// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.rule.engine;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class DeviceAttributesEventNotificationMsg implements ToDeviceActorNotificationMsg {

    @Serial
    private static final long serialVersionUID = 2422071590415277039L;

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final Set<AttributeKey> deletedKeys;
    private final String scope;
    private final List<AttributeKvEntry> values;
    private final boolean deleted;

    public static DeviceAttributesEventNotificationMsg onUpdate(TenantId tenantId, DeviceId deviceId, String scope, List<AttributeKvEntry> values) {
        return new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, scope, values, false);
    }

    public static DeviceAttributesEventNotificationMsg onDelete(TenantId tenantId, DeviceId deviceId, String scope, List<String> keys) {
        Set<AttributeKey> keysToNotify = new HashSet<>();
        keys.forEach(key -> keysToNotify.add(new AttributeKey(scope, key)));
        return new DeviceAttributesEventNotificationMsg(tenantId, deviceId, keysToNotify, null, null, true);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG;
    }

}
