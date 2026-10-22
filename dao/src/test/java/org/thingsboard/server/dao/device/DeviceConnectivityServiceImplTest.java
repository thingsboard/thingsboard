// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceConnectivityServiceImplTest {

    private static final String BASE_URL = "http://192.168.1.10:8080";

    @Mock
    private AdminSettingsService adminSettingsService;
    @Mock
    private DeviceCredentialsService deviceCredentialsService;
    @Mock
    private DeviceProfileService deviceProfileService;
    @InjectMocks
    private DeviceConnectivityServiceImpl service;

    @Test
    void configuredHostIsReportedAsDeviceConnectivity() throws Exception {
        givenConnectivity("""
                {"mqtt": {"enabled": true, "host": "mqtt.example.com", "port": "1884"}}""");

        JsonNode mqtt = service.getConnectivityInfo(BASE_URL, DeviceConnectivityHostSource.REQUEST).get("mqtt");

        assertThat(mqtt.get("host").asText()).isEqualTo("mqtt.example.com");
        assertThat(mqtt.get("port").asText()).isEqualTo("1884");
        assertThat(mqtt.get("hostSource").asText()).isEqualTo("DEVICE_CONNECTIVITY");
    }

    @Test
    void blankHostTakesBaseUrlAndItsSource() throws Exception {
        givenConnectivity("""
                {"mqtt": {"enabled": true, "host": "", "port": "1883"}}""");

        JsonNode fromRequest = service.getConnectivityInfo(BASE_URL, DeviceConnectivityHostSource.REQUEST).get("mqtt");
        JsonNode fromBaseUrl = service.getConnectivityInfo(BASE_URL, DeviceConnectivityHostSource.BASE_URL).get("mqtt");

        assertThat(fromRequest.get("host").asText()).isEqualTo("192.168.1.10");
        assertThat(fromRequest.get("hostSource").asText()).isEqualTo("REQUEST");
        assertThat(fromBaseUrl.get("hostSource").asText()).isEqualTo("BASE_URL");
    }

    @Test
    void blankPortIsReplacedWithStandardPort() throws Exception {
        givenConnectivity("""
                {"http": {"enabled": true, "host": "", "port": ""},
                 "https": {"enabled": true, "host": "", "port": ""},
                 "mqtt": {"enabled": true, "host": "", "port": ""},
                 "mqtts": {"enabled": true, "host": "", "port": ""},
                 "coap": {"enabled": true, "host": "", "port": ""},
                 "coaps": {"enabled": true, "host": "", "port": ""}}""");

        JsonNode info = service.getConnectivityInfo(BASE_URL, DeviceConnectivityHostSource.REQUEST);

        assertThat(info.get("http").get("port").asText()).isEqualTo("80");
        assertThat(info.get("https").get("port").asText()).isEqualTo("443");
        assertThat(info.get("mqtt").get("port").asText()).isEqualTo("1883");
        assertThat(info.get("mqtts").get("port").asText()).isEqualTo("8883");
        assertThat(info.get("coap").get("port").asText()).isEqualTo("5683");
        assertThat(info.get("coaps").get("port").asText()).isEqualTo("5684");
    }

    @Test
    void disabledProtocolIsLeftOut() throws Exception {
        givenConnectivity("""
                {"mqtt": {"enabled": true, "host": "", "port": "1883"},
                 "coap": {"enabled": false, "host": "", "port": "5683"}}""");

        JsonNode info = service.getConnectivityInfo(BASE_URL, DeviceConnectivityHostSource.REQUEST);

        assertThat(info.has("mqtt")).isTrue();
        assertThat(info.has("coap")).isFalse();
    }

    private void givenConnectivity(String json) {
        AdminSettings settings = new AdminSettings();
        settings.setJsonValue(JacksonUtil.toJsonNode(json));
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "connectivity")).thenReturn(settings);
    }

}
