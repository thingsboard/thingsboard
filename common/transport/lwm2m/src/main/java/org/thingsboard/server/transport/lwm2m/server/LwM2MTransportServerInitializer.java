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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.lwm2m.secure.LWM2MGenerationPSkRPkECC;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service("LwM2MTransportServerInitializer")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerInitializer {

    @Autowired
    @Qualifier("leshanServerCert")
    private LeshanServer lhServerCert;

    @Autowired
    @Qualifier("leshanServerRPK")
    private LeshanServer lhServerRPK;

    @Autowired
    private LwM2MTransportContextServer context;

    @PostConstruct
    public void init() {
        if (context.getEnableGenPskRpk()) new LWM2MGenerationPSkRPkECC();
        this.context.setSessions(new ConcurrentHashMap<>());
        this.lhServerCert.start();
        this.lhServerRPK.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport Server!");
        try {
            lhServerCert.destroy();
            lhServerRPK.destroy();
        } finally {
        }
        log.info("LwM2M transport Server stopped!");
    }
}
