// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvListProto;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractIntegrationUplinkProtoTest {

    private static final PostTelemetryMsg TELEMETRY = PostTelemetryMsg.newBuilder().addTsKvList(TsKvListProto.newBuilder().setTs(1L)).build();
    private static final PostAttributeMsg ATTRIBUTES = PostAttributeMsg.newBuilder().build();

    private IntegrationContext context;
    private AbstractIntegration<String> integration;

    @BeforeEach
    public void setUp() {
        context = mock(IntegrationContext.class);
        when(context.getRateLimitService()).thenReturn(Optional.empty());
        Integration configuration = new Integration();
        configuration.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        integration = new AbstractIntegration<>() {
            @Override
            public void process(String msg) {
            }

            @Override
            public void destroy() {
            }
        };
        integration.setConfiguration(configuration);
    }

    @Test
    public void testDeviceUplinkProtoCarriesTypeCastDisabledKeys() throws Exception {
        integration.processUplinkData(context, UplinkData.builder()
                .deviceName("d").deviceType("default")
                .telemetry(TELEMETRY).attributesUpdate(ATTRIBUTES)
                .telemetryTypeCastDisabledKeys(Set.of("devAddr"))
                .attributesTypeCastDisabledKeys(Set.of("joinEui", "hardwareVersion"))
                .build());

        ArgumentCaptor<DeviceUplinkDataProto> captor = ArgumentCaptor.forClass(DeviceUplinkDataProto.class);
        verify(context).processUplinkData(captor.capture(), any());
        DeviceUplinkDataProto proto = captor.getValue();
        assertThat(proto.getTelemetryTypeCastDisabledKeysList()).containsExactly("devAddr");
        assertThat(proto.getAttributesTypeCastDisabledKeysList()).containsExactlyInAnyOrder("joinEui", "hardwareVersion");
        assertThat(DeviceUplinkDataProto.parseFrom(proto.toByteArray())).isEqualTo(proto);
    }

    @Test
    public void testAssetUplinkProtoCarriesTypeCastDisabledKeys() throws Exception {
        integration.processUplinkData(context, UplinkData.builder()
                .isAsset(true).assetName("a").assetType("default")
                .telemetry(TELEMETRY).attributesUpdate(ATTRIBUTES)
                .telemetryTypeCastDisabledKeys(Set.of("devAddr"))
                .attributesTypeCastDisabledKeys(Set.of("joinEui"))
                .build());

        ArgumentCaptor<AssetUplinkDataProto> captor = ArgumentCaptor.forClass(AssetUplinkDataProto.class);
        verify(context).processUplinkData(captor.capture(), any());
        AssetUplinkDataProto proto = captor.getValue();
        assertThat(proto.getTelemetryTypeCastDisabledKeysList()).containsExactly("devAddr");
        assertThat(proto.getAttributesTypeCastDisabledKeysList()).containsExactly("joinEui");
        assertThat(AssetUplinkDataProto.parseFrom(proto.toByteArray())).isEqualTo(proto);
    }

    @Test
    public void testUplinkWithoutTypeCastDisabledKeysLeavesProtoFieldsEmpty() {
        integration.processUplinkData(context, UplinkData.builder()
                .deviceName("d").deviceType("default")
                .telemetry(TELEMETRY).attributesUpdate(ATTRIBUTES)
                .build());

        ArgumentCaptor<DeviceUplinkDataProto> captor = ArgumentCaptor.forClass(DeviceUplinkDataProto.class);
        verify(context).processUplinkData(captor.capture(), any());
        assertThat(captor.getValue().getTelemetryTypeCastDisabledKeysList()).isEmpty();
        assertThat(captor.getValue().getAttributesTypeCastDisabledKeysList()).isEmpty();
    }

}
