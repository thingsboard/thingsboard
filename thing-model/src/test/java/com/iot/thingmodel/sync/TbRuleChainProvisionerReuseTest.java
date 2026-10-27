package com.iot.thingmodel.sync;

import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.support.TbClientStubs;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleNode;
import org.thingsboard.client.model.RuleNodeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbRuleChainProvisionerReuseTest extends TbRuleChainProvisionerTestSupport {

    @Test
    void renamesLegacyRuleChainToProductId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);

        stubByName(client, PRODUCT_ID, emptyPage(false));
        RuleChain legacy = existingChain(chainId);
        legacy.setName(Constants.RuleChain.LEGACY_NAME_PREFIX + PRODUCT_ID
                + Constants.RuleChain.LEGACY_NAME_SUFFIX);
        stubByName(client, legacy.getName(), page(legacy, false));
        TbClientStubs.stubRuleChainSaved(client);

        UUID actual = new TbRuleChainProvisioner(provider, properties(), objectMapper)
                .ensure(request(tenantId));

        assertEquals(chainId, actual);
        ArgumentCaptor<RuleChain> chainCaptor = ArgumentCaptor.forClass(RuleChain.class);
        verify(client).saveRuleChain(chainCaptor.capture());
        assertEquals(chainId, chainCaptor.getValue().getId().getId());
        assertEquals(PRODUCT_ID, chainCaptor.getValue().getName());
        verify(client).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void reusesExistingRuleChainAndPreservesNodeIds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        UUID switchNodeId = UUID.randomUUID();
        UUID saveTimeseriesNodeId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        stubByName(client, chainName(), page(existingChain(chainId), false));

        RuleNode existingSwitch = existingNode(
                chainId, Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH, switchNodeId);
        existingSwitch.setAdditionalInfo(objectMapper.createObjectNode()
                .put(Constants.RuleChain.CONFIG_LAYOUT_X, 42)
                .put(Constants.RuleChain.CONFIG_LAYOUT_Y, 84));
        RuleNode existingSaveTimeseries = existingNode(
                chainId, Constants.RuleChain.NODE_KEY_SAVE_TIMESERIES, saveTimeseriesNodeId);
        existingSaveTimeseries.setAdditionalInfo(objectMapper.createObjectNode()
                .put(Constants.RuleChain.CONFIG_LAYOUT_X, 43)
                .put(Constants.RuleChain.CONFIG_LAYOUT_Y, 85));
        RuleChainMetaData existing = new RuleChainMetaData(new RuleChainId().id(chainId));
        existing.setNodes(List.of(existingSwitch, existingSaveTimeseries));
        when(client.getRuleChainMetaData(chainId.toString())).thenReturn(existing);

        UUID actual = new TbRuleChainProvisioner(provider, properties(), objectMapper)
                .ensure(request(tenantId));

        assertEquals(chainId, actual);
        verify(client, never()).saveRuleChain(any(RuleChain.class));

        RuleChainMetaData metadata = captureMetadata(client);
        Map<UUID, RuleNode> byExternalId = metadata.getNodes().stream()
                .collect(Collectors.toMap(node -> node.getExternalId().getId(), Function.identity()));
        assertEquals(switchNodeId, byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH)).getId().getId());
        assertEquals(saveTimeseriesNodeId, byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_SAVE_TIMESERIES)).getId().getId());
        assertNull(byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY)).getId());
        RuleNode savedSwitch = byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH));
        assertEquals(42, savedSwitch.getAdditionalInfo()
                .path(Constants.RuleChain.CONFIG_LAYOUT_X).asInt());
        assertEquals(84, savedSwitch.getAdditionalInfo()
                .path(Constants.RuleChain.CONFIG_LAYOUT_Y).asInt());
        RuleNode savedKafka = byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY));
        assertEquals(
                Constants.RuleChain.LAYOUT_START_X
                        + 7 * Constants.RuleChain.LAYOUT_COLUMN_GAP,
                savedKafka.getAdditionalInfo()
                        .path(Constants.RuleChain.CONFIG_LAYOUT_X).asInt());
        assertEquals(Constants.RuleChain.LAYOUT_START_Y,
                savedKafka.getAdditionalInfo()
                        .path(Constants.RuleChain.CONFIG_LAYOUT_Y).asInt());
    }

    @Test
    void findsExistingChainAcrossPages() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(client.getRuleChains(
                Constants.ThingsBoard.PAGE_SIZE,
                0,
                Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                chainName(),
                Constants.ThingsBoard.SORT_PROPERTY_NAME,
                Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(emptyPage(true));
        when(client.getRuleChains(
                Constants.ThingsBoard.PAGE_SIZE,
                1,
                Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                chainName(),
                Constants.ThingsBoard.SORT_PROPERTY_NAME,
                Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page(existingChain(chainId), false));

        UUID actual = new TbRuleChainProvisioner(provider, properties(), objectMapper)
                .ensure(request(tenantId));

        assertEquals(chainId, actual);
        verify(client, never()).saveRuleChain(any(RuleChain.class));
        verify(client).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void keepsExternalIdsStableAndUsesServerAssignedIds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(client.getRuleChains(
                Constants.ThingsBoard.PAGE_SIZE,
                0,
                Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                chainName(),
                Constants.ThingsBoard.SORT_PROPERTY_NAME,
                Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(emptyPage(false), page(existingChain(chainId), false));
        TbClientStubs.stubRuleChainCreated(client, chainId);

        TbRuleChainProvisioner provisioner =
                new TbRuleChainProvisioner(provider, properties(), objectMapper);
        provisioner.ensure(request(tenantId));

        ArgumentCaptor<RuleChainMetaData> firstCaptor =
                ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client).saveRuleChainMetaData(firstCaptor.capture(), eq(false));
        RuleChainMetaData first = firstCaptor.getValue();
        Map<UUID, UUID> assignedIds = first.getNodes().stream()
                .collect(Collectors.toMap(
                        node -> node.getExternalId().getId(),
                        node -> UUID.randomUUID()));
        RuleChainMetaData existing = new RuleChainMetaData(new RuleChainId().id(chainId));
        existing.setNodes(first.getNodes().stream().map(node -> {
            RuleNode saved = new RuleNode();
            saved.setId(new RuleNodeId().id(assignedIds.get(node.getExternalId().getId())));
            saved.setExternalId(node.getExternalId());
            return saved;
        }).toList());
        when(client.getRuleChainMetaData(chainId.toString())).thenReturn(existing);

        provisioner.ensure(request(tenantId));

        ArgumentCaptor<RuleChainMetaData> captor = ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client, times(2)).saveRuleChainMetaData(captor.capture(), eq(false));
        RuleChainMetaData second = captor.getAllValues().get(1);
        Map<UUID, UUID> actualIds = second.getNodes().stream()
                .collect(Collectors.toMap(
                        node -> node.getExternalId().getId(),
                        node -> node.getId().getId()));
        assertEquals(assignedIds, actualIds);
        assertEquals(
                first.getNodes().stream().map(node -> node.getExternalId().getId()).toList(),
                second.getNodes().stream().map(node -> node.getExternalId().getId()).toList());
    }
}
