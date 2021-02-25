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
package org.thingsboard.server.transport.lwm2m.secure;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContextServer;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.X509;

@Slf4j
@Component
@TbLwM2mTransportComponent
public class LwM2mCredentialsSecurityInfoValidator {

    private final LwM2mTransportContextServer contextS;

    public LwM2mCredentialsSecurityInfoValidator(LwM2mTransportContextServer contextS) {
        this.contextS = contextS;
    }

    /**
     * Request to thingsboard Response from thingsboard ValidateDeviceLwM2MCredentials
     * @param endpoint -
     * @param keyValue -
     * @return ValidateDeviceCredentialsResponseMsg and SecurityInfo
     */
    public ReadResultSecurityStore createAndValidateCredentialsSecurityInfo(String endpoint, TypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final ReadResultSecurityStore[] resultSecurityStore = new ReadResultSecurityStore[1];
        contextS.getTransportService().process(ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(endpoint).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponseMsg msg) {
                        String credentialsBody = msg.getCredentialsBody();
                        resultSecurityStore[0] = createSecurityInfo(endpoint, credentialsBody, keyValue);
                        resultSecurityStore[0].setMsg(msg);
                        Optional<DeviceProfile> deviceProfileOpt = LwM2mTransportHandler.decode(msg.getProfileBody().toByteArray());
                        deviceProfileOpt.ifPresent(profile -> resultSecurityStore[0].setDeviceProfile(profile));
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] [{}] Failed to process credentials ", endpoint, e);
                        resultSecurityStore[0] = createSecurityInfo(endpoint, null, null);
                        latch.countDown();
                    }
                });
        try {
            latch.await(contextS.getLwM2MTransportConfigServer().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to await credentials!", e);
        }
        return resultSecurityStore[0];
    }

    /**
     * Create new SecurityInfo
     * @param endPoint -
     * @param jsonStr -
     * @param keyValue -
     * @return SecurityInfo
     */
    private ReadResultSecurityStore createSecurityInfo(String endPoint, String jsonStr, TypeServer keyValue) {
        ReadResultSecurityStore result = new ReadResultSecurityStore();
        JsonObject objectMsg = LwM2mTransportHandler.validateJson(jsonStr);
        if (objectMsg != null && !objectMsg.isJsonNull()) {
            JsonObject object = (objectMsg.has(keyValue.type) && !objectMsg.get(keyValue.type).isJsonNull()) ? objectMsg.get(keyValue.type).getAsJsonObject() : null;
            /**
             * Only PSK
             */
            String endPointPsk = (objectMsg.has("client")
                    && objectMsg.get("client").getAsJsonObject().has("endpoint")
                    && objectMsg.get("client").getAsJsonObject().get("endpoint").isJsonPrimitive()) ? objectMsg.get("client").getAsJsonObject().get("endpoint").getAsString() : null;
            endPoint = (endPointPsk == null || endPointPsk.isEmpty()) ? endPoint : endPointPsk;
            if (object != null && !object.isJsonNull()) {
                if (keyValue.equals(TypeServer.BOOTSTRAP)) {
                    result.setBootstrapJsonCredential(object);
                    result.setEndPoint(endPoint);
                    result.setSecurityMode(LwM2MSecurityMode.fromSecurityMode(object.get("bootstrapServer").getAsJsonObject().get("securityMode").getAsString().toLowerCase()).code);
                } else {
                    LwM2MSecurityMode lwM2MSecurityMode = LwM2MSecurityMode.fromSecurityMode(object.get("securityConfigClientMode").getAsString().toLowerCase());
                    switch (lwM2MSecurityMode) {
                        case NO_SEC:
                            createClientSecurityInfoNoSec(result);
                            break;
                        case PSK:
                            createClientSecurityInfoPSK(result, endPoint, object);
                            break;
                        case RPK:
                            createClientSecurityInfoRPK(result, endPoint, object);
                            break;
                        case X509:
                            createClientSecurityInfoX509(result, endPoint);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return result;
    }

    private void createClientSecurityInfoNoSec(ReadResultSecurityStore result) {
        result.setSecurityInfo(null);
        result.setSecurityMode(NO_SEC.code);
    }

    private void createClientSecurityInfoPSK(ReadResultSecurityStore result, String endPoint, JsonObject object) {
        /** PSK Deserialization */
        String identity = (object.has("identity") && object.get("identity").isJsonPrimitive()) ? object.get("identity").getAsString() : null;
        if (identity != null && !identity.isEmpty()) {
            try {
                byte[] key = (object.has("key") && object.get("key").isJsonPrimitive()) ? Hex.decodeHex(object.get("key").getAsString().toCharArray()) : null;
                if (key != null && key.length > 0) {
                    if (endPoint != null && !endPoint.isEmpty()) {
                        result.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(endPoint, identity, key));
                        result.setSecurityMode(PSK.code);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Missing PSK key: " + e.getMessage());
            }
        } else {
            log.error("Missing PSK identity");
        }
    }

    private void createClientSecurityInfoRPK(ReadResultSecurityStore result, String endpoint, JsonObject object) {
        try {
            if (object.has("key") && object.get("key").isJsonPrimitive()) {
                byte[] rpkkey = Hex.decodeHex(object.get("key").getAsString().toLowerCase().toCharArray());
                PublicKey key = SecurityUtil.publicKey.decode(rpkkey);
                result.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(endpoint, key));
                result.setSecurityMode(RPK.code);
            } else {
                log.error("Missing RPK key");
            }
        } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
            log.error("RPK: Invalid security info content: " + e.getMessage());
        }
    }

    private void createClientSecurityInfoX509(ReadResultSecurityStore result, String endpoint) {
        result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
        result.setSecurityMode(X509.code);
    }
}
