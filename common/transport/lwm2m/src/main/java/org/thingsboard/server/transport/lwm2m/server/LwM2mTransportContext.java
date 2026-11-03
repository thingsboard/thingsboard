// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.leshan.server.LeshanServer;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@TbLwM2mTransportComponent
public class LwM2mTransportContext extends TransportContext {

    @Getter @Setter
    private LeshanServer server;

    private final AtomicInteger connectionsActiveCounterLWM2M = new AtomicInteger();
    private final AtomicInteger connectionsActiveCounterLWM2MDTLS = new AtomicInteger();

    @PostConstruct
    public void init() {
        super.init();
        transportService.createGaugeStats("connections_active", connectionsActiveCounterLWM2M, "protocol", "LWM2M");
        transportService.createGaugeStats("connections_active", connectionsActiveCounterLWM2MDTLS, "protocol", "LWM2M_DTLS");
    }

    /**
     * Active sessions are the registered clients, secure ones are registered over DTLS (PSK, RPK or X.509 identity).
     * Counted from the local client context, which is complete because the LwM2M transport runs as a single node.
     * With several nodes sharing the client store, each node would also count the clients fetched from the store on startup.
     */
    public void updateActiveSessions(int lwm2m, int lwm2mDtls) {
        connectionsActiveCounterLWM2M.set(lwm2m);
        connectionsActiveCounterLWM2MDTLS.set(lwm2mDtls);
    }

}
