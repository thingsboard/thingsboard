// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.TransportListChangedEvent;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.ModbusTransportContext;

@TbModbusTransportComponent
@Component
@RequiredArgsConstructor
public class ModbusTransportListChangedEventListener extends TbApplicationEventListener<TransportListChangedEvent> {

    private final ModbusTransportContext modbusTransportContext;

    @Override
    protected boolean filterTbApplicationEvent(TransportListChangedEvent event) {
        return DataConstants.MODBUS_TRANSPORT_NAME.equals(event.getTransportName());
    }

    @Override
    protected void onTbApplicationEvent(TransportListChangedEvent event) {
        modbusTransportContext.onModbusTransportListChanged();
    }
}