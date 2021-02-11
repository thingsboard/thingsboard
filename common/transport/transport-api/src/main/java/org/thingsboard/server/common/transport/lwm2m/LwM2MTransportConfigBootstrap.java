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
package org.thingsboard.server.common.transport.lwm2m;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.security.PublicKey;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MTransportConfigBootstrap {

    @Getter
    @Value("${transport.lwm2m.bootstrap.enable:}")
    private Boolean bootstrapEnable;

    @Getter
    @Value("${transport.lwm2m.bootstrap.id:}")
    private Integer bootstrapServerId;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_address:}")
    private String bootstrapHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec:}")
    private Integer bootstrapPortNoSec;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_address_security:}")
    private String bootstrapHostSecurity;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_security:}")
    private Integer bootstrapPortSecurity;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.public_x:}")
    private String bootstrapPublicX;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.public_y:}")
    private String bootstrapPublicY;

    @Getter
    @Setter
    private PublicKey bootstrapPublicKey;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.private_s:}")
    private String bootstrapPrivateS;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.alias:}")
    private String bootstrapAlias;

    @Getter
    @Setter
    private Map<String /** clientEndPoint */, TransportProtos.ValidateDeviceCredentialsResponseMsg> sessions;
}
