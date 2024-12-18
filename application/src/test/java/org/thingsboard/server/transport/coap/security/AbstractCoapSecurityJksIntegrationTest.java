/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@TestPropertySource(properties = {
        "coap.enabled=true",
        "coap.dtls.enabled=true",
        "coap.dtls.credentials.type=KEYSTORE",
        "coap.dtls.credentials.keystore.store_file=coap/credentials/coapserver.jks",
        "coap.dtls.credentials.keystore.key_password=server_ks_password",
        "coap.dtls.credentials.keystore.key_alias=server",
        "device.connectivity.coaps.enabled=true",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
public abstract class AbstractCoapSecurityJksIntegrationTest extends AbstractCoapSecurityIntegrationTest {
}