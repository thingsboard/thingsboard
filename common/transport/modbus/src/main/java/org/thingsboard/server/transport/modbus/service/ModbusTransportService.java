// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

/**
 * Network side of the Modbus transport. Being a {@link TbTransportService} named {@code MODBUS} is what advertises
 * this node as a Modbus node in service discovery, so the balancer can split devices between Modbus nodes.
 */
@Slf4j
@TbModbusTransportComponent
@Service
public class ModbusTransportService implements TbTransportService {

    @Override
    public String getName() {
        return DataConstants.MODBUS_TRANSPORT_NAME;
    }

    public void startPolling(ModbusSessionContext session) {
        // todo: acquire the endpoint, plan read blocks, schedule timeseries/attributes poll tasks into session.getPollTasks()
        log.debug("[{}] Start polling", session.getDevice().getId());
    }

    public void stopPolling(ModbusSessionContext session) {
        // todo: cancel poll tasks, release the endpoint
        log.debug("[{}] Stop polling", session.getDevice().getId());
    }

}
