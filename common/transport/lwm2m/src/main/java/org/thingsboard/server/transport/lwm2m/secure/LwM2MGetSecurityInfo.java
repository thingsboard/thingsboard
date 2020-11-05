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
package org.thingsboard.server.transport.lwm2m.secure;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.StandardCharsets;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.common.transport.util.ProtoWithFSTService;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.bootstrap.LwM2MTransportContextBootstrap;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MJsonAdaptor;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Component("LwM2MGetSecurityInfo")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MGetSecurityInfo {

    @Autowired
    public LwM2MTransportContextServer contextS;

    @Autowired
    public LwM2MTransportContextBootstrap contextBS;

    @Autowired
    private LwM2MJsonAdaptor adaptor;

    public ReadResultSecurityStore getSecurityInfo(String endPoint, TypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final ReadResultSecurityStore[] resultSecurityStore = new ReadResultSecurityStore[1];
        contextS.getTransportService().process(ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(endPoint).build(),
                new TransportServiceCallback<ValidateDeviceCredentialsResponseMsg>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponseMsg msg) {
                        String ingfosStr = msg.getCredentialsBody();
                        Optional<DeviceProfile> deviceProfile = decode(msg.getProfileBody().toByteArray());
                        if (deviceProfile.isPresent()) {
                            DeviceProfile profile = deviceProfile.get();

                        }

                        //                        Optional<DeviceProfile> deviceProfile = decodeProfile(msg.getProfileBody().toByteArray());
                        resultSecurityStore[0] = putSecurityInfo(endPoint, msg.getDeviceInfo().getDeviceName(), ingfosStr, keyValue);
                        resultSecurityStore[0].setMsg(msg);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] Failed to process credentials PSK: {}", endPoint, e);
                        resultSecurityStore[0] = putSecurityInfo(endPoint, null, null, null);
                        latch.countDown();
                    }
                });
        try {
            latch.await(contextS.getCtxServer().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resultSecurityStore[0];
    }

    private ReadResultSecurityStore putSecurityInfo(String endPoint, String deviceName, String jsonStr, TypeServer keyValue) {
        ReadResultSecurityStore result = new ReadResultSecurityStore();
        JsonObject objectMsg = adaptor.validateJson(jsonStr);
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
                    result.setBootstrapJson(object);
                    result.setEndPoint(endPoint);
                } else {
                    LwM2MSecurityMode lwM2MSecurityMode = LwM2MSecurityMode.fromSecurityMode(object.get("securityConfigClientMode").getAsString().toLowerCase());
                    switch (lwM2MSecurityMode) {
                        case NO_SEC:
                            getClientSecurityInfoNoSec(result);
                            break;
                        case PSK:
                            getClientSecurityInfoPSK(result, endPoint, object);
                            break;
                        case RPK:
                            getClientSecurityInfoRPK(result, endPoint, object);
                            break;
                        case X509:
                            getClientSecurityInfoX509(result, endPoint);
                            break;
                        default:
                            break;
                    }

                }
            }
        }
        return result;
    }

    private void getClientSecurityInfoNoSec(ReadResultSecurityStore result) {
        result.setSecurityInfo(null);
        result.setSecurityMode(NO_SEC.code);
    }

    private void getClientSecurityInfoPSK(ReadResultSecurityStore result, String endPoint, JsonObject object) {
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

    private void getClientSecurityInfoRPK(ReadResultSecurityStore result, String endpoint, JsonObject object) {
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

    private void getClientSecurityInfoX509(ReadResultSecurityStore result, String endpoint) {
        result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
        result.setSecurityMode(X509.code);
    }

    private <T> Optional<T> decode(byte[] byteArray) {
        try {
            FSTConfiguration config = FSTConfiguration.createDefaultConfiguration();;
            T msg = (T) config.asObject(byteArray);
            return Optional.ofNullable(msg);
        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
            return Optional.empty();
        }
    }
}
