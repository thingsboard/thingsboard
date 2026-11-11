// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;

/**
 * Thin REST client for the official chirpstack-rest-api gateway.
 * Provisions tenant/application/device-profile/device for tests.
 * Auth: login as admin/admin once, reuse the returned JWT as Bearer for all calls.
 */
@Slf4j
public class ChirpStackTestClient {

    private final RestTemplate http = new RestTemplate();
    private final String baseUrl;

    @Getter
    private final String token;

    public ChirpStackTestClient(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    public String createTenant(String name) {
        ObjectNode tenant = JacksonUtil.newObjectNode();
        tenant.put("name", name);
        tenant.put("canHaveGateways", true);
        tenant.put("maxGatewayCount", 0);
        tenant.put("maxDeviceCount", 0);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("tenant", tenant);
        return post("/api/tenants", body, token).get("id").asText();
    }

    public String createApplication(String tenantId, String name) {
        ObjectNode application = JacksonUtil.newObjectNode();
        application.put("name", name);
        application.put("description", "");
        application.put("tenantId", tenantId);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("application", application);
        return post("/api/applications", body, token).get("id").asText();
    }

    public String createDeviceProfile(String tenantId, String name) {
        ObjectNode profile = JacksonUtil.newObjectNode();
        profile.put("name", name);
        profile.put("tenantId", tenantId);
        profile.put("region", "EU868");
        profile.put("macVersion", "LORAWAN_1_0_3");
        profile.put("regParamsRevision", "A");
        profile.put("supportsOtaa", true);
        profile.put("uplinkInterval", 3600);
        profile.put("deviceStatusReqInterval", 1);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("deviceProfile", profile);
        return post("/api/device-profiles", body, token).get("id").asText();
    }

    public void createDevice(String applicationId, String deviceProfileId, String devEui, String name) {
        ObjectNode device = JacksonUtil.newObjectNode();
        device.put("devEui", devEui);
        device.put("name", name);
        device.put("description", "");
        device.put("applicationId", applicationId);
        device.put("deviceProfileId", deviceProfileId);
        device.put("skipFcntCheck", false);
        device.put("isDisabled", false);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("device", device);
        postRaw("/api/devices", body, token);
    }

    public JsonNode getDeviceQueue(String devEui) {
        return get("/api/devices/" + devEui + "/queue", token);
    }

    public void flushDeviceQueue(String devEui) {
        delete("/api/devices/" + devEui + "/queue", token);
    }

    private JsonNode post(String path, JsonNode body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<String> resp = http.exchange(baseUrl + path, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
        return JacksonUtil.toJsonNode(resp.getBody());
    }

    private void postRaw(String path, JsonNode body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        http.exchange(baseUrl + path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<String> resp = http.exchange(baseUrl + path, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        return JacksonUtil.toJsonNode(resp.getBody());
    }

    private void delete(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        http.exchange(baseUrl + path, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

}
