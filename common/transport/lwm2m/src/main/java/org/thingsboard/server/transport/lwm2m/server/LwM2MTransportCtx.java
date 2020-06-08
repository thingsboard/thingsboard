/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Map;

@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Component("LwM2MTransportCtx")
public class LwM2MTransportCtx extends TransportContext {

    @Getter
    @Value("${transport.lwm2m.bind_address:localhost}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.bind_port:5685}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.secure.bind_address:localhost}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.secure.bind_port:5686}")
    private Integer securePort;

    @Getter
    @Value("${transport.lwm2m.timeout:5000}")
    private Long timeout;

    @Getter
    @Value("${transport.lwm2m.secure.x509.enabled:}")
    private boolean X509Enabled;

    @Getter
    @Value("${transport.lwm2m.secure.x509.key_store_type:}")
    private String keyStoreType;

    @Getter
    @Value("${transport.lwm2m.secure.x509.root_alias:}")
    private String rootAlias;

    @Getter
    @Value("${transport.lwm2m.secure.x509.bootstrap.alias:}")
    private String aliasBootstrap;

    @Getter
    @Value("${transport.lwm2m.secure.x509.server.key_store_path:}")
    private String keyStorePathServer;

    @Getter
    @Value("${transport.lwm2m.secure.x509.server.key_store_password:}")
    private String keyStorePasswordServer;

    @Getter
    @Value("${transport.lwm2m.secure.x509.server.alias:}")
    private String aliasServer;

    @Getter
    @Value("${transport.lwm2m.secure.x509.client.key_store_path:}")
    private String keyStorePathClient;

    @Getter
    @Value("${transport.lwm2m.secure.x509.client.key_store_password:}")
    private String keyStorePasswordClient;

    @Getter
    @Value("${transport.lwm2m.secure.x509.client.alias:}")
    private String aliasClient;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.enabled:}")
    private boolean rpkEnabled;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.server.public_x:}")
    private String publicServerX;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.server.public_y:}")
    private String publicServerY;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.server.public_encoded:}")
    private String publicServerEncoded;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.server.private_s:}")
    private String privateServerS;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.server.private_encoded:}")
    private String privateServerEncoded;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.client.public_x:}")
    private String publicClientX;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.client.public_y:}")
    private String publicClientY;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.client.public_encoded:}")
    private String publicClientEncoded;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.client.private_s:}")
    private String privateClientS;

    @Getter
    @Value("${transport.lwm2m.secure.rpk.client.private_encoded:}")
    private String privateClientEncoded;

    @Getter
    @Value("${transport.lwm2m.secure.redis_url:}")
    private String redisUrl;

    @Getter
    @Setter
    private Map<String /* clientEndPoint */, TransportProtos.ValidateDeviceCredentialsResponseMsg> sessions;

}
