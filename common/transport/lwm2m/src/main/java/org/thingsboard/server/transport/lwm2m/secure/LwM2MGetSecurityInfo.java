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
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.bootstrap.LwM2MTransportContextBootstrap;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Component("LwM2MGetSecurityInfo")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MGetSecurityInfo {

    @Autowired
    public LwM2MTransportContextServer contextS;

    @Autowired
    public LwM2MTransportContextBootstrap contextBS;

    public ReadResultSecurityStore getSecurityInfo(String identity, TypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final ReadResultSecurityStore[] resultSecurityStore = new ReadResultSecurityStore[1];
        contextS.getTransportService().process(TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(identity).build(),
                new TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg>() {
                    @Override
                    public void onSuccess(TransportProtos.ValidateDeviceCredentialsResponseMsg msg) {
                        String ingfosStr = msg.getCredentialsBody();
                        resultSecurityStore[0] = putSecurityInfo(msg.getDeviceInfo().getDeviceName(), ingfosStr, keyValue);
                        if (resultSecurityStore[0].getSecurityMode() < DEFAULT_MODE.code && keyValue.equals(TypeServer.SERVER)) {
                            contextS.getSessions().put(msg.getDeviceInfo().getDeviceName(), msg);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] Failed to process credentials PSK: {}", identity, e);
//                        resultSecurityStore[0]  = credentials.getSecurityInfo(identity, null);
                        resultSecurityStore[0] = putSecurityInfo(identity, null, null);
                        latch.countDown();
                    }
                });
        try {
            latch.await(contextS.getTimeout(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resultSecurityStore[0];
    }

    private ReadResultSecurityStore putSecurityInfo(String endpoint, String jsonStr, TypeServer keyValue) {
        ReadResultSecurityStore result = new ReadResultSecurityStore();
        JsonObject objectMsg = validateJson(jsonStr);
        if (objectMsg != null && !objectMsg.isJsonNull()) {
            JsonObject object = (objectMsg.has(keyValue.type) && !objectMsg.get(keyValue.type).isJsonNull()) ? objectMsg.get(keyValue.type).getAsJsonObject() : null;
            if (object != null && !object.isJsonNull()) {
                if (keyValue.equals(TypeServer.SERVER)) {
                    if (!object.isJsonNull()) {
                        boolean isX509 = (object.has("x509") && !object.get("x509").isJsonNull() && object.get("x509").isJsonPrimitive()) ? object.get("x509").getAsBoolean() : false;
                        boolean isPsk = (object.has("psk") && !object.get("psk").isJsonNull()) ? true : false;
                        boolean isRpk = (!isX509 && object.has("rpk") && !object.get("rpk").isJsonNull()) ? true : false;
                        boolean isNoSec = (!isX509 && object.has("no_sec") && !object.get("no_sec").isJsonNull()) ? true : false;
                        if (isX509) {
                            result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
                            result.setSecurityMode(X509.code);
                        } else if (isPsk) {
                            /** PSK Deserialization */
                            JsonObject psk = (object.get("psk").isJsonObject()) ? object.get("psk").getAsJsonObject() : null;
                            if (!psk.isJsonNull()) {
                                String identity = null;
                                if (psk.has("identity") && psk.get("identity").isJsonPrimitive()) {
                                    identity = psk.get("identity").getAsString();
                                } else {
                                    log.error("Missing PSK identity");
                                }
                                if (identity != null && !identity.isEmpty()) {
                                    byte[] key = new byte[0];
                                    try {
                                        if (psk.has("key") && psk.get("key").isJsonPrimitive()) {
                                            key = Hex.decodeHex(psk.get("key").getAsString().toCharArray());
                                        } else {
                                            log.error("Missing PSK key");
                                        }
                                    } catch (IllegalArgumentException e) {
                                        log.error("Missing PSK key: " + e.getMessage());
                                    }
                                    if (key.length > 0) {
                                        if (psk.has("endpoint") && psk.get("endpoint").isJsonPrimitive()) {
                                            endpoint = psk.get("endpoint").getAsString();
                                            result.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(endpoint, identity, key));
                                            result.setSecurityMode(PSK.code);
                                        }
                                    }
                                }
                            }
                        } else if (isRpk) {
                            JsonObject rpk = (object.get("rpk").isJsonObject()) ? object.get("rpk").getAsJsonObject() : null;
                            if (!rpk.isJsonNull()) {
                                PublicKey key = null;
                                try {
                                    if (rpk.has("key") && rpk.get("key").isJsonPrimitive()) {
                                        byte[] rpkkey = Hex.decodeHex(rpk.get("key").getAsString().toLowerCase().toCharArray());
                                        key = SecurityUtil.publicKey.decode(rpkkey);
//                                        ECPublicKey ecPublicKey = (ECPublicKey) key;
//                                        log.info("params : [{}]", ecPublicKey.getParams().toString());
//                                        log.info("x : [{}]", Hex.encodeHexString(ecPublicKey.getW().getAffineX().toByteArray()));
//                                        log.info("y : [{}]", Hex.encodeHexString(ecPublicKey.getW().getAffineY().toByteArray()));
                                    } else {
                                        log.error("Missing RPK key");
                                    }
                                } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
                                    log.error("RPK: Invalid security info content: " + e.getMessage());
                                }
                                result.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(endpoint, key));
                                result.setSecurityMode(RPK.code);
                            }
                        } else if (isNoSec) {
                            JsonObject noSec = (object.isJsonObject()) ? object.getAsJsonObject() : null;
                            if (noSec.has("no_sec") && noSec.get("no_sec").isJsonPrimitive() && noSec.get("no_sec").getAsBoolean()) {
                                result.setSecurityInfo(null);
                                result.setSecurityMode(NO_SEC.code);
                            } else {
                                log.error("[{}] no sec error", endpoint);
                            }
                        }
                    }
                } else if (keyValue.equals(TypeServer.BOOTSTRAP)) {
                    result.setBootstrapJson(object);
                    result.setEndPoint(endpoint);
                }
            }
        }
        return result;
    }

    private JsonObject validateJson(String jsonStr) {
        JsonObject object = null;
        if (jsonStr != null && !jsonStr.isEmpty()) {
            String jsonValidFlesh = jsonStr.replaceAll("\\\\", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll("\n", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll("\t", "");
            jsonValidFlesh = jsonValidFlesh.replaceAll(" ", "");
            String jsonValid = (jsonValidFlesh.substring(0, 1).equals("\"") && jsonValidFlesh.substring(jsonValidFlesh.length() - 1).equals("\"")) ? jsonValidFlesh.substring(1, jsonValidFlesh.length() - 1) : jsonValidFlesh;
            try {
                object = new JsonParser().parse(jsonValid).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                log.error("[{}] Fail validateJson [{}]", jsonStr, e.getMessage());
            }
        }
        return object;
    }

}
