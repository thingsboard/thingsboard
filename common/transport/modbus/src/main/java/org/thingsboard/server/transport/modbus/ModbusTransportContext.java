package org.thingsboard.server.transport.modbus;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Set;

@Component
public class ModbusTransportContext extends TransportContext {

    // todo: sessions
    private Set<DeviceId> allModbusDeviceIds;

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void setUp() {

    }

    private void fetchAllDeviceIds() {

    }
}
