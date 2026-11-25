// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.snmp.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.TransportListChangedEvent;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;

@TbSnmpTransportComponent
@Component
@RequiredArgsConstructor
public class SnmpTransportListChangedEventListener extends TbApplicationEventListener<TransportListChangedEvent> {

    private final SnmpTransportContext snmpTransportContext;

    @Override
    protected boolean filterTbApplicationEvent(TransportListChangedEvent event) {
        return DataConstants.SNMP_TRANSPORT_NAME.equals(event.getTransportName());
    }

    @Override
    protected void onTbApplicationEvent(TransportListChangedEvent event) {
        snmpTransportContext.onSnmpTransportListChanged();
    }
}
