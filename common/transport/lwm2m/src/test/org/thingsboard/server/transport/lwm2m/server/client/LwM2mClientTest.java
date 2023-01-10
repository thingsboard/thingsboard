/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.net.InetSocketAddress;

public class LwM2mClientTest {

    @Test
    public void setRegistration() {
        LwM2mClient client = new LwM2mClient("nodeId", "testEndpoint");
        Registration registration = new Registration
                .Builder("test", "testEndpoint", Identity.unsecure(new InetSocketAddress(1000)))
                .objectLinks(new Link[0])
                .build();

        Assertions.assertDoesNotThrow(() -> client.setRegistration(registration));
    }
}