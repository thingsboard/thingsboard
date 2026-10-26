package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.config.ThingModelProperties;
import com.iot.thingmodel.sync.ProfileGenerator;
import com.iot.thingmodel.sync.TbProfileSyncer;
import com.iot.thingmodel.sync.TbRuleChainProvisioner;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import com.iot.thingmodel.tsl.CompactSchema;
import com.iot.thingmodel.tsl.TslParser;
import com.iot.thingmodel.tsl.TslValidator;
import java.io.InputStream;
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
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.api.ThingsboardApi;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AssetId;
import org.thingsboard.client.model.AssetProfileId;
import org.thingsboard.client.model.AssetProfileInfo;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.DeviceProfileId;
import org.thingsboard.client.model.PageDataDeviceProfile;
import org.thingsboard.client.model.PageDataRuleChain;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleChainType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class ProductRuleChainSyncIntegrationTest {

    private static final String PRODUCT_ID = "9HYN9PJNOY";
    private static final String PRODUCT_NAME = "WiFi诊断终端";
    private static final String PROFILE_NAME = PRODUCT_NAME + " " + PRODUCT_ID;
    private static final String ASSET_NAME = Constants.Product.ASSET_NAME_PREFIX + PRODUCT_ID;

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static DataSource dataSource;
    private static DSLContext dsl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslParser parser = new TslParser(objectMapper);
    private final TslValidator validator = new TslValidator(parser);
    private final CompactSchema compactSchema = new CompactSchema(objectMapper);
    private final ProfileGenerator profileGenerator = new ProfileGenerator();

    private UUID tenantId;
    private TenantScope tenantScope;
    private ThingsboardClientProvider clientProvider;
    private ThingsboardClient client;
    private ThingModelProperties properties;
    private ProductRepository productRepository;
    private ProductService productService;

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
    void setUp() {
        dsl.execute("TRUNCATE TABLE plat_product, plat_vendor_account RESTART IDENTITY CASCADE");
        tenantId = UUID.randomUUID();
        tenantScope = new TenantScope(tenantId, false);
        clientProvider = mock(ThingsboardClientProvider.class);
        client = mock(ThingsboardClient.class);
        when(clientProvider.get(tenantId)).thenReturn(client);
        properties = new ThingModelProperties();
        properties.getRuleChain().setKafkaBootstrap(
                "test-kafka-" + UUID.randomUUID() + ":9092");
        productRepository = new ProductRepository(dsl, objectMapper);
        productService = newService(properties);
    }

    @Test
    void createProductSyncsRuleChainToThingsBoardAndDatabase() throws Exception {
        UUID rootId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();

        stubRoot(rootId);
        stubChainLookup(chainName(), List.of());
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(ruleChain(chainId, false));
        stubProfileLookup(List.of());
        stubNewProfile(profileId);
        stubNewAsset(assetId, assetProfileId);
        stubAttributes();

        ProductResponse response = productService.create(command());

        assertEquals(Constants.Product.STATUS_PUBLISHED, response.status());
        assertEquals(profileId.toString(), response.tbProfileId());
        assertEquals(assetId.toString(), response.tbAssetId());
        assertEquals(chainId.toString(), response.tbRuleChainId());
        assertEquals(7, response.propertyCount());

        ProductRepository.TslRow tsl = productRepository
                .findLatestTsl(PRODUCT_ID)
                .orElseThrow();
        assertEquals(Constants.Product.STATUS_PUBLISHED, tsl.status());
        assertEquals(Constants.Product.DEFAULT_TSL_VERSION, tsl.version());

        ProductRepository.BindingRow binding = productRepository
                .findBinding(PRODUCT_ID, Constants.Product.DEFAULT_TSL_VERSION)
                .orElseThrow();
        assertEquals(profileId, binding.profileId());
        assertEquals(assetId, binding.assetId());
        assertEquals(chainId, binding.ruleChainId());
        assertEquals(Constants.Tsl.SCHEMA_VERSION, binding.schemaVersion());
        assertEquals(Constants.Product.SYNC_RESULT_OK, binding.syncResult());
        assertNotNull(binding.syncedAt());

        List<ProductItem> products = productRepository.listProducts(tenantId, null);
        assertEquals(1, products.size());
        assertEquals(7, products.get(0).propertyCount());
        assertEquals(0, products.get(0).eventCount());
        assertEquals(0, products.get(0).actionCount());

        ArgumentCaptor<RuleChain> chainCaptor = ArgumentCaptor.forClass(RuleChain.class);
        verify(client).saveRuleChain(chainCaptor.capture());
        assertEquals(chainName(), chainCaptor.getValue().getName());
        assertEquals(RuleChainType.CORE, chainCaptor.getValue().getType());
        assertEquals(Boolean.FALSE, chainCaptor.getValue().getRoot());

        ArgumentCaptor<RuleChainMetaData> metadataCaptor =
                ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client).saveRuleChainMetaData(metadataCaptor.capture(), eq(false));
        RuleChainMetaData metadata = metadataCaptor.getValue();
        assertEquals(chainId, metadata.getRuleChainId().getId());
        assertEquals(12, metadata.getNodes().size());
        assertEquals(11, metadata.getConnections().size());

        ArgumentCaptor<DeviceProfile> profileCaptor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(profileCaptor.capture());
        assertEquals(chainId, profileCaptor.getValue().getDefaultRuleChainId().getId());

        ArgumentCaptor<String> attributesCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).saveEntityAttributesV2(
                eq(Constants.Product.ENTITY_TYPE_ASSET),
                eq(assetId.toString()),
                eq(Constants.Product.ATTRIBUTE_SCOPE_SERVER),
                attributesCaptor.capture());
        assertTrue(attributesCaptor.getValue().contains(Constants.Product.ATTRIBUTE_TSL_SCHEMA));
        assertTrue(attributesCaptor.getValue().contains(
                "\"" + Constants.Product.ATTRIBUTE_PRODUCT_ID + "\":\"" + PRODUCT_ID + "\""));
    }

    @Test
    void manualSyncReusesRuleChainAndUpdatesBinding() throws Exception {
        UUID rootId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();

        productRepository.saveDraft(
                tenantId,
                PRODUCT_ID,
                PRODUCT_NAME,
                PRODUCT_ID,
                "1",
                null,
                Constants.Product.DEFAULT_TSL_VERSION,
                "sum-1",
                false,
                loadValidTsl());

        stubRoot(rootId);
        stubChainLookup(chainName(), List.of(ruleChain(chainId, false)));
        stubProfileLookup(List.of(deviceProfile(profileId, chainId)));
        stubExistingProfile();
        stubExistingAsset(assetId, assetProfileId);
        stubAttributes();

        ProductResponse response = productService.sync(tenantScope, PRODUCT_ID);

        assertEquals(Constants.Product.STATUS_PUBLISHED, response.status());
        assertEquals(chainId.toString(), response.tbRuleChainId());

        verify(client, never()).saveRuleChain(any(RuleChain.class));
        verify(client).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));

        ArgumentCaptor<DeviceProfile> profileCaptor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(profileCaptor.capture());
        assertEquals(chainId, profileCaptor.getValue().getDefaultRuleChainId().getId());

        ProductRepository.TslRow tsl = productRepository
                .findLatestTsl(PRODUCT_ID)
                .orElseThrow();
        assertEquals(Constants.Product.STATUS_PUBLISHED, tsl.status());

        ProductRepository.BindingRow binding = productRepository
                .findBinding(PRODUCT_ID, Constants.Product.DEFAULT_TSL_VERSION)
                .orElseThrow();
        assertEquals(chainId, binding.ruleChainId());
        assertEquals(profileId, binding.profileId());
        assertEquals(assetId, binding.assetId());
    }

    @Test
    void createKeepsDraftWhenRuleChainSyncFails() throws Exception {
        UUID rootId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();

        stubRoot(rootId);
        stubChainLookup(chainName(), List.of());
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(ruleChain(chainId, false));
        doThrow(new org.thingsboard.client.ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), "boom"))
                .when(client)
                .saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));

        ApiException ex = assertThrows(ApiException.class, () -> productService.create(command()));

        assertEquals(HttpStatus.BAD_GATEWAY.value(), ex.getStatus().value());
        assertEquals(Constants.ErrorCode.TB_SYNC_FAILED, ex.getCode());
        assertEquals(
                String.format(Constants.Api.PRODUCT_SYNC_RETRY_PATH, PRODUCT_ID),
                ex.getRetry());

        ProductRepository.TslRow tsl = productRepository
                .findLatestTsl(PRODUCT_ID)
                .orElseThrow();
        assertEquals(Constants.Product.STATUS_DRAFT, tsl.status());
        assertTrue(productRepository
                .findBinding(PRODUCT_ID, Constants.Product.DEFAULT_TSL_VERSION)
                .isEmpty());

        verify(client, never()).saveDeviceProfile(any(DeviceProfile.class));
        verify(client, never()).saveAsset(any(ThingsboardApi.SaveAssetArgs.class));
    }

    @Test
    void createWithRuleChainDisabledPublishesWithoutBinding() throws Exception {
        properties.getRuleChain().setEnabled(false);
        productService = newService(properties);

        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();

        stubProfileLookup(List.of());
        stubNewProfile(profileId);
        stubNewAsset(assetId, assetProfileId);
        stubAttributes();

        ProductResponse response = productService.create(command());

        assertEquals(Constants.Product.STATUS_PUBLISHED, response.status());
        assertNull(response.tbRuleChainId());

        ProductRepository.BindingRow binding = productRepository
                .findBinding(PRODUCT_ID, Constants.Product.DEFAULT_TSL_VERSION)
                .orElseThrow();
        assertNull(binding.ruleChainId());
        assertEquals(profileId, binding.profileId());
        assertEquals(assetId, binding.assetId());

        verify(client, never()).saveRuleChain(any(RuleChain.class));
        verify(client, never()).saveRuleChainMetaData(
                any(RuleChainMetaData.class), anyBoolean());
    }

    private ProductService newService(ThingModelProperties properties) {
        TbRuleChainProvisioner provisioner =
                new TbRuleChainProvisioner(clientProvider, properties, objectMapper);
        TbProfileSyncer syncer = new TbProfileSyncer(
                clientProvider, profileGenerator, provisioner, objectMapper);
        return new ProductService(
                productRepository, parser, validator, compactSchema, syncer, objectMapper);
    }

    private CreateProductCommand command() throws Exception {
        return new CreateProductCommand(
                tenantId,
                PRODUCT_ID,
                PRODUCT_NAME,
                PRODUCT_ID,
                "1",
                Constants.Product.DEFAULT_TSL_VERSION,
                loadValidTsl(),
                false);
    }

    private void stubRoot(UUID rootId) {
        when(client.getRuleChains(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                        null,
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(ruleChainPage(List.of(ruleChain(rootId, true))));
    }

    private void stubChainLookup(String name, List<RuleChain> chains) {
        when(client.getRuleChains(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                        name,
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(ruleChainPage(chains));
    }

    private void stubProfileLookup(List<DeviceProfile> profiles) {
        PageDataDeviceProfile page = new PageDataDeviceProfile();
        page.setData(profiles);
        when(client.getDeviceProfiles(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        PROFILE_NAME,
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page);
    }

    private void stubNewProfile(UUID profileId) {
        when(client.saveDeviceProfile(any(DeviceProfile.class))).thenAnswer(invocation -> {
            DeviceProfile profile = invocation.getArgument(0);
            profile.setId(new DeviceProfileId().id(profileId));
            return profile;
        });
    }

    private void stubExistingProfile() {
        when(client.saveDeviceProfile(any(DeviceProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubNewAsset(UUID assetId, UUID assetProfileId) throws Exception {
        when(client.getTenantAssetByName(ASSET_NAME))
                .thenThrow(new org.thingsboard.client.ApiException(
                        HttpStatus.NOT_FOUND.value(), "not found"));
        AssetProfileInfo info = new AssetProfileInfo();
        info.setId(new AssetProfileId().id(assetProfileId));
        when(client.getDefaultAssetProfileInfo()).thenReturn(info);
        Asset saved = new Asset();
        saved.setId(new AssetId().id(assetId));
        when(client.saveAsset(any(ThingsboardApi.SaveAssetArgs.class))).thenReturn(saved);
    }

    private void stubExistingAsset(UUID assetId, UUID assetProfileId) throws Exception {
        Asset existing = new Asset();
        existing.setId(new AssetId().id(assetId));
        when(client.getTenantAssetByName(ASSET_NAME)).thenReturn(existing);
        AssetProfileInfo info = new AssetProfileInfo();
        info.setId(new AssetProfileId().id(assetProfileId));
        when(client.getDefaultAssetProfileInfo()).thenReturn(info);
        when(client.saveAsset(any(ThingsboardApi.SaveAssetArgs.class))).thenReturn(existing);
    }

    private void stubAttributes() throws Exception {
        when(client.saveEntityAttributesV2(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{}");
    }

    private RuleChain ruleChain(UUID id, boolean root) {
        RuleChain chain = new RuleChain();
        chain.setId(new RuleChainId().id(id));
        chain.setName(root ? "Root Rule Chain" : chainName());
        chain.setType(RuleChainType.CORE);
        chain.setRoot(root);
        return chain;
    }

    private DeviceProfile deviceProfile(UUID profileId, UUID ruleChainId) {
        DeviceProfile profile = new DeviceProfile();
        profile.setId(new DeviceProfileId().id(profileId));
        profile.setName(PROFILE_NAME);
        profile.setDefaultRuleChainId(new RuleChainId().id(ruleChainId));
        return profile;
    }

    private PageDataRuleChain ruleChainPage(List<RuleChain> chains) {
        PageDataRuleChain page = new PageDataRuleChain(null, null, false);
        page.setData(chains);
        return page;
    }

    private String chainName() {
        return properties.getRuleChain().getNamePrefix()
                + PRODUCT_ID
                + Constants.RuleChain.NAME_SUFFIX;
    }

    private JsonNode loadValidTsl() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/valid-tsl.json")) {
            assertNotNull(input);
            return objectMapper.readTree(input);
        }
    }
}
