package com.iot.thingmodel.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.support.TbClientStubs;
import com.iot.thingmodel.support.TslFixtures;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.DeviceProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbProfileSyncerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
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

        TbClientStubs.stubProfileLookup(client, TbClientStubs.emptyDeviceProfilePage());
        TbClientStubs.stubProfileSaved(client, profileId);
        TbClientStubs.stubNoAsset(client);
        TbClientStubs.stubDefaultAssetProfile(client, assetProfileId);
        TbClientStubs.stubAssetSaved(client, assetId);
        TbClientStubs.stubAttributes(client);

        TbSyncRequest request = TslFixtures.syncRequest(objectMapper, tenantId);
        TbProfileSyncer syncer = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper);

        SyncResult result = syncer.sync(request);

        assertEquals(profileId, result.profileId());
        assertEquals(assetId, result.assetId());
        assertEquals(ruleChainId, result.ruleChainId());
        assertEquals(request.compact().bytes(), result.schemaBytes());

        ArgumentCaptor<DeviceProfile> profileCaptor =
                ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(profileCaptor.capture());
        assertEquals(ruleChainId, profileCaptor.getValue().getDefaultRuleChainId().getId());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(client).saveEntityAttributesV2(
                eq(Constants.Product.ENTITY_TYPE_ASSET),
                eq(assetId.toString()),
                eq(Constants.Product.ATTRIBUTE_SCOPE_SERVER),
                body.capture());
        assertTrue(body.getValue().contains("\"product_id\":\"" + TslFixtures.PRODUCT_ID + "\""));
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

        DeviceProfile existing = TbClientStubs.deviceProfile(
                profileId, "WiFi诊断终端 9HYN9PJNOY", existingRuleChainId);
        existing.setDefaultQueueName("edge-queue");
        TbClientStubs.stubProfileLookup(
                client, TbClientStubs.deviceProfilePage(List.of(existing)));
        TbClientStubs.stubProfileSaved(client, profileId);
        TbClientStubs.stubExistingAsset(client, assetId);
        TbClientStubs.stubDefaultAssetProfile(client, assetProfileId);
        TbClientStubs.stubAssetSaved(client, assetId);
        TbClientStubs.stubAttributes(client);

        TbProfileSyncer syncer = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper);
        SyncResult result = syncer.sync(TslFixtures.syncRequest(objectMapper, tenantId));

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

        TbClientStubs.stubProfileLookup(client, TbClientStubs.emptyDeviceProfilePage());
        TbClientStubs.stubProfileSaved(client, profileId);
        TbClientStubs.stubNoAsset(client);
        TbClientStubs.stubDefaultAssetProfile(client, assetProfileId);
        TbClientStubs.stubAssetSaved(client, assetId);
        TbClientStubs.stubAttributes(client);

        TbProfileSyncer syncer = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper);
        SyncResult result = syncer.sync(TslFixtures.syncRequest(objectMapper, tenantId));

        assertEquals(ruleChainId, result.ruleChainId());
        verify(provider).evict(tenantId);
        verify(ruleChainProvisioner, times(2)).ensure(any());
        ArgumentCaptor<DeviceProfile> captor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(captor.capture());
        assertEquals(ruleChainId, captor.getValue().getDefaultRuleChainId().getId());
    }

    @Test
    void updatesExistingProfileAndAsset() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID assetProfileId = UUID.randomUUID();
        UUID oldRuleChainId = UUID.randomUUID();
        UUID newRuleChainId = UUID.randomUUID();

        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any())).thenReturn(newRuleChainId);

        DeviceProfile existing = TbClientStubs.deviceProfile(
                profileId, "WiFi诊断终端 9HYN9PJNOY", oldRuleChainId);
        existing.setDefaultQueueName("edge-queue");
        TbClientStubs.stubProfileLookup(
                client, TbClientStubs.deviceProfilePage(List.of(existing)));
        TbClientStubs.stubProfileSaved(client, profileId);
        TbClientStubs.stubExistingAsset(client, assetId);
        TbClientStubs.stubDefaultAssetProfile(client, assetProfileId);
        TbClientStubs.stubAssetSaved(client, assetId);
        TbClientStubs.stubAttributes(client);

        SyncResult result = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper)
                .sync(TslFixtures.syncRequest(objectMapper, tenantId));

        assertEquals(profileId, result.profileId());
        assertEquals(assetId, result.assetId());
        assertEquals(newRuleChainId, result.ruleChainId());
        ArgumentCaptor<DeviceProfile> captor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(client).saveDeviceProfile(captor.capture());
        assertEquals(newRuleChainId, captor.getValue().getDefaultRuleChainId().getId());
        assertEquals("edge-queue", captor.getValue().getDefaultQueueName());
    }

    @Test
    void failsWhenDefaultAssetProfileIsMissing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any())).thenReturn(UUID.randomUUID());
        TbClientStubs.stubProfileLookup(client, TbClientStubs.emptyDeviceProfilePage());
        TbClientStubs.stubProfileSaved(client, UUID.randomUUID());
        TbClientStubs.stubNoAsset(client);
        when(client.getDefaultAssetProfileInfo()).thenReturn(null);

        TbProfileSyncer syncer = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper);

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> syncer.sync(TslFixtures.syncRequest(objectMapper, tenantId)));

        assertTrue(ex.getMessage().contains("默认资产模板"));
    }

    @Test
    void failsAfterSecondAuthenticationFailure() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any()))
                .thenThrow(new org.thingsboard.client.ApiException(
                        HttpStatus.UNAUTHORIZED.value(), "unauthorized"));

        TbProfileSyncer syncer = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper);

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> syncer.sync(TslFixtures.syncRequest(objectMapper, tenantId)));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getHttpCode());
        verify(provider).evict(tenantId);
        verify(ruleChainProvisioner, times(2)).ensure(any());
    }

    @Test
    void doesNotSaveProfileWhenRuleChainProvisioningFails() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        TbRuleChainProvisioner ruleChainProvisioner = mock(TbRuleChainProvisioner.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(ruleChainProvisioner.ensure(any()))
                .thenThrow(new TbSyncException(
                        "boom", HttpStatus.INTERNAL_SERVER_ERROR.value(), null));

        TbProfileSyncer syncer = new TbProfileSyncer(
                provider, profileGenerator, ruleChainProvisioner, objectMapper);

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> syncer.sync(TslFixtures.syncRequest(objectMapper, tenantId)));

        assertEquals("boom", ex.getMessage());
        verify(client, never()).saveDeviceProfile(any(DeviceProfile.class));
        verify(client, never()).saveAsset(any());
    }
}
