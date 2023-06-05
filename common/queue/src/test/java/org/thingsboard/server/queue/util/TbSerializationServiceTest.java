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
package org.thingsboard.server.queue.util;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbSerializable;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.data.ComponentLifecycleMsgProto;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TbSerializationServiceTest {

    @Test
    void givenValidDevice_ok() {
        testSerialization(buildDevice(), Device.class);
    }

    @Test
    void givenValidDeviceProfile_ok() {
        testSerialization(buildDeviceProfile(), DeviceProfile.class);
    }

    @Test
    void givenValidTenantProfile_ok() {
        testSerialization(buildTenantProfile(), TenantProfile.class);
    }

    @Test
    void givenValidLifecycleMsg_ok() {
        testSerialization(buildLifecycleMsg(), ComponentLifecycleMsg.class);
    }

    @Test
    void givenValidTenant_ok() {
        testSerialization(buildTenant(), Tenant.class);
    }

    @Test
    void givenValidApiUsage_ok() {
        testSerialization(buildApiUsageState(), ApiUsageState.class);
    }

    private static <T extends TbSerializable> void testSerialization(T t, Class<T> clazz) {
        ProtoTbSerializationService service = new ProtoTbSerializationService();
        byte[] encoded = service.encode(t);
        Optional<T> decoded = service.decode(encoded, clazz);
        Assert.assertTrue(decoded.isPresent());
        Assert.assertEquals(t, decoded.get());
    }

    public Device buildDevice() {
        Device v = new Device();
        v.setId(new DeviceId(UUID.randomUUID()));
        v.setCreatedTime(System.currentTimeMillis());
        v.setTenantId(new TenantId(UUID.randomUUID()));
        v.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        v.setCustomerId(new CustomerId(UUID.randomUUID()));
        v.setName(StringUtils.randomAlphabetic(10));
        v.setLabel(StringUtils.randomAlphabetic(10));
        v.setType(StringUtils.randomAlphabetic(10));
        var dd = new DeviceData();
        dd.setConfiguration(new DefaultDeviceConfiguration());
        dd.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        v.setDeviceData(dd);
        v.setAdditionalInfo(JacksonUtil.newObjectNode().put("smth", StringUtils.randomAlphabetic(100)));
        v.setFirmwareId(new OtaPackageId(UUID.randomUUID()));
        //v.setSoftwareId(new OtaPackageId(UUID.randomUUID())); - check some fields are null;
        v.setExternalId(new DeviceId(UUID.randomUUID()));
        return v;
    }

    public DeviceProfile buildDeviceProfile() {
        DeviceProfile v = new DeviceProfile();
        v.setId(new DeviceProfileId(UUID.randomUUID()));
        v.setCreatedTime(System.currentTimeMillis());
        v.setTenantId(new TenantId(UUID.randomUUID()));
        v.setName(StringUtils.randomAlphabetic(10));
        var dd = new DeviceProfileData();
        dd.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        v.setProfileData(dd);
        v.setFirmwareId(new OtaPackageId(UUID.randomUUID()));
        //v.setSoftwareId(new OtaPackageId(UUID.randomUUID())); - check some fields are null;
        v.setExternalId(new DeviceProfileId(UUID.randomUUID()));
        return v;
    }

    public TenantProfile buildTenantProfile() {
        TenantProfile v = new TenantProfile();
        v.setId(new TenantProfileId(UUID.randomUUID()));
        v.setCreatedTime(System.currentTimeMillis());
        v.setName(StringUtils.randomAlphabetic(10));
        v.setProfileData(v.createDefaultTenantProfileData());
        return v;
    }

    public Tenant buildTenant() {
        Tenant v = new Tenant();
        v.setId(new TenantId(UUID.randomUUID()));
        v.setCreatedTime(System.currentTimeMillis());
        v.setTitle(StringUtils.randomAlphabetic(10));
        v.setTenantProfileId(new TenantProfileId(UUID.randomUUID()));
        return v;
    }

    public ApiUsageState buildApiUsageState() {
        ApiUsageState v = new ApiUsageState();
        v.setId(new ApiUsageStateId(UUID.randomUUID()));
        v.setEntityId(new TenantId(UUID.randomUUID()));
        v.setDbStorageState(ApiUsageStateValue.WARNING);
        v.setReExecState(ApiUsageStateValue.ENABLED);
        return v;
    }

    public ComponentLifecycleMsg buildLifecycleMsg() {
        return new ComponentLifecycleMsg(new TenantId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), ComponentLifecycleEvent.DELETED);
    }

}
