// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class LwM2mClientTest {

    @Test
    public void setRegistration() {
        LwM2mClient client = new LwM2mClient("nodeId", "testEndpoint");
        Registration registration = new Registration
                .Builder("testId", "testEndpoint", new IpPeer(new InetSocketAddress(1000)),
                        EndpointUriUtil.createUri("coap://localhost:5685"))
                .objectLinks(new Link[0])
                .build();

        Assertions.assertDoesNotThrow(() -> client.setRegistration(registration));
    }
}