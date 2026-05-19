/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;

import java.util.concurrent.TimeUnit;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.report.urgent_interval=1",
})
@Slf4j
public class CoapTransportFeatureDisabledTest extends AbstractCoapIntegrationTest {

    private static final int MAX_TRANSPORT_MESSAGES = 10;
    private static final double WARN_THRESHOLD = 0.5;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    @Autowired
    private DefaultTransportService defaultTransportService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        DefaultTenantProfileConfiguration config =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        config.setMaxTransportMessages(MAX_TRANSPORT_MESSAGES);
        config.setWarnThreshold(WARN_THRESHOLD);
        doPost("/api/tenantProfile", tenantProfile);

        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Coap transport disable test device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        try {
            processAfterTest();
        } finally {
            try {
                loginSysAdmin();
                TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
                DefaultTenantProfileConfiguration config =
                        (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
                config.setMaxTransportMessages(0);
                doPost("/api/tenantProfile", tenantProfile);
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void testCoapObserveSessionClosedWhenTransportDisabled() {
        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        CoapTestCallback callback = new CoapTestCallback();
        CoapObserveRelation observeRelation = client.getObserveRelation(callback);

        Awaitility.await("await initial observe response")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callback.getResponseCode())
                        && callback.getObserve() != null);

        Assert.assertFalse("CoAP transport must hold at least one registered session after observe",
                defaultTransportService.sessions.isEmpty());

        for (int i = 0; i < MAX_TRANSPORT_MESSAGES + 5; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.TRANSPORT_MSG_COUNT);
        }

        Awaitility.await("transport state flips to DISABLED")
                .atMost(15, TimeUnit.SECONDS)
                .until(() -> apiUsageStateService.findTenantApiUsageState(tenantId).getTransportState() == ApiUsageStateValue.DISABLED);

        Awaitility.await("CoAP session is removed from DefaultTransportService")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> defaultTransportService.sessions.isEmpty());

        observeRelation.proactiveCancel();
    }

}
