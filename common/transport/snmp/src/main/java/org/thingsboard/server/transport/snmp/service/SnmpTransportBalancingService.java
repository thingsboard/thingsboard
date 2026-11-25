// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.snmp.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.service.AbstractTransportBalancingService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;

@TbSnmpTransportComponent
@Service
public class SnmpTransportBalancingService extends AbstractTransportBalancingService {

    public SnmpTransportBalancingService(PartitionService partitionService, ApplicationEventPublisher eventPublisher) {
        super(partitionService, eventPublisher);
    }

    @Override
    protected String getTransportName() {
        return DataConstants.SNMP_TRANSPORT_NAME;
    }

}
