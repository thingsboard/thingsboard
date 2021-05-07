/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.coapserver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Slf4j
@ConditionalOnExpression("'${transport.coap.security.mode:null}'=='NO_SECURE' || '${transport.coap.security.mode:null}'=='MIXED'")
@Component
public class TbCoapNoSecureSettings {

    @Getter
    @Value("${transport.coap.no_secure.bind_address}")
    private String host;

    @Getter
    @Value("${transport.coap.no_secure.bind_port}")
    private Integer port;

    public CoapEndpoint getNoSecureCoapEndpoint() throws UnknownHostException {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        InetSocketAddress sockAddr = getInetSocketAddress();
        builder.setInetSocketAddress(sockAddr);
        builder.setNetworkConfig(NetworkConfig.getStandard());
        return builder.build();
    }

    private InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }

}