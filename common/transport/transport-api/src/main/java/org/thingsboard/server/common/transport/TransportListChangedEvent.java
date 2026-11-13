// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

/**
 * Published when the set of nodes running a polling transport changes, so that transport can
 * re-check which devices it owns. Listeners filter by {@link #getTransportName()}.
 */
@Getter
@ToString(callSuper = true)
public class TransportListChangedEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -7392641582044617236L;

    private final String transportName;

    public TransportListChangedEvent(String transportName) {
        super(new Object());
        this.transportName = transportName;
    }

}
