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
package org.thingsboard.server.common.mapping;

import com.google.protobuf.InvalidProtocolBufferException;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TbSerializable;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.data.ApiUsageStateProto;
import org.thingsboard.server.gen.data.ComponentLifecycleMsgProto;
import org.thingsboard.server.gen.data.DeviceProfileProto;
import org.thingsboard.server.gen.data.DeviceProto;
import org.thingsboard.server.gen.data.TenantProfileProto;
import org.thingsboard.server.gen.data.TenantProto;

import java.util.HashMap;
import java.util.Map;

public class TbSerializationRegistry {

    private static final ToProtoMapper toProto = ToProtoMapper.INSTANCE;
    private static final ToDataMapper toData = ToDataMapper.INSTANCE;

    private static final Map<String, TbSerializationMapping<? extends TbSerializable>> mapping = new HashMap<>();

    static {
        mapping.put(Device.class.getName(), new TbSerializationMapping<Device>() {

            @Override
            public byte[] toBytes(Device device) {
                return toProto.mapDevice(device).toByteArray();
            }

            @Override
            public Device fromBytes(byte[] data) throws InvalidProtocolBufferException {
                return toData.mapDevice(DeviceProto.parseFrom(data));
            }
        });
        mapping.put(DeviceProfile.class.getName(), new TbSerializationMapping<DeviceProfile>() {

            @Override
            public byte[] toBytes(DeviceProfile msg) {
                return toProto.mapDeviceProfile(msg).toByteArray();
            }

            @Override
            public DeviceProfile fromBytes(byte[] data) throws InvalidProtocolBufferException {
                return toData.mapDeviceProfile(DeviceProfileProto.parseFrom(data));
            }
        });

        mapping.put(Tenant.class.getName(), new TbSerializationMapping<Tenant>() {

            @Override
            public byte[] toBytes(Tenant msg) {
                return toProto.mapTenant(msg).toByteArray();
            }

            @Override
            public Tenant fromBytes(byte[] data) throws InvalidProtocolBufferException {
                return toData.mapTenant(TenantProto.parseFrom(data));
            }
        });
        mapping.put(TenantProfile.class.getName(), new TbSerializationMapping<TenantProfile>() {

            @Override
            public byte[] toBytes(TenantProfile msg) {
                return toProto.mapTenantProfile(msg).toByteArray();
            }

            @Override
            public TenantProfile fromBytes(byte[] data) throws InvalidProtocolBufferException {
                return toData.mapTenantProfile(TenantProfileProto.parseFrom(data));
            }
        });
        mapping.put(ApiUsageState.class.getName(), new TbSerializationMapping<ApiUsageState>() {

            @Override
            public byte[] toBytes(ApiUsageState msg) {
                return toProto.mapApiUsageState(msg).toByteArray();
            }

            @Override
            public ApiUsageState fromBytes(byte[] data) throws InvalidProtocolBufferException {
                return toData.mapApiUsageState(ApiUsageStateProto.parseFrom(data));
            }
        });
        mapping.put(ComponentLifecycleMsg.class.getName(), new TbSerializationMapping<ComponentLifecycleMsg>() {

            @Override
            public byte[] toBytes(ComponentLifecycleMsg msg) {
                return toProto.mapComponentLifecycleMsg(msg).toByteArray();
            }

            @Override
            public ComponentLifecycleMsg fromBytes(byte[] data) throws InvalidProtocolBufferException {
                return toData.mapComponentLifecycleMsg(ComponentLifecycleMsgProto.parseFrom(data));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends TbSerializable> TbSerializationMapping<T> get(Class<T> clazz) {
        return (TbSerializationMapping<T>) mapping.get(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T extends TbSerializable> TbSerializationMapping<T> get(T t) {
        return (TbSerializationMapping<T>) mapping.get(t.getClass().getName());
    }

}
