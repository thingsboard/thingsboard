package com.iot.thingmodel.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.admin.TenantAccountRepository;
import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.product.ProductItem;
import com.iot.thingmodel.product.ProductRepository;
import com.iot.thingmodel.tsl.TslAction;
import com.iot.thingmodel.tsl.TslDefine;
import com.iot.thingmodel.tsl.TslDefinition;
import com.iot.thingmodel.tsl.TslEvent;
import com.iot.thingmodel.tsl.TslProperty;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class RepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static DataSource dataSource;
    private static DSLContext dsl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProductRepository productRepository;
    private TenantAccountRepository accountRepository;

    @BeforeAll
    static void initDatabase() {
        dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        dsl = DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    @BeforeEach
    void cleanDatabase() {
        dsl.execute("TRUNCATE TABLE plat_product, plat_vendor_account RESTART IDENTITY CASCADE");
        productRepository = new ProductRepository(dsl, objectMapper);
        accountRepository = new TenantAccountRepository(dsl);
    }

    @Test
    void savesAndReadsDraft() throws Exception {
        UUID tenantId = UUID.randomUUID();
        JsonNode tsl = objectMapper.readTree("""
                {"productId":"P1","version":"1.0","properties":[]}
                """);

        ProductRepository.DraftResult result = productRepository.saveDraft(
                tenantId, "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl);

        assertEquals(Constants.Product.STATUS_DRAFT, result.tsl().status());
        assertEquals(tsl, result.tsl().tsl());
        assertEquals(tenantId, productRepository.findProduct("P1").orElseThrow().tenantId());
        assertEquals(tsl, productRepository.findLatestTsl("P1").orElseThrow().tsl());
        assertEquals(1, productRepository.listVersions("P1").size());
    }

    @Test
    void keepsLatestTslVersion() throws Exception {
        UUID tenantId = UUID.randomUUID();
        productRepository.saveDraft(
                tenantId, "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl("1.0"));
        productRepository.saveDraft(
                tenantId, "P1", "Alpha", "M1", "C1", null, "1.1", "sum-2", false, tsl("1.1"));

        assertEquals("1.1", productRepository.findLatestTsl("P1").orElseThrow().version());
        assertEquals(2, productRepository.listVersions("P1").size());
    }

    @Test
    void rejectsSameTslVersionWithDifferentContent() throws Exception {
        UUID tenantId = UUID.randomUUID();
        productRepository.saveDraft(
                tenantId, "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl("1.0"));

        ApiException ex = assertThrows(ApiException.class, () -> productRepository.saveDraft(
                tenantId, "P1", "Alpha", "M1", "C1", null, "1.0", "sum-2", false, tsl("1.0")));

        assertEquals(Constants.ErrorCode.TSL_VERSION_EXISTS, ex.getCode());
    }

    @Test
    void rejectsProductIdOwnedByAnotherTenant() throws Exception {
        productRepository.saveDraft(
                UUID.randomUUID(), "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl("1.0"));

        ApiException ex = assertThrows(ApiException.class, () -> productRepository.saveDraft(
                UUID.randomUUID(), "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl("1.0")));

        assertEquals(Constants.ErrorCode.PRODUCT_ID_CONFLICT, ex.getCode());
    }

    @Test
    void publishesCatalogAndUpsertsBinding() throws Exception {
        UUID tenantId = UUID.randomUUID();
        JsonNode tsl = tsl("1.0");
        productRepository.saveDraft(
                tenantId, "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl);

        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();
        productRepository.publish(
                "P1",
                Constants.Product.DEFAULT_TSL_VERSION,
                definition(tsl),
                Constants.Tsl.SCHEMA_VERSION,
                profileId,
                assetId,
                ruleChainId,
                Constants.Product.SYNC_RESULT_OK);

        ProductRepository.TslRow published = productRepository.findLatestTsl("P1").orElseThrow();
        assertEquals(Constants.Product.STATUS_PUBLISHED, published.status());
        assertNotNull(published.publishedAt());

        ProductRepository.BindingRow binding = productRepository.findBinding("P1", "1.0").orElseThrow();
        assertEquals(profileId, binding.profileId());
        assertEquals(assetId, binding.assetId());
        assertEquals(ruleChainId, binding.ruleChainId());

        List<ProductItem> items = productRepository.listProducts(tenantId, null);
        assertEquals(1, items.size());
        assertEquals(1, items.get(0).propertyCount());
        assertEquals(1, items.get(0).eventCount());
        assertEquals(1, items.get(0).actionCount());
        assertEquals(profileId.toString(), items.get(0).tbProfileId());

        UUID newProfileId = UUID.randomUUID();
        productRepository.publish(
                "P1",
                Constants.Product.DEFAULT_TSL_VERSION,
                definition(tsl),
                Constants.Tsl.SCHEMA_VERSION,
                newProfileId,
                assetId,
                ruleChainId,
                Constants.Product.SYNC_RESULT_OK);
        assertEquals(newProfileId, productRepository.findBinding("P1", "1.0").orElseThrow().profileId());
    }

    @Test
    void persistsRuleChainBindingDetails() throws Exception {
        UUID tenantId = UUID.randomUUID();
        JsonNode tsl = tsl(Constants.Product.DEFAULT_TSL_VERSION);
        productRepository.saveDraft(
                tenantId,
                "P1",
                "Alpha",
                "M1",
                "C1",
                null,
                Constants.Product.DEFAULT_TSL_VERSION,
                "sum-1",
                false,
                tsl);

        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();
        productRepository.publish(
                "P1",
                Constants.Product.DEFAULT_TSL_VERSION,
                definition(tsl),
                Constants.Tsl.SCHEMA_VERSION,
                profileId,
                assetId,
                ruleChainId,
                Constants.Product.SYNC_RESULT_OK);

        ProductRepository.BindingRow binding = productRepository
                .findBinding("P1", Constants.Product.DEFAULT_TSL_VERSION)
                .orElseThrow();

        assertEquals(profileId, binding.profileId());
        assertEquals(assetId, binding.assetId());
        assertEquals(ruleChainId, binding.ruleChainId());
        assertEquals(Constants.Tsl.SCHEMA_VERSION, binding.schemaVersion());
        assertEquals(Constants.Product.SYNC_RESULT_OK, binding.syncResult());
        assertNotNull(binding.syncedAt());
    }

    @Test
    void filtersProductsByTenantAndKeyword() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        productRepository.saveDraft(
                tenantA, "P1", "Alpha", "M1", "C1", null, "1.0", "sum-1", false, tsl("1.0"));
        productRepository.saveDraft(
                tenantA, "P2", "Beta", "M2", "C2", null, "1.0", "sum-2", false, tsl("1.0"));
        productRepository.saveDraft(
                tenantB, "P3", "Alpha", "M3", "C3", null, "1.0", "sum-3", false, tsl("1.0"));

        assertEquals(2, productRepository.listProducts(tenantA, null).size());
        List<ProductItem> filtered = productRepository.listProducts(tenantA, "alp");
        assertEquals(1, filtered.size());
        assertEquals("P1", filtered.get(0).productId());
        assertEquals(3, productRepository.listProducts(null, null).size());
    }

    @Test
    void upsertsTenantAccount() {
        UUID tenantId = UUID.randomUUID();

        accountRepository.upsert(tenantId, "vendor@example.com", "cipher-1");
        TenantAccountRepository.TenantAccountRow created = accountRepository.find(tenantId).orElseThrow();
        assertEquals("vendor@example.com", created.username());
        assertEquals("cipher-1", created.passwordCipher());
        assertTrue(created.enabled());

        accountRepository.upsert(tenantId, "vendor2@example.com", "cipher-2");
        TenantAccountRepository.TenantAccountRow updated = accountRepository.find(tenantId).orElseThrow();
        assertEquals("vendor2@example.com", updated.username());
        assertEquals("cipher-2", updated.passwordCipher());
    }

    private JsonNode tsl(String version) throws Exception {
        return objectMapper.readTree("""
                {"productId":"P1","version":"%s","properties":[]}
                """.formatted(version));
    }

    private TslDefinition definition(JsonNode raw) {
        TslDefine define = new TslDefine(
                Constants.Tsl.TYPE_INT, 0.0, 100.0, 0.0, 1.0, "rpm", null, null);
        TslProperty property = new TslProperty(
                "speed",
                "Speed",
                null,
                Constants.Tsl.MODE_READ,
                true,
                define,
                objectMapper.createObjectNode().put("register", "0x01"));
        TslEvent event = new TslEvent(
                "alarm", "Alarm", null, Constants.Tsl.EVENT_TYPE_INFO, List.of());
        TslAction action = new TslAction("reset", "Reset", null, List.of(), List.of());
        return new TslDefinition(
                Constants.Product.DEFAULT_TSL_VERSION,
                "P1",
                "C1",
                List.of(property),
                List.of(event),
                List.of(action),
                raw);
    }
}
