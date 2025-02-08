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
package org.thingsboard.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.PskSecretResult;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedMultiPskStore;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.eclipse.californium.scandium.util.ServerNames;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import javax.crypto.SecretKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TbAdvancedMultiPskStore extends AdvancedMultiPskStore {

    private PskPublicInformation identity;
    private byte[] key;
    private TransportService transportService;

    /**
     * Create simple store with initial credentials.
     *
     * @param identity PSK identity
     * @param key      PSK secret key
     */
    public TbAdvancedMultiPskStore(TransportService transportService, String identity, byte[] key) {
        this.identity = new PskPublicInformation(identity);
        this.key = key;
        this.transportService = transportService;
        this.setKey(this.identity, this.key);
    }

    @Override
    public PskSecretResult requestPskSecretResult(ConnectionId cid, ServerNames serverName,
                                                  PskPublicInformation identityToken, String hmacAlgorithm, SecretKey otherSecret, byte[] seed, boolean useExtendedMasterSecret) {
        String accessToken = identityToken.getPublicInfoAsString();
        if (this.identity.equals(identityToken)) {
            throw new RuntimeException("Coaps AccessToken + DTLS, token [" + identityToken + "] fails, token equals server token!");
        }
        try {
            final ValidateDeviceCredentialsResponse[] deviceCredentialsResponse = new ValidateDeviceCredentialsResponse[1];
            CountDownLatch latch = new CountDownLatch(1);
            transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(accessToken).build(),
                    new TransportServiceCallback<>() {
                        @Override
                        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                            if (msg.hasDeviceInfo()) {
                                deviceCredentialsResponse[0] = msg;
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.error(e.getMessage(), e);
                            latch.countDown();
                        }
                    });
            latch.await(10, TimeUnit.SECONDS);
            ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
            if (msg != null) {
                return new PskSecretResult(cid, new PskPublicInformation(accessToken), SecretUtil.create(this.key, "PSK"));
            } else {
                throw new RuntimeException("Coaps AccessToken + DTLS, token [" + identityToken + "] fails!");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
