// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceIdsByTransportTypeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetSnmpDevicesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetSnmpDevicesResponseMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceProtoServiceTest {

    @Mock
    private TransportService transportService;

    private DeviceProtoService deviceProtoService;
    private final List<DeviceId> received = new ArrayList<>();

    @BeforeEach
    void setUp() {
        deviceProtoService = new DeviceProtoService(transportService);
    }

    @Test
    void readsAllPagesFromCore() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(transportService.getDeviceIdsByTransportType(DeviceTransportType.MODBUS, 0, 512)).thenReturn(page(true, first));
        when(transportService.getDeviceIdsByTransportType(DeviceTransportType.MODBUS, 1, 512)).thenReturn(page(false, second));

        deviceProtoService.forEachDeviceIdByTransportType("test", DeviceTransportType.MODBUS, 1, received::add);

        assertThat(received).containsExactly(new DeviceId(first), new DeviceId(second));
    }

    @Test
    @SuppressWarnings("deprecation")
    void snmpFallsBackToOldRequestWhenCoreDoesNotSupportTheNewOne() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(transportService.getDeviceIdsByTransportType(any(), anyInt(), anyInt())).thenReturn(null);
        when(transportService.getSnmpDevicesIds(snmpRequest(0))).thenReturn(snmpPage(true, first));
        when(transportService.getSnmpDevicesIds(snmpRequest(1))).thenReturn(snmpPage(false, second));

        deviceProtoService.forEachDeviceIdByTransportType("test", DeviceTransportType.SNMP, 1, received::add);

        assertThat(received).containsExactly(new DeviceId(first), new DeviceId(second));
    }

    @Test
    @SuppressWarnings("deprecation")
    void otherTransportsFailInsteadOfSeeingNoDevicesWhenCoreDoesNotSupportTheRequest() {
        when(transportService.getDeviceIdsByTransportType(DeviceTransportType.MODBUS, 0, 512)).thenReturn(null);

        deviceProtoService.forEachDeviceIdByTransportType("test", DeviceTransportType.MODBUS, 1, received::add);

        assertThat(received).isEmpty();
        verify(transportService, times(1)).getDeviceIdsByTransportType(DeviceTransportType.MODBUS, 0, 512);
        verify(transportService, never()).getSnmpDevicesIds(any());
    }

    @Test
    void unsupportedRequestIsReportedAsAnError() {
        when(transportService.getDeviceIdsByTransportType(DeviceTransportType.MODBUS, 0, 512)).thenReturn(null);

        assertThatThrownBy(() -> deviceProtoService.getDevicesIds(DeviceTransportType.MODBUS, 0, 512))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MODBUS");
    }

    private static GetDeviceIdsByTransportTypeResponseMsg page(boolean hasNext, UUID... ids) {
        GetDeviceIdsByTransportTypeResponseMsg.Builder builder = GetDeviceIdsByTransportTypeResponseMsg.newBuilder().setHasNextPage(hasNext);
        for (UUID id : ids) {
            builder.addIds(id.toString());
        }
        return builder.build();
    }

    @SuppressWarnings("deprecation")
    private static GetSnmpDevicesRequestMsg snmpRequest(int page) {
        return GetSnmpDevicesRequestMsg.newBuilder().setPage(page).setPageSize(512).build();
    }

    @SuppressWarnings("deprecation")
    private static GetSnmpDevicesResponseMsg snmpPage(boolean hasNext, UUID... ids) {
        GetSnmpDevicesResponseMsg.Builder builder = GetSnmpDevicesResponseMsg.newBuilder().setHasNextPage(hasNext);
        for (UUID id : ids) {
            builder.addIds(id.toString());
        }
        return builder.build();
    }

}
