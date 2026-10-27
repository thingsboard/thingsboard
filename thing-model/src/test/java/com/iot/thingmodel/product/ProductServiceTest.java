package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.sync.SyncResult;
import com.iot.thingmodel.sync.TbProfileSyncer;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tsl.CompactSchema;
import com.iot.thingmodel.tsl.TslParser;
import com.iot.thingmodel.tsl.TslValidator;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslParser parser = new TslParser(objectMapper);
    private final TslValidator validator = new TslValidator(parser);
    private final CompactSchema compactSchema = new CompactSchema(objectMapper);
    private final UUID tenantId = UUID.randomUUID();
    private final TenantScope tenantScope = new TenantScope(tenantId, false);

    private ProductRepository repository;
    private TbProfileSyncer syncer;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        syncer = mock(TbProfileSyncer.class);
        service = new ProductService(repository, parser, validator, compactSchema, syncer, objectMapper);
    }

    @Test
    void rejectsInvalidTslBeforePersisting() throws Exception {
        JsonNode invalid = load("invalid-tsl.json");

        ApiException ex = assertThrows(ApiException.class, () -> service.create(command(invalid)));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatus().value());
        assertEquals(Constants.ErrorCode.TSL_INVALID, ex.getCode());
        verifyNoInteractions(syncer);
        verify(repository, never()).saveDraft(any(), anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    void publishesAfterSuccessfulSync() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        when(repository.saveDraft(any(), anyString(), anyString(), anyString(), anyString(), any(),
                anyString(), anyString(), anyBoolean(), any())).thenReturn(draft(valid));
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();
        when(syncer.sync(any())).thenReturn(new SyncResult(profileId, assetId, ruleChainId, 700));

        ProductResponse response = service.create(command(valid));

        assertEquals(Constants.Product.STATUS_PUBLISHED, response.status());
        assertEquals(7, response.propertyCount());
        assertEquals(profileId.toString(), response.tbProfileId());
        assertEquals(assetId.toString(), response.tbAssetId());
        assertEquals(ruleChainId.toString(), response.tbRuleChainId());
        verify(repository).publish(
                eq("9HYN9PJNOY"),
                eq(Constants.Product.DEFAULT_TSL_VERSION),
                any(),
                eq(Constants.Tsl.SCHEMA_VERSION),
                eq(profileId),
                eq(assetId),
                eq(ruleChainId),
                eq(Constants.Product.SYNC_RESULT_OK));
    }

    @Test
    void syncsStoredProductAndPersistsRuleChainId() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        ProductRepository.DraftResult draft = draft(valid);
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.product()));
        when(repository.findLatestTsl("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.tsl()));
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();
        when(syncer.sync(any())).thenReturn(new SyncResult(profileId, assetId, ruleChainId, 700));

        ProductResponse response = service.sync(tenantScope, "9HYN9PJNOY");

        assertEquals(Constants.Product.STATUS_PUBLISHED, response.status());
        assertEquals(ruleChainId.toString(), response.tbRuleChainId());
        verify(repository).publish(
                eq("9HYN9PJNOY"),
                eq(draft.tsl().version()),
                any(),
                eq(Constants.Tsl.SCHEMA_VERSION),
                eq(profileId),
                eq(assetId),
                eq(ruleChainId),
                eq(Constants.Product.SYNC_RESULT_OK));
    }

    @Test
    void returnsLatestThingModel() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        ProductRepository.DraftResult draft = draft(valid);
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.product()));
        when(repository.findLatestTsl("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.tsl()));

        ThingModelView view = service.getThingModel(tenantScope, "9HYN9PJNOY");

        assertEquals("9HYN9PJNOY", view.productId());
        assertEquals(draft.tsl().version(), view.version());
        assertEquals(Constants.Product.STATUS_DRAFT, view.status());
        assertEquals(valid, view.tsl());
    }

    @Test
    void updateThingModelTriggersRuleChainSync() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        ProductRepository.DraftResult draft = draft(valid);
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.product()));
        when(repository.saveDraft(any(), anyString(), anyString(), anyString(), anyString(), any(),
                anyString(), anyString(), anyBoolean(), any())).thenReturn(draft);
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();
        when(syncer.sync(any())).thenReturn(new SyncResult(profileId, assetId, ruleChainId, 700));

        ProductResponse response = service.updateThingModel(
                tenantScope,
                "9HYN9PJNOY",
                new UpdateThingModelRequest(null, valid, false));

        assertEquals(Constants.Product.STATUS_PUBLISHED, response.status());
        assertEquals(ruleChainId.toString(), response.tbRuleChainId());
        verify(syncer).sync(any());
        verify(repository).publish(
                eq("9HYN9PJNOY"),
                eq(draft.tsl().version()),
                any(),
                eq(Constants.Tsl.SCHEMA_VERSION),
                eq(profileId),
                eq(assetId),
                eq(ruleChainId),
                eq(Constants.Product.SYNC_RESULT_OK));
    }

    @Test
    void keepsDraftWhenSyncFails() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        when(repository.saveDraft(any(), anyString(), anyString(), anyString(), anyString(), any(),
                anyString(), anyString(), anyBoolean(), any())).thenReturn(draft(valid));
        when(syncer.sync(any())).thenThrow(new TbSyncException(
                "boom", HttpStatus.INTERNAL_SERVER_ERROR.value(), "boom"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(command(valid)));

        assertEquals(HttpStatus.BAD_GATEWAY.value(), ex.getStatus().value());
        assertEquals(
                String.format(Constants.Api.PRODUCT_SYNC_RETRY_PATH, "9HYN9PJNOY"),
                ex.getRetry());
        verify(repository, never()).publish(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void hidesProductsFromAnotherTenant() {
        UUID otherTenant = UUID.randomUUID();
        ProductRepository.ProductRow product = new ProductRepository.ProductRow(
                otherTenant,
                "9HYN9PJNOY",
                "WiFi诊断终端",
                "9HYN9PJNOY",
                "1",
                null,
                Constants.Product.AUTH_TYPE_ACCESS_TOKEN,
                Constants.Product.PROTOCOL_MQTT,
                Instant.now(),
                Instant.now());
        when(repository.findProduct("9HYN9PJNOY")).thenReturn(java.util.Optional.of(product));

        ApiException ex = assertThrows(ApiException.class, () -> service.get(tenantScope, "9HYN9PJNOY"));

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getStatus().value());
        assertEquals(Constants.ErrorCode.PRODUCT_NOT_FOUND, ex.getCode());
    }

    @Test
    void listsProductsForTenant() {
        when(repository.listProducts(tenantId, "wifi")).thenReturn(List.of());

        List<ProductItem> result = service.list(tenantScope, "wifi");

        assertTrue(result.isEmpty());
        verify(repository).listProducts(tenantId, "wifi");
    }

    @Test
    void listsAllProductsForSystemAdmin() {
        TenantScope systemAdmin = new TenantScope(null, true);
        when(repository.listProducts(null, null)).thenReturn(List.of());

        List<ProductItem> result = service.list(systemAdmin, null);

        assertTrue(result.isEmpty());
        verify(repository).listProducts(null, null);
    }

    @Test
    void rejectsListWithoutTenant() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.list(new TenantScope(null, false), null));

        assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus().value());
    }

    @Test
    void returnsCatalog() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        ProductRepository.TslRow tsl = draft(valid).tsl();
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft(valid).product()));
        when(repository.findLatestTsl("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(tsl));

        CatalogView catalog = service.catalog(tenantScope, "9HYN9PJNOY");

        assertEquals("9HYN9PJNOY", catalog.productId());
        assertEquals(Constants.Product.DEFAULT_TSL_VERSION, catalog.version());
        assertEquals(7, catalog.properties().size());
    }

    @Test
    void validatesThingModelWithoutPersisting() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.empty());
        com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.createObjectNode();
        body.set("tsl", valid);

        ValidationResponse response = service.validate(tenantScope, "9HYN9PJNOY", body);

        assertTrue(response.valid());
        verify(repository, never()).saveDraft(
                any(), anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    void reportsInvalidThingModel() throws Exception {
        JsonNode invalid = load("invalid-tsl.json");
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.empty());

        ValidationResponse response = service.validate(tenantScope, "9HYN9PJNOY", invalid);

        assertFalse(response.valid());
        assertFalse(response.errors().isEmpty());
    }

    @Test
    void rejectsProductIdMismatch() throws Exception {
        CreateProductCommand command = new CreateProductCommand(
                tenantId, "OTHER", "Name", "Model", "1",
                Constants.Product.DEFAULT_TSL_VERSION, load("valid-tsl.json"), false);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(command));

        assertEquals(Constants.ErrorCode.PRODUCT_ID_MISMATCH, ex.getCode());
        verifyNoInteractions(syncer);
    }

    @Test
    void rejectsVersionMismatch() throws Exception {
        CreateProductCommand command = new CreateProductCommand(
                tenantId, "9HYN9PJNOY", "Name", "Model", "1",
                "2.0", load("valid-tsl.json"), false);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(command));

        assertEquals(Constants.ErrorCode.VERSION_MISMATCH, ex.getCode());
        verifyNoInteractions(syncer);
    }

    @Test
    void returnsProductDetailWithBinding() throws Exception {
        JsonNode valid = load("valid-tsl.json");
        ProductRepository.DraftResult draft = draft(valid);
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();
        ProductRepository.BindingRow binding = new ProductRepository.BindingRow(
                profileId, assetId, ruleChainId, Constants.Tsl.SCHEMA_VERSION,
                Constants.Product.SYNC_RESULT_OK, Instant.now());
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.product()));
        when(repository.findLatestTsl("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft.tsl()));
        when(repository.findBinding(
                "9HYN9PJNOY", Constants.Product.DEFAULT_TSL_VERSION))
                .thenReturn(java.util.Optional.of(binding));

        ProductDetail detail = service.get(tenantScope, "9HYN9PJNOY");

        assertEquals(profileId.toString(), detail.product().tbProfileId());
        assertEquals(assetId.toString(), detail.product().tbAssetId());
        assertEquals(ruleChainId.toString(), detail.product().tbRuleChainId());
        assertEquals(7, detail.catalog().properties().size());
        assertNotNull(detail.versions());
    }

    @Test
    void rejectsCatalogWhenThingModelIsMissing() throws Exception {
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(draft(load("valid-tsl.json")).product()));
        when(repository.findLatestTsl("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.catalog(tenantScope, "9HYN9PJNOY"));

        assertEquals(Constants.ErrorCode.TSL_NOT_FOUND, ex.getCode());
    }

    @Test
    void rejectsSyncWhenProductIsMissing() {
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sync(tenantScope, "9HYN9PJNOY"));

        assertEquals(Constants.ErrorCode.PRODUCT_NOT_FOUND, ex.getCode());
    }

    @Test
    void rejectsUpdateThingModelForAnotherTenant() throws Exception {
        ProductRepository.ProductRow product = new ProductRepository.ProductRow(
                UUID.randomUUID(),
                "9HYN9PJNOY",
                "Other",
                "Other",
                "1",
                null,
                Constants.Product.AUTH_TYPE_ACCESS_TOKEN,
                Constants.Product.PROTOCOL_MQTT,
                Instant.now(),
                Instant.now());
        when(repository.findProduct("9HYN9PJNOY"))
                .thenReturn(java.util.Optional.of(product));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateThingModel(
                        tenantScope,
                        "9HYN9PJNOY",
                        new UpdateThingModelRequest(
                                Constants.Product.DEFAULT_TSL_VERSION,
                                load("valid-tsl.json"),
                                false)));

        assertEquals(Constants.ErrorCode.PRODUCT_NOT_FOUND, ex.getCode());
    }

    private CreateProductCommand command(JsonNode tsl) {
        return new CreateProductCommand(
                tenantId, "9HYN9PJNOY", "WiFi诊断终端", "9HYN9PJNOY", "1", Constants.Product.DEFAULT_TSL_VERSION, tsl, false);
    }

    private ProductRepository.DraftResult draft(JsonNode tsl) {
        ProductRepository.ProductRow product = new ProductRepository.ProductRow(
                tenantId,
                "9HYN9PJNOY",
                "WiFi诊断终端",
                "9HYN9PJNOY",
                "1",
                null,
                Constants.Product.AUTH_TYPE_ACCESS_TOKEN,
                Constants.Product.PROTOCOL_MQTT,
                Instant.now(),
                Instant.now());
        ProductRepository.TslRow row = new ProductRepository.TslRow(
                1L,
                tenantId,
                "9HYN9PJNOY",
                Constants.Product.DEFAULT_TSL_VERSION, "checksum",
                tsl,
                false,
                Constants.Product.STATUS_DRAFT,
                null,
                Instant.now());
        return new ProductRepository.DraftResult(product, row);
    }

    private JsonNode load(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/" + name)) {
            return objectMapper.readTree(input);
        }
    }
}
