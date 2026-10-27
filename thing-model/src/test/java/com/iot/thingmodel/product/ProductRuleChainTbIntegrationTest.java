package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.support.RuleChainAssertions;
import com.iot.thingmodel.support.TbRestTestClient;
import com.iot.thingmodel.support.TslFixtures;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRuleChainTbIntegrationTest {

    private static final String TB_URL = stripTrailingSlash(
            env("TB_URL", "http://localhost:18080"));
    private static final String TB_USERNAME = env("TB_USERNAME", "tenant@thingsboard.org");
    private static final String TB_PASSWORD = env("TB_PASSWORD", "tenant");
    private static final String TB_SYSADMIN_USERNAME =
            env("TB_SYSADMIN_USERNAME", "sysadmin@thingsboard.org");
    private static final String TB_SYSADMIN_PASSWORD = env("TB_SYSADMIN_PASSWORD", "sysadmin");
    private static final String SECRET_KEY =
            Base64.getEncoder().encodeToString(new byte[32]);
    private static final String PRODUCT_ID = "E2E_RULE_CHAIN";
    private static final String PRODUCT_NAME = "E2E Rule Chain " + PRODUCT_ID;
    private static final String PROFILE_NAME = PRODUCT_NAME + " " + PRODUCT_ID;
    private static final String CATEGORY_ID = "1";
    private static final String TSL_VERSION = "1.0";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("thingmodel")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.schema", () -> "public");
        registry.add("spring.flyway.default-schema", () -> "public");
        registry.add("spring.flyway.schemas", () -> "public");
        registry.add("spring.flyway.init-sql", () -> "SET search_path TO public");
        registry.add("thingmodel.database.expected-name", () -> "thingmodel");
        registry.add("thingmodel.database.expected-schema", () -> "");
        registry.add("thingmodel.tb.base-url", () -> TB_URL);
        registry.add("thingmodel.auth.cache-seconds", () -> 0);
        registry.add("thingmodel.security.secret-key", () -> SECRET_KEY);
        registry.add("thingmodel.rulechain.enabled", () -> true);
        registry.add("thingmodel.rulechain.kafka-bootstrap", () -> "localhost:9092");
        registry.add("thingmodel.rulechain.topic-telemetry",
                () -> Constants.Defaults.KAFKA_TOPIC_TELEMETRY);
        registry.add("thingmodel.rulechain.topic-event",
                () -> Constants.Defaults.KAFKA_TOPIC_EVENT);
        registry.add("thingmodel.rulechain.topic-invalid",
                () -> Constants.Defaults.KAFKA_TOPIC_INVALID);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private TbRestTestClient api;
    private String tenantToken;
    private String tenantId;
    private String ruleChainId;
    private String profileId;
    private String assetId;

    @BeforeEach
    void setUp() {
        api = new TbRestTestClient(rest, objectMapper, TB_URL);
        tenantToken = api.login(TB_USERNAME, TB_PASSWORD);
        tenantId = api.tenantId(tenantToken);
    }

    @Test
    void createsProductAndProvisionsRuleChainInThingsBoard() throws Exception {
        resetE2eEntities();
        ensureVendorAccount();

        String productId = PRODUCT_ID;
        JsonNode tsl = TslFixtures.eventTsl(objectMapper);
        ((ObjectNode) tsl.path("profile")).put("ProductId", productId);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("productId", productId);
        request.put("name", PRODUCT_NAME);
        request.put("model", productId);
        request.put("categoryId", CATEGORY_ID);
        request.put("tslVersion", TSL_VERSION);
        request.set("tsl", tsl);
        request.put("strictUnknown", false);

        JsonNode response = api.post(thingModelUrl() + "/api/products", tenantToken, request);
        assertEquals(Constants.Product.STATUS_PUBLISHED, response.path("status").asText());
        assertEquals(1, response.path("propertyCount").asInt());
        assertEquals(1, response.path("eventCount").asInt());
        ruleChainId = api.required(response, "tbRuleChainId");
        profileId = api.required(response, "tbProfileId");
        assetId = api.required(response, "tbAssetId");
        System.out.printf(
                "Retained E2E product=%s ruleChain=%s profile=%s asset=%s%n",
                productId, ruleChainId, profileId, assetId);

        assertProductBinding(productId);
        assertDeviceProfile(productId);

        JsonNode syncResponse = api.post(
                thingModelUrl() + "/api/products/" + productId + "/sync", tenantToken, null);
        assertEquals(ruleChainId, api.required(syncResponse, "tbRuleChainId"));
        assertEquals(profileId, api.required(syncResponse, "tbProfileId"));
        assertEquals(assetId, api.required(syncResponse, "tbAssetId"));

        assertProductBinding(productId);
        assertDeviceProfile(productId);
        assertAsset(productId);

        JsonNode chain = api.get(TB_URL + "/api/ruleChain/" + ruleChainId, tenantToken);
        assertEquals(productId, chain.path("name").asText());
        assertEquals(Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE, chain.path("type").asText());
        assertFalse(chain.path("root").asBoolean(true));

        JsonNode metadata = api.get(
                TB_URL + "/api/ruleChain/" + ruleChainId + "/metadata", tenantToken);
        RuleChainAssertions.assertMetadata(metadata, productId, "overSpeed");

        assertRuleChainUniqueness(productId);
    }

    private void assertProductBinding(String productId) {
        JsonNode detail = api.get(thingModelUrl() + "/api/products/" + productId, tenantToken);
        JsonNode product = detail.path("product");
        assertEquals(productId, product.path("productId").asText());
        assertEquals(PRODUCT_NAME, product.path("name").asText());
        assertEquals(Constants.Product.STATUS_PUBLISHED, product.path("status").asText());
        assertEquals(TSL_VERSION, product.path("tslVersion").asText());
        assertEquals(profileId, product.path("tbProfileId").asText());
        assertEquals(assetId, product.path("tbAssetId").asText());
        assertEquals(ruleChainId, product.path("tbRuleChainId").asText());
        assertEquals(1, product.path("propertyCount").asInt());
        assertEquals(1, product.path("eventCount").asInt());
        assertEquals(0, product.path("actionCount").asInt());

        JsonNode versions = detail.path("versions");
        assertEquals(1, versions.size());
        assertEquals(TSL_VERSION, versions.get(0).path("version").asText());
        assertEquals(Constants.Product.STATUS_PUBLISHED,
                versions.get(0).path("status").asText());

        JsonNode catalog = detail.path("catalog");
        assertEquals(productId, catalog.path("productId").asText());
        assertEquals(TSL_VERSION, catalog.path("version").asText());
        assertEquals(1, catalog.path("properties").size());
        assertEquals(1, catalog.path("events").size());
        assertEquals(0, catalog.path("actions").size());
    }

    private void assertDeviceProfile(String productId) {
        JsonNode profile = api.get(TB_URL + "/api/deviceProfile/" + profileId, tenantToken);
        assertEquals(PROFILE_NAME, profile.path("name").asText());
        assertEquals(
                String.format(Constants.Product.PROFILE_DESCRIPTION_FORMAT,
                        productId, CATEGORY_ID, TSL_VERSION, 1, 1, 0),
                profile.path("description").asText());
        assertEquals("DEFAULT", profile.path("type").asText());
        assertEquals("DEFAULT", profile.path("transportType").asText());
        assertEquals("DISABLED", profile.path("provisionType").asText());
        assertEquals(ruleChainId, profile.path("defaultRuleChainId").path("id").asText());
    }

    private void assertAsset(String productId) {
        JsonNode asset = api.get(TB_URL + "/api/asset/" + assetId, tenantToken);
        assertEquals(Constants.Product.ASSET_NAME_PREFIX + productId,
                asset.path("name").asText());

        JsonNode attributes = api.get(
                TB_URL + "/api/plugins/telemetry/ASSET/" + assetId
                        + "/values/attributes/SERVER_SCOPE",
                tenantToken);
        assertTrue(containsAttribute(attributes, Constants.Product.ATTRIBUTE_PRODUCT_ID));
        assertTrue(containsAttribute(attributes, Constants.Product.ATTRIBUTE_TSL_VERSION));
        assertTrue(containsAttribute(attributes, Constants.Product.ATTRIBUTE_TSL_SCHEMA));
        assertTrue(containsAttribute(attributes, Constants.Product.ATTRIBUTE_TSL_JSON));
    }

    private boolean containsAttribute(JsonNode attributes, String key) {
        for (JsonNode attribute : attributes) {
            if (key.equals(attribute.path("key").asText())) {
                return true;
            }
        }
        return false;
    }

    private void assertRuleChainUniqueness(String productId) {
        String searchUrl = TB_URL
                + "/api/ruleChains?pageSize=100&page=0&type=CORE"
                + "&sortProperty=name&sortOrder=ASC&textSearch="
                + URLEncoder.encode(productId, StandardCharsets.UTF_8);
        JsonNode page = api.get(searchUrl, tenantToken);
        int exact = 0;
        for (JsonNode item : page.path("data")) {
            if (productId.equals(item.path("name").asText())) {
                exact++;
            }
        }
        assertEquals(1, exact);

        String legacyName = Constants.RuleChain.LEGACY_NAME_PREFIX
                + productId + Constants.RuleChain.LEGACY_NAME_SUFFIX;
        String legacyUrl = TB_URL
                + "/api/ruleChains?pageSize=100&page=0&type=CORE"
                + "&sortProperty=name&sortOrder=ASC&textSearch="
                + URLEncoder.encode(legacyName, StandardCharsets.UTF_8);
        JsonNode legacyPage = api.getOrNull(legacyUrl, tenantToken);
        if (legacyPage != null) {
            for (JsonNode item : legacyPage.path("data")) {
                assertFalse(legacyName.equals(item.path("name").asText()));
            }
        }
    }

    private void resetE2eEntities() {
        deleteDeviceProfilesByName(PROFILE_NAME);
        deleteRuleChainsByName(PRODUCT_ID);
        deleteRuleChainsByName(Constants.RuleChain.LEGACY_NAME_PREFIX
                + PRODUCT_ID + Constants.RuleChain.LEGACY_NAME_SUFFIX);
    }

    private void deleteDeviceProfilesByName(String name) {
        String url = TB_URL
                + "/api/deviceProfiles?pageSize=100&page=0"
                + "&sortProperty=name&sortOrder=ASC&textSearch="
                + URLEncoder.encode(name, StandardCharsets.UTF_8);
        JsonNode page = api.getOrNull(url, tenantToken);
        if (page == null) {
            return;
        }
        for (JsonNode item : page.path("data")) {
            if (name.equals(item.path("name").asText())) {
                api.delete(TB_URL + "/api/deviceProfile/"
                        + item.path("id").path("id").asText(), tenantToken);
            }
        }
    }

    private void deleteRuleChainsByName(String name) {
        String url = TB_URL
                + "/api/ruleChains?pageSize=100&page=0&type=CORE"
                + "&sortProperty=name&sortOrder=ASC&textSearch="
                + URLEncoder.encode(name, StandardCharsets.UTF_8);
        JsonNode page = api.getOrNull(url, tenantToken);
        if (page == null) {
            return;
        }
        for (JsonNode item : page.path("data")) {
            if (name.equals(item.path("name").asText())) {
                api.delete(TB_URL + "/api/ruleChain/"
                        + item.path("id").path("id").asText(), tenantToken);
            }
        }
    }

    private void ensureVendorAccount() {
        String url = thingModelUrl() + "/api/admin/tenants/" + tenantId + "/account";
        String sysadminToken = api.login(TB_SYSADMIN_USERNAME, TB_SYSADMIN_PASSWORD);
        JsonNode existing = api.getOrNull(url, sysadminToken);
        if (existing != null) {
            return;
        }
        ObjectNode account = objectMapper.createObjectNode();
        account.put("username", TB_USERNAME);
        account.put("password", TB_PASSWORD);
        api.post(url, sysadminToken, account);
    }

    private String thingModelUrl() {
        return "http://localhost:" + port;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
