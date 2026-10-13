// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Verifies the ChirpStack v4 downlink-queue contract against a real server in Testcontainers:
 * fPort must be sent as a JSON integer (uint32), not a string.
 *
 * Exercises the same endpoint that {@link ChirpStackIntegration} hits — POST /api/devices/{eui}/queue —
 * so this test acts as the contract reference for the integration's request body shape.
 */
@Slf4j
class ChirpStackContainerTest {

    private static final String DEV_EUI = "0102030405060708";

    private static ChirpStackTestStack stack;
    private static ChirpStackTestClient cs;
    private static final RestTemplate http = new RestTemplate();

    @BeforeAll
    static void setUp() {
        stack = new ChirpStackTestStack();
        stack.start();

        cs = new ChirpStackTestClient(stack.getRestApiUrl(), stack.getApiToken());

        String tenantId = cs.createTenant("test-tenant");
        String applicationId = cs.createApplication(tenantId, "test-app");
        String deviceProfileId = cs.createDeviceProfile(tenantId, "test-profile");
        cs.createDevice(applicationId, deviceProfileId, DEV_EUI, "test-device");
    }

    @AfterAll
    static void tearDown() {
        if (stack != null) {
            stack.close();
        }
    }

    @Test
    void chirpstackAcceptsIntegerFPortAndStoresItInQueue() {
        cs.flushDeviceQueue(DEV_EUI);

        ObjectNode queueItem = JacksonUtil.newObjectNode();
        queueItem.put("devEui", DEV_EUI);
        queueItem.put("fPort", 80);
        queueItem.put("data", "Kj8=");
        queueItem.put("confirmed", false);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("queueItem", queueItem);

        ResponseEntity<String> resp = postQueueItem(body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode queue = cs.getDeviceQueue(DEV_EUI);
        JsonNode items = queue.get("result");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(1);
        JsonNode stored = items.get(0);
        assertThat(stored.get("fPort").isInt()).isTrue();
        assertThat(stored.get("fPort").asInt()).isEqualTo(80);
        assertThat(stored.get("data").asText()).isEqualTo("Kj8=");
    }

    /**
     * Documents observed behavior on ChirpStack 4.15: the REST gateway coerces a JSON-string
     * fPort to uint32 silently rather than returning 400. This means the long-standing
     * {@link ChirpStackIntegration#createBodyForParameter} call that writes fPort as a String
     * still works against current CS — the fix is forward-compatible hardening, not a
     * recovery from a server-side rejection.
     */
    @Test
    void chirpstackCoercesStringFPortToInteger() {
        cs.flushDeviceQueue(DEV_EUI);

        ObjectNode queueItem = JacksonUtil.newObjectNode();
        queueItem.put("devEui", DEV_EUI);
        queueItem.put("fPort", "80");
        queueItem.put("data", "Kj8=");
        queueItem.put("confirmed", false);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("queueItem", queueItem);

        ResponseEntity<String> resp = postQueueItem(body);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode stored = cs.getDeviceQueue(DEV_EUI).get("result").get(0);
        assertThat(stored.get("fPort").isInt()).isTrue();
        assertThat(stored.get("fPort").asInt()).isEqualTo(80);
    }

    @Test
    void chirpstackRejectsNonNumericFPortWithProtoError() {
        cs.flushDeviceQueue(DEV_EUI);

        ObjectNode queueItem = JacksonUtil.newObjectNode();
        queueItem.put("devEui", DEV_EUI);
        queueItem.put("fPort", "not-a-number");
        queueItem.put("data", "Kj8=");
        queueItem.put("confirmed", false);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("queueItem", queueItem);

        HttpClientErrorException error = catchThrowableOfType(() -> postQueueItem(body), HttpClientErrorException.class);
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Example body: {"code":3,"message":"proto: (line 1:51): invalid value for uint32 field fPort: \"not-a-number\"","details":[]}
        assertThat(error.getResponseBodyAsString())
                .contains("invalid value for uint32 field fPort")
                .contains("not-a-number");
    }

    @Test
    void chirpstackRejectsStringConfirmedWithProtoError() {
        cs.flushDeviceQueue(DEV_EUI);

        ObjectNode queueItem = JacksonUtil.newObjectNode();
        queueItem.put("devEui", DEV_EUI);
        queueItem.put("fPort", 80);
        queueItem.put("data", "Kj8=");
        queueItem.put("confirmed", "false"); // string, not bool — was the production bug
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("queueItem", queueItem);

        HttpClientErrorException error = catchThrowableOfType(() -> postQueueItem(body), HttpClientErrorException.class);
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Example body: {"code":3,"message":"proto: (line 1:80): invalid value for bool field confirmed: \"false\"","details":[]}
        assertThat(error.getResponseBodyAsString())
                .contains("invalid value for bool field confirmed")
                .contains("false");
    }

    @Test
    void chirpstackRejectsCs3WrapperKeyAgainstCs4Server() {
        cs.flushDeviceQueue(DEV_EUI);

        ObjectNode queueItem = JacksonUtil.newObjectNode();
        queueItem.put("devEui", DEV_EUI);
        queueItem.put("fPort", 80);
        queueItem.put("data", "Kj8=");
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("deviceQueueItem", queueItem); // CS v3 name, ignored by CS v4 -> fPort stays 0 -> validation fails

        HttpClientErrorException error = catchThrowableOfType(() -> postQueueItem(body), HttpClientErrorException.class);
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Example body: {"code":3,"message":"Validation error: FPort must be between 1 - 255","details":[]}
        assertThat(error.getResponseBodyAsString())
                .contains("FPort must be between 1 - 255");
    }

    private ResponseEntity<String> postQueueItem(JsonNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cs.getToken());
        return http.exchange(stack.getRestApiUrl() + "/api/devices/" + DEV_EUI + "/queue",
                HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

}
