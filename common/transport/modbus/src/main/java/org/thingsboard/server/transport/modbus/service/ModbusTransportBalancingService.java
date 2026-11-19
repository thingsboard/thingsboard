// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.service.AbstractTransportBalancingService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;

@TbModbusTransportComponent
@Service
public class ModbusTransportBalancingService extends AbstractTransportBalancingService {

    public ModbusTransportBalancingService(PartitionService partitionService, ApplicationEventPublisher eventPublisher) {
        super(partitionService, eventPublisher);
    }

    @Override
    protected String getTransportName() {
        return DataConstants.MODBUS_TRANSPORT_NAME;
    }

}
