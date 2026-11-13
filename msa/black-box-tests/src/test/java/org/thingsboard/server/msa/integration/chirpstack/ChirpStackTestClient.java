// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.msa.ContainerTestSuite;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provisions a real ChirpStack v4 server (the {@code chirpstack-rest-api} service exposed by
 * {@link ContainerTestSuite}) over its REST API so an end-to-end uplink can be produced.
 *
 * <p>ChirpStack v4 has no REST login endpoint — the REST API authenticates with a Bearer
 * API-key JWT signed with the configured {@code [api] secret}. There is no API key out of the
 * box, so we seed one admin {@code api_key} row directly in ChirpStack's PostgreSQL (the table
 * only exists after ChirpStack applies its migrations) and mint a matching HS256 token.
 */
@Slf4j
public class ChirpStackTestClient {

    // Must match `secret` in chirpstack/configuration/chirpstack/chirpstack.toml.
    private static final String API_SECRET = "black-box-test-secret";
    // Fixed admin api_key id we seed and sign tokens for.
    private static final String API_KEY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String POSTGRES_SERVICE = "chirpstack-postgres_1";

    // In-cluster URL ChirpStack POSTs uplinks to. We hit haproxy on plain HTTP (port 80) because
    // (a) it routes /api/v1/integrations/* to the tb-integration-executor backend that actually
    // serves local integrations — tb-http-integration is only used for *remote* integrations
    // and would return 404 — and (b) HTTPS would force ChirpStack's HTTP client to validate the
    // self-signed cert haproxy serves on 443.
    private static final String TB_INTEGRATION_BASE_URL = "http://haproxy";

    // In-cluster URL the TB ChirpStack integration uses to enqueue downlinks. The TB side runs
    // inside the compose network and resolves the rest-api container by its service name.
    private static final String IN_NETWORK_REST_URL = "http://chirpstack-rest-api:8090";

    private final String restUrl;
    private final String token;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final SecureRandom random = new SecureRandom();

    public ChirpStackTestClient(String restUrl) {
        this.restUrl = restUrl;
        this.token = mintApiKeyToken();
    }

    /**
     * Creates a device profile, application, ABP-activated device, gateway and an HTTP
     * integration that forwards uplinks to ThingsBoard for the given integration routing key.
     */
    public ProvisionedDevice provision(String routingKey) {
        seedAdminApiKey();
        String tenantId = getDefaultTenantId();

        String deviceProfileId = createDeviceProfile(tenantId);
        String applicationId = createApplication(tenantId);

        byte[] devEui = randomBytes(8);
        byte[] gatewayId = randomBytes(8);
        byte[] devAddr = randomBytes(4);
        byte[] nwkSKey = randomBytes(16);
        byte[] appSKey = randomBytes(16);
        String devEuiHex = HexFormat.of().formatHex(devEui);
        String gatewayIdHex = HexFormat.of().formatHex(gatewayId);

        createDevice(applicationId, deviceProfileId, devEuiHex);
        activateDevice(devEuiHex, devAddr, nwkSKey, appSKey);
        createGateway(tenantId, gatewayIdHex);
        createHttpIntegration(applicationId, TB_INTEGRATION_BASE_URL
                + "/api/v1/integrations/chirpstack/" + routingKey);

        return new ProvisionedDevice(applicationId, devEui, devAddr, nwkSKey, appSKey, gatewayIdHex);
    }

    /** The admin API-key JWT used to authenticate REST calls — exposed so the TB integration can use it too. */
    public String getToken() {
        return token;
    }

    /** In-network base URL for the ChirpStack REST API — used by the TB integration's downlink call. */
    public static String getInNetworkRestUrl() {
        return IN_NETWORK_REST_URL;
    }

    /** Returns the current ChirpStack downlink queue for the device (GET /api/devices/{devEui}/queue). */
    public JsonNode getDeviceQueue(String devEuiHex) {
        return get("/api/devices/" + devEuiHex + "/queue");
    }

    public void deleteApplication(String applicationId) {
        try {
            delete("/api/applications/" + applicationId);
        } catch (Exception e) {
            log.warn("Failed to delete ChirpStack application {}", applicationId, e);
        }
    }

    private String createDeviceProfile(String tenantId) {
        ObjectNode dp = JacksonUtil.newObjectNode();
        dp.put("name", "bb-dp-" + HexFormat.of().formatHex(randomBytes(4)));
        dp.put("tenantId", tenantId);
        dp.put("region", "EU868");
        dp.put("macVersion", "LORAWAN_1_0_3");
        dp.put("regParamsRevision", "A");
        dp.put("supportsOtaa", false);
        dp.put("uplinkInterval", 3600);
        dp.put("adrAlgorithmId", "default");
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("deviceProfile", dp);
        return post("/api/device-profiles", body).get("id").asText();
    }

    private String createApplication(String tenantId) {
        ObjectNode app = JacksonUtil.newObjectNode();
        app.put("name", "bb-app-" + HexFormat.of().formatHex(randomBytes(4)));
        app.put("tenantId", tenantId);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("application", app);
        return post("/api/applications", body).get("id").asText();
    }

    private void createDevice(String applicationId, String deviceProfileId, String devEuiHex) {
        ObjectNode device = JacksonUtil.newObjectNode();
        device.put("devEui", devEuiHex);
        device.put("name", "bb-dev-" + devEuiHex);
        device.put("applicationId", applicationId);
        device.put("deviceProfileId", deviceProfileId);
        device.put("skipFcntCheck", true);
        device.put("isDisabled", false);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("device", device);
        post("/api/devices", body);
    }

    private void activateDevice(String devEuiHex, byte[] devAddr, byte[] nwkSKey, byte[] appSKey) {
        String nwkSKeyHex = HexFormat.of().formatHex(nwkSKey);
        ObjectNode activation = JacksonUtil.newObjectNode();
        activation.put("devEui", devEuiHex);
        activation.put("devAddr", HexFormat.of().formatHex(devAddr));
        activation.put("appSKey", HexFormat.of().formatHex(appSKey));
        // LoRaWAN 1.0.x uses a single network session key for all three key roles.
        activation.put("nwkSEncKey", nwkSKeyHex);
        activation.put("sNwkSIntKey", nwkSKeyHex);
        activation.put("fNwkSIntKey", nwkSKeyHex);
        activation.put("fCntUp", 0);
        activation.put("nFCntDown", 0);
        activation.put("aFCntDown", 0);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("deviceActivation", activation);
        post("/api/devices/" + devEuiHex + "/activate", body);
    }

    private void createGateway(String tenantId, String gatewayIdHex) {
        ObjectNode gateway = JacksonUtil.newObjectNode();
        gateway.put("gatewayId", gatewayIdHex);
        gateway.put("name", "bb-gw-" + gatewayIdHex);
        gateway.put("tenantId", tenantId);
        gateway.put("statsInterval", 30);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("gateway", gateway);
        post("/api/gateways", body);
    }

    private void createHttpIntegration(String applicationId, String eventEndpointUrl) {
        ObjectNode integration = JacksonUtil.newObjectNode();
        integration.put("applicationId", applicationId);
        integration.put("encoding", "JSON");
        integration.put("eventEndpointUrl", eventEndpointUrl);
        ObjectNode body = JacksonUtil.newObjectNode();
        body.set("integration", integration);
        post("/api/applications/" + applicationId + "/integrations/http", body);
    }

    private String getDefaultTenantId() {
        // Retries until ChirpStack has finished its migrations and the API answers our token.
        return Awaitility.await("ChirpStack REST API ready")
                .atMost(120, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        JsonNode result = get("/api/tenants?limit=1").get("result");
                        if (result != null && result.size() > 0) {
                            return result.get(0).get("id").asText();
                        }
                    } catch (Exception e) {
                        log.debug("ChirpStack not ready yet: {}", e.getMessage());
                    }
                    return null;
                }, java.util.Objects::nonNull);
    }

    // Inserts a fixed admin api_key row so our minted token validates. Idempotent and retried,
    // because the api_key table only exists once ChirpStack has applied its schema migrations.
    private void seedAdminApiKey() {
        ContainerState postgres = ContainerTestSuite.getInstance().getTestContainer()
                .getContainerByServiceName(POSTGRES_SERVICE)
                .orElseThrow(() -> new IllegalStateException("ChirpStack postgres container not found"));
        String sql = "insert into api_key(id, created_at, name, is_admin, is_read_only) "
                + "values ('" + API_KEY_ID + "', now(), 'blackbox', true, false) on conflict (id) do nothing;";
        AtomicReference<String> lastError = new AtomicReference<>("(no attempt recorded)");
        try {
            Awaitility.await("Seed ChirpStack admin api_key")
                    .atMost(120, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .until(() -> {
                        try {
                            Container.ExecResult result = postgres.execInContainer(
                                    "psql", "-U", "chirpstack", "-d", "chirpstack", "-v", "ON_ERROR_STOP=1", "-c", sql);
                            if (result.getExitCode() != 0) {
                                lastError.set("psql exit=" + result.getExitCode() + " stderr=" + result.getStderr().trim());
                                log.debug("api_key seed not ready: {}", lastError.get());
                                return false;
                            }
                            return true;
                        } catch (Exception e) {
                            lastError.set("exec failed: " + e);
                            log.debug("api_key seed exec failed: {}", e.getMessage());
                            return false;
                        }
                    });
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException("Failed to seed ChirpStack admin api_key. Last error: " + lastError.get()
                    + ". This usually means the 'chirpstack' server never applied its schema migrations "
                    + "(check the chirpstack container logs; it requires the pg_trgm/hstore extensions created by the "
                    + "postgres initdb script).", e);
        }
    }

    // ChirpStack api-key JWT: HS256, claims {aud,iss=chirpstack, sub=<api_key id>, typ=key}, no exp.
    private static String mintApiKeyToken() {
        try {
            String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = base64Url(("{\"aud\":\"chirpstack\",\"iss\":\"chirpstack\",\"sub\":\""
                    + API_KEY_ID + "\",\"typ\":\"key\"}").getBytes(StandardCharsets.UTF_8));
            String signingInput = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(API_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to mint ChirpStack API token", e);
        }
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private JsonNode post(String path, JsonNode body) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(restUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JacksonUtil.toString(body)))
                .build());
    }

    private JsonNode get(String path) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(restUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());
    }

    private void delete(String path) {
        send(HttpRequest.newBuilder()
                .uri(URI.create(restUrl + path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build());
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("ChirpStack API call " + request.method() + " " + request.uri()
                        + " failed: " + response.statusCode() + " - " + response.body());
            }
            return response.body().isEmpty() ? JacksonUtil.newObjectNode() : JacksonUtil.toJsonNode(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("ChirpStack API call failed: " + request.uri(), e);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /** Holds the secrets/identifiers needed to craft and publish an uplink for the device. */
    @Getter
    public static class ProvisionedDevice {
        private final String applicationId;
        private final byte[] devEui;
        private final byte[] devAddr;
        private final byte[] nwkSKey;
        private final byte[] appSKey;
        private final String gatewayId;

        public ProvisionedDevice(String applicationId, byte[] devEui, byte[] devAddr,
                                 byte[] nwkSKey, byte[] appSKey, String gatewayId) {
            this.applicationId = applicationId;
            this.devEui = devEui;
            this.devAddr = devAddr;
            this.nwkSKey = nwkSKey;
            this.appSKey = appSKey;
            this.gatewayId = gatewayId;
        }
    }
}
