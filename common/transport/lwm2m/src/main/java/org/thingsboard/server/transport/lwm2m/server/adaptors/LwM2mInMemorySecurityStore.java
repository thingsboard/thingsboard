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
package org.thingsboard.server.transport.lwm2m.server.adaptors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportCtx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("LwM2mInMemorySecurityStore")
public class LwM2mInMemorySecurityStore extends InMemorySecurityStore {

    @Autowired
    public LwM2MTransportCtx context;

    @Autowired
    private LwM2MCredentials credentials;

    @Override
    public SecurityInfo getByIdentity(String identity) {
        SecurityInfo info = securityByIdentity.get(identity);
        if (info == null) {
            info = setSecurityInfo(identity);
            try {
                add(info);
            } catch (NonUniqueSecurityInfoException e) {
                log.error("[{}] FAILED registration endpointId: [{}]", identity, e.getMessage() );
            }
        }
        return info;
    }

    private SecurityInfo setSecurityInfo(String identity) {
        CountDownLatch latch = new CountDownLatch(1);
        final SecurityInfo[] securityInfo = new SecurityInfo[1];
        context.getTransportService().process(TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(identity).build(),
                new TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg>() {
                    @Override
                    public void onSuccess(TransportProtos.ValidateDeviceCredentialsResponseMsg msg) {
//                        log.info("Success to process credentials PSK: [{}]", msg);
                        String ingfosStr = msg.getCredentialsBody();
                        securityInfo[0] = credentials.getSecurityInfo(ingfosStr);
                        String endpointId = msg.getDeviceInfo().getDeviceName();
                        context.getSessions().put(endpointId, msg);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] Failed to process credentials PSK: {}", identity, e);
                        securityInfo[0] = null;
                        latch.countDown();
                    }
                });
        try {
            latch.await(context.getTimeout(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return securityInfo[0];
    }
}
