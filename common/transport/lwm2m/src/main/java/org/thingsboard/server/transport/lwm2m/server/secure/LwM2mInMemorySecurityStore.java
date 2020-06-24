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
package org.thingsboard.server.transport.lwm2m.server.secure;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MGetSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.NO_SEC;

@Slf4j
@Component("LwM2mInMemorySecurityStore")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2mInMemorySecurityStore extends InMemorySecurityStore {

    @Autowired
    LwM2MGetSecurityInfo lwM2MGetSecurityInfo;
//
//    @Autowired
//    public LwM2MTransportContext context;
//
//    @Autowired
//    private LwM2MCredentials credentials;

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        SecurityInfo info = securityByEp.get(endpoint);
        if (info == null) {
            info = addNew(endpoint);
        }
        return info;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        SecurityInfo info = securityByIdentity.get(identity);
        if (info == null) {
            info = addNew(identity);
        }
        return info;
    }

    private SecurityInfo addNew(String identity) {
        TypeServer keyValue = TypeServer.SERVER;
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, keyValue);
        if (store.getSecurityInfo() != null) {
            try {
                add(store.getSecurityInfo());
            } catch (NonUniqueSecurityInfoException e) {
                log.error("[{}] FAILED registration endpointId: [{}]", identity, e.getMessage());
            }
        }
        else {
            if (store.getSecurityMode() != NO_SEC.code) log.error("Registration failed: FORBIDDEN, endpointId: [{}]", identity);
        }
        return store.getSecurityInfo();
    }

//    private ReadResultSecurityStore getSecurityInfo(String identity) {
//        CountDownLatch latch = new CountDownLatch(1);
//        final ReadResultSecurityStore[] resultSecurityStore = new ReadResultSecurityStore[1];
//        context.getTransportService().process(TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(identity).build(),
//                new TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg>() {
//                    @Override
//                    public void onSuccess(TransportProtos.ValidateDeviceCredentialsResponseMsg msg) {
//                        String ingfosStr = msg.getCredentialsBody();
//                        resultSecurityStore[0] = credentials.getSecurityInfo(msg.getDeviceInfo().getDeviceName(), ingfosStr);
//                        if (resultSecurityStore[0].getSecurityMode() < DEFAULT_MODE.code) {
//                            context.getSessions().put(msg.getDeviceInfo().getDeviceName(), msg);
//                        }
//                        latch.countDown();
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        log.trace("[{}] Failed to process credentials PSK: {}", identity, e);
//                        resultSecurityStore[0]  = credentials.getSecurityInfo(identity, null);
//                        latch.countDown();
//                    }
//                });
//        try {
//            latch.await(context.getTimeout(), TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return resultSecurityStore[0] ;
//    }
}
