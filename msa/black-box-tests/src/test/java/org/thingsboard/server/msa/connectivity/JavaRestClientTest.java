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
package org.thingsboard.server.msa.connectivity;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.TestProperties;

import javax.net.ssl.SSLContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

public class JavaRestClientTest extends AbstractContainerTest {

    private RestClient restClient;

    @BeforeClass
    public void beforeClass() throws Exception {
        SSLContext ssl = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

        var tls = new DefaultClientTlsStrategy(
                ssl,
                HostnameVerificationPolicy.CLIENT,
                NoopHostnameVerifier.INSTANCE
        );

        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tls)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        RestTemplate rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        restClient = new RestClient(rt, TestProperties.getBaseUrl());
    }

    @BeforeMethod
    public void setUp() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testGetAlarmsV2() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.randomAlphabetic(5)));
        assertThat(device).isNotNull();

        String type = "High temp" + RandomStringUtils.randomAlphabetic(5);
        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();
        restClient.saveAlarm(alarm);

        // get /api/v2/alarm
        PageData<AlarmInfo> alarmsV2 = restClient.getAlarmsV2(device.getId(), null, null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(alarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> activeAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(activeAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> cleared = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(cleared.getData()).hasSize(0);

        PageData<AlarmInfo> activeAndClearedAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED, AlarmSearchStatus.ACTIVE), null, null, null, new TimePageLink(10, 0));
        assertThat(activeAndClearedAlarms.getData()).hasSize(1);

        // get /api/v2/alarms
        PageData<AlarmInfo> allAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allAlarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allClearedAlarmsV2.getData()).hasSize(0);

        // get /api/alarms
        PageData<AlarmInfo> allAlarms = restClient.getAllAlarms(AlarmSearchStatus.ACTIVE, null, new TimePageLink(10, 0), null);
        assertThat(allAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarms = restClient.getAllAlarms(AlarmSearchStatus.CLEARED, null, new TimePageLink(10, 0), null);
        assertThat(allClearedAlarms.getData()).hasSize(0);

    }
}
