package com.iot.thingmodel.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import com.iot.thingmodel.tsl.CompactSchema;
import com.iot.thingmodel.tsl.TslDefinition;
import com.iot.thingmodel.tsl.TslParser;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.api.ThingsboardApi;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AssetId;
import org.thingsboard.client.model.AssetProfileId;
import org.thingsboard.client.model.AssetProfileInfo;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.DeviceProfileId;
import org.thingsboard.client.model.PageDataDeviceProfile;
import org.thingsboard.client.model.RuleChainId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbProfileSyncerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslParser parser = new TslParser(objectMapper);
    private final CompactSchema compactSchema = new CompactSchema(objectMapper);
    private final ProfileGenerator profileGenerator = new ProfileGenerator();

    @Test
    void syncsProfileAssetAndAttributes() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();

        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any())).thenReturn(ruleChainId);

        PageDataDeviceProfile page = new PageDataDeviceProfile();
        page.setData(List.of());
        when(client.getDeviceProfiles(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        "WiFi诊断终端 9HYN9PJNOY",
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page);

        DeviceProfile savedProfile = new DeviceProfile();
        savedProfile.setId(new DeviceProfileId().id(profileId));
        when(client.saveDeviceProfile(any(DeviceProfile.class))).thenReturn(savedProfile);

        when(client.getTenantAssetByName(
                        Constants.Product.ASSET_NAME_PREFIX + "9HYN9PJNOY"))
                .thenThrow(new org.thingsboard.client.ApiException(
                        HttpStatus.NOT_FOUND.value(), "not found"));

        AssetProfileInfo info = new AssetProfileInfo();
        info.setId(new AssetProfileId().id(assetProfileId));
        when(client.getDefaultAssetProfileInfo()).thenReturn(info);

        Asset savedAsset = new Asset();
        savedAsset.setId(new AssetId().id(assetId));
        when(client.saveAsset(any(ThingsboardApi.SaveAssetArgs.class))).thenReturn(savedAsset);
        when(client.saveEntityAttributesV2(
                        eq(Constants.Product.ENTITY_TYPE_ASSET),
                        eq(assetId.toString()),
                        eq(Constants.Product.ATTRIBUTE_SCOPE_SERVER),
                        anyString()))
                .thenReturn("{}");

        TbProfileSyncer syncer = new TbProfileSyncer(provider, profileGenerator, ruleChainProvisioner, objectMapper);
        TslDefinition definition = parser.parse(load());
        CompactSchema.CompactResult compact = compactSchema.generate(definition, false);
        TbSyncRequest request = new TbSyncRequest(
                tenantId,
                "9HYN9PJNOY",
                "WiFi诊断终端",
                "9HYN9PJNOY",
                "1",
                "1.0",
                definition.raw(),
                definition,
                compact);

        SyncResult result = syncer.sync(request);

        assertEquals(profileId, result.profileId());
        assertEquals(assetId, result.assetId());
        assertEquals(ruleChainId, result.ruleChainId());
        assertEquals(compact.bytes(), result.schemaBytes());

        ArgumentCaptor<DeviceProfile> profileCaptor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(profileCaptor.capture());
        assertEquals(ruleChainId, profileCaptor.getValue().getDefaultRuleChainId().getId());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(client).saveEntityAttributesV2(
                eq(Constants.Product.ENTITY_TYPE_ASSET),
                eq(assetId.toString()),
                eq(Constants.Product.ATTRIBUTE_SCOPE_SERVER),
                body.capture());
        assertTrue(body.getValue().contains("\"product_id\":\"9HYN9PJNOY\""));
        assertTrue(body.getValue().contains("\"tsl_schema\""));
        assertTrue(body.getValue().contains("\"tsl_json\""));
    }

    @Test
    void preservesExistingRuleChainWhenProvisioningDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();
        UUID existingRuleChainId = UUID.randomUUID();

        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any())).thenReturn(null);

        DeviceProfile existing = new DeviceProfile();
        existing.setId(new DeviceProfileId().id(profileId));
        existing.setName("WiFi诊断终端 9HYN9PJNOY");
        existing.setDefaultRuleChainId(new RuleChainId().id(existingRuleChainId));
        existing.setDefaultQueueName("edge-queue");
        PageDataDeviceProfile page = new PageDataDeviceProfile();
        page.setData(List.of(existing));
        when(client.getDeviceProfiles(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        "WiFi诊断终端 9HYN9PJNOY",
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page);
        when(client.saveDeviceProfile(any(DeviceProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Asset existingAsset = new Asset();
        existingAsset.setId(new AssetId().id(assetId));
        when(client.getTenantAssetByName(
                        Constants.Product.ASSET_NAME_PREFIX + "9HYN9PJNOY"))
                .thenReturn(existingAsset);
        AssetProfileInfo info = new AssetProfileInfo();
        info.setId(new AssetProfileId().id(assetProfileId));
        when(client.getDefaultAssetProfileInfo()).thenReturn(info);
        when(client.saveAsset(any(ThingsboardApi.SaveAssetArgs.class))).thenReturn(existingAsset);
        when(client.saveEntityAttributesV2(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{}");

        SyncResult result = new TbProfileSyncer(
                        provider, profileGenerator, ruleChainProvisioner, objectMapper)
                .sync(request(tenantId));

        assertNull(result.ruleChainId());
        ArgumentCaptor<DeviceProfile> captor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(captor.capture());
        assertEquals(existingRuleChainId, captor.getValue().getDefaultRuleChainId().getId());
        assertEquals("edge-queue", captor.getValue().getDefaultQueueName());
        verify(ruleChainProvisioner).ensure(any());
    }

    @Test
    void retriesAfterAuthenticationFailureAndBindsRuleChain() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();
        UUID ruleChainId = UUID.randomUUID();

        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any()))
                .thenThrow(new org.thingsboard.client.ApiException(
                        HttpStatus.UNAUTHORIZED.value(), "unauthorized"))
                .thenReturn(ruleChainId);

        PageDataDeviceProfile page = new PageDataDeviceProfile();
        page.setData(List.of());
        when(client.getDeviceProfiles(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        "WiFi诊断终端 9HYN9PJNOY",
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page);
        DeviceProfile savedProfile = new DeviceProfile();
        savedProfile.setId(new DeviceProfileId().id(profileId));
        when(client.saveDeviceProfile(any(DeviceProfile.class))).thenReturn(savedProfile);

        when(client.getTenantAssetByName(
                        Constants.Product.ASSET_NAME_PREFIX + "9HYN9PJNOY"))
                .thenThrow(new org.thingsboard.client.ApiException(
                        HttpStatus.NOT_FOUND.value(), "not found"));
        AssetProfileInfo info = new AssetProfileInfo();
        info.setId(new AssetProfileId().id(assetProfileId));
        when(client.getDefaultAssetProfileInfo()).thenReturn(info);
        Asset savedAsset = new Asset();
        savedAsset.setId(new AssetId().id(assetId));
        when(client.saveAsset(any(ThingsboardApi.SaveAssetArgs.class))).thenReturn(savedAsset);
        when(client.saveEntityAttributesV2(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{}");

        SyncResult result = new TbProfileSyncer(
                        provider, profileGenerator, ruleChainProvisioner, objectMapper)
                .sync(request(tenantId));

        assertEquals(ruleChainId, result.ruleChainId());
        verify(provider).evict(tenantId);
        verify(ruleChainProvisioner, times(2)).ensure(any());
        ArgumentCaptor<DeviceProfile> captor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(captor.capture());
        assertEquals(ruleChainId, captor.getValue().getDefaultRuleChainId().getId());
    }

    @Test
    void doesNotSaveProfileWhenRuleChainProvisioningFails() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any()))
                .thenThrow(new TbSyncException("boom", HttpStatus.INTERNAL_SERVER_ERROR.value(), null));

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> new TbProfileSyncer(
                                provider, profileGenerator, ruleChainProvisioner, objectMapper)
                        .sync(request(tenantId)));

        assertEquals("boom", ex.getMessage());
        verify(client, never()).saveDeviceProfile(any(DeviceProfile.class));
        verify(client, never()).saveAsset(any(ThingsboardApi.SaveAssetArgs.class));
    }

    private TbSyncRequest request(UUID tenantId) throws Exception {
        TslDefinition definition = parser.parse(load());
        CompactSchema.CompactResult compact = compactSchema.generate(definition, false);
        return new TbSyncRequest(
                tenantId,
                "9HYN9PJNOY",
                "WiFi诊断终端",
                "9HYN9PJNOY",
                "1",
                "1.0",
                definition.raw(),
                definition,
                compact);
    }

    private JsonNode load() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/valid-tsl.json")) {
            return objectMapper.readTree(input);
        }
    }
}
