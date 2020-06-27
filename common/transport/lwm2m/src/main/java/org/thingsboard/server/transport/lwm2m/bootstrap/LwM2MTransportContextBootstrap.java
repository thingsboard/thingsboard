package org.thingsboard.server.transport.lwm2m.bootstrap;
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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Map;

@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Component("LwM2MTransportContextBootstrap")
public class LwM2MTransportContextBootstrap extends TransportContext {

    @Getter
    @Value("#{${transport.lwm2m.bootstrap.bind_address} ?: localhost}")
    private String bootstrapHost;

    @Getter
    @Value("#{${transport.lwm2m.bootstrap.bind_port} ?: 5687}")
    private Integer bootstrapPort;

    @Getter
    @Value("#{${transport.lwm2m.bootstrap.secure.dtls_mode} ?: 3}")
    private int bootStrapDtlsMode;

    @Getter
    @Value("#{${transport.lwm2m.bootstrap.secure.bind_address} ?: localhost}")
    private String bootstrapSecureHost;

    @Getter
    @Value("#{${transport.lwm2m.bootstrap.secure.bind_port} ?: 5688}")
    private Integer bootstrapSecurePort;

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
