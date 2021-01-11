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
package org.thingsboard.server.common.transport.lwm2m;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
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
    @Value("${transport.lwm2m.bootstrap.secure.start_psk:}")
    private Boolean bootstrapStartPsk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.start_rpk:}")
    private Boolean bootstrapStartRpk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.start_x509:}")
    private Boolean bootstrapStartX509;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_address:}")
    private String bootstrapHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_address:}")
    private String bootstrapSecureHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec_psk:}")
    private Integer bootstrapPortNoSecPsk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec_rpk:}")
    private Integer bootstrapPortNoSecRpk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec_x509:}")
    private Integer bootstrapPortNoSecX509;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_psk:}")
    private Integer bootstrapSecurePortPsk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_rpk:}")
    private Integer bootstrapSecurePortRpk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_x509:}")
    private Integer bootstrapSecurePortX509;

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
    private X509Certificate bootstrapCertificate;

    @Getter
    @Setter
    private Map<String /** clientEndPoint */, TransportProtos.ValidateDeviceCredentialsResponseMsg> sessions;
}
