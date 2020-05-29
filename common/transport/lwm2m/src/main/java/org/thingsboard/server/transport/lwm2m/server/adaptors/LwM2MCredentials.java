/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.server.adaptors;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;

import javax.annotation.PostConstruct;
import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

@Slf4j
public class LwM2MCredentials {

    public SecurityInfo getSecurityInfo(String jsonStr) {
        SecurityInfo securityInfo = null;
        JsonObject object = validateJsonObjectFromString(jsonStr);
        if (!object.isJsonNull()) {
            String endpoint = null;
            if (object.has("endpoint")) {
                endpoint = object.get("endpoint").getAsString();
            } else {
                log.error("Missing endpoint");
            }
            JsonObject psk = (object.has("psk") && !object.get("psk").isJsonNull() && object.get("psk").isJsonObject()) ? object.get("psk").getAsJsonObject() : null;
            JsonObject rpk = (object.has("rpk") && !object.get("rpk").isJsonNull() && object.get("rpk").isJsonObject()) ? object.get("rpk").getAsJsonObject() : null;
            boolean x509 = (object.has("x509") && !object.get("x509").isJsonNull() && object.get("x509").isJsonPrimitive()) ? object.get("x509").getAsBoolean() : false;
            if (psk != null) {
                // PSK Deserialization
                String identity = null;
                if (psk.has("identity") && psk.get("identity").isJsonPrimitive()) {
                    identity = psk.get("identity").getAsString();
                } else {
                    log.error("Missing PSK identity");
                }
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
                securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint, identity, key);
            } else if (rpk != null) {
                PublicKey key = null;
                try {
                    if (rpk.has("key") && rpk.get("key").isJsonPrimitive()) {
                        byte[] rpkkey = Hex.decodeHex(rpk.get("key").getAsString().toCharArray());
                        key = SecurityUtil.publicKey.decode(rpkkey);
                    } else {
                        log.error("Missing RPK key");
                    }
                } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
                    log.error("RPK: Invalid security info content: " + e.getMessage());
                }
                securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint, key);
            } else if (x509) {
                securityInfo = SecurityInfo.newX509CertInfo(endpoint);
            } else {
                throw new JsonParseException("Invalid security info content");
            }
        }
        return securityInfo;
    }

    private JsonElement validateJsonFromString(String jsonStr) {
        try {
            return new JsonParser().parse(jsonStr);
        } catch (JsonParseException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    private JsonObject validateJsonObjectFromString(String jsonStr) {
        JsonObject jsonObject = null;
        JsonElement jsonElement = validateJsonFromString(jsonStr);
        if (jsonElement.isJsonObject()) {
            jsonObject = (JsonObject) jsonElement;
        }
        return jsonObject;
    }
}
