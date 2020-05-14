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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;

@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Component
public class LwM2MTransportCtx extends TransportContext {
    @Getter
    @Value("${transport.lwm2m.bind_address}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.bind_port}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.secure.bind_address}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.secure.bind_port}")
    private Integer securePort;

    @Getter
    @Value("${transport.lwm2m.timeout}")
    private Long timeout;

}
