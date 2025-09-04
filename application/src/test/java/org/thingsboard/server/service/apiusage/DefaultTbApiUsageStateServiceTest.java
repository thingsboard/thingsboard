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
package org.thingsboard.server.service.apiusage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class DefaultTbApiUsageStateServiceTest extends AbstractControllerTest {

    @Autowired
    DefaultTbApiUsageStateService service;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    private TenantId tenantId;
    private Tenant savedTenant;

    private static final int MAX_ENABLE_VALUE = 5000;
    private static final long VALUE_WARNING = 4500L;
    private static final long VALUE_DISABLE = 5500L;
    private static final double WARN_THRESHOLD_VALUE = 0.8;

    @Before
    public void init() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = createTenantProfile();
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        savedTenant = saveTenant(tenant);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);
    }

    @Test
    public void testProcess_transitionFromWarningToDisabled() {
        TransportProtos.ToUsageStatsServiceMsg.Builder warningMsgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        warningMsgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                .setKey(ApiUsageRecordKey.STORAGE_DP_COUNT.name())
                .setValue(VALUE_WARNING)
                .build());

        TransportProtos.ToUsageStatsServiceMsg warningStatsMsg = warningMsgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> warningMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), warningStatsMsg);

        service.process(warningMsg, TbCallback.EMPTY);
        assertEquals(ApiUsageStateValue.WARNING, apiUsageStateService.findTenantApiUsageState(tenantId).getDbStorageState());

        TransportProtos.ToUsageStatsServiceMsg.Builder disableMsgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        disableMsgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                .setKey(ApiUsageRecordKey.STORAGE_DP_COUNT.name())
                .setValue(VALUE_DISABLE)
                .build());

        TransportProtos.ToUsageStatsServiceMsg disableStatsMsg = disableMsgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> disableMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), disableStatsMsg);

        service.process(disableMsg, TbCallback.EMPTY);
        assertEquals(ApiUsageStateValue.DISABLED, apiUsageStateService.findTenantApiUsageState(tenantId).getDbStorageState());
    }

    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Tenant Profile");
        tenantProfile.setDescription("Tenant Profile" + " Test");

        TenantProfileData tenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .warnThreshold(WARN_THRESHOLD_VALUE)
                .build();

        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        return tenantProfile;
    }

}