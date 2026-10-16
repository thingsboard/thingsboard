// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import lombok.ToString;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.DeviceFields;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.edqs.DataPoint;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ToString(callSuper = true)
public class DeviceData extends ProfileAwareData<DeviceFields> {

    private final Map<Integer, DataPoint> clientAttrMap;
    private final Map<Integer, DataPoint> sharedAttrMap;

    public DeviceData(UUID entityId) {
        super(entityId);
        this.clientAttrMap = new ConcurrentHashMap<>();
        this.sharedAttrMap = new ConcurrentHashMap<>();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

    @Override
    public DataPoint getAttr(Integer keyId, EntityKeyType entityKeyType) {
        return switch (entityKeyType) {
            case ATTRIBUTE -> getAttributeDataPoint(keyId);
            case SERVER_ATTRIBUTE -> serverAttrMap.get(keyId);
            case CLIENT_ATTRIBUTE -> clientAttrMap.get(keyId);
            case SHARED_ATTRIBUTE -> sharedAttrMap.get(keyId);
            default -> throw new RuntimeException(entityKeyType + " not implemented");
        };
    }

    @Override
    public boolean putAttr(Integer keyId, AttributeScope scope, DataPoint value) {
        return switch (scope) {
            case SERVER_SCOPE -> serverAttrMap.put(keyId, value) == null;
            case CLIENT_SCOPE -> clientAttrMap.put(keyId, value) == null;
            case SHARED_SCOPE -> sharedAttrMap.put(keyId, value) == null;
        };
    }

    @Override
    public boolean removeAttr(Integer keyId, AttributeScope scope) {
        return switch (scope) {
            case SERVER_SCOPE -> serverAttrMap.remove(keyId) != null;
            case CLIENT_SCOPE -> clientAttrMap.remove(keyId) != null;
            case SHARED_SCOPE -> sharedAttrMap.remove(keyId) != null;
        };
    }

    private DataPoint getAttributeDataPoint(Integer keyId) {
        DataPoint dp = serverAttrMap.get(keyId);
        if (dp == null) {
            dp = sharedAttrMap.get(keyId);
            if (dp == null) {
                dp = clientAttrMap.get(keyId);
            }
        }
        return dp;
    }

}
