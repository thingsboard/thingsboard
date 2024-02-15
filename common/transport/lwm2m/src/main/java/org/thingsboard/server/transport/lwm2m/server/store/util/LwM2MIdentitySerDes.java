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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;

import java.security.PublicKey;

public class LwM2MIdentitySerDes {

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ID = "id";
    private static final String KEY_CN = "cn";
    private static final String KEY_RPK = "rpk";
    protected static final String KEY_LWM2MIDENTITY_TYPE = "type";
    protected static final String LWM2MIDENTITY_TYPE_UNSECURE = "unsecure";
    protected static final String LWM2MIDENTITY_TYPE_PSK = "psk";
    protected static final String LWM2MIDENTITY_TYPE_X509 = "x509";
    protected static final String LWM2MIDENTITY_TYPE_RPK = "rpk";

    public static JsonObject serialize(Identity identity) {
        JsonObject o = Json.object();

        if (identity.isPSK()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_PSK);
            o.set(KEY_ID, identity.getPskIdentity());
        } else if (identity.isRPK()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_RPK);
            PublicKey publicKey = identity.getRawPublicKey();
            o.set(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
        } else if (identity.isX509()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_X509);
            o.set(KEY_CN, identity.getX509CommonName());
        } else {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_UNSECURE);
            o.set(KEY_ADDRESS, identity.getPeerAddress().getHostString());
            o.set(KEY_PORT, identity.getPeerAddress().getPort());
        }
        return o;
    }

    public static Identity deserialize(JsonObject peer) {
        throw new NotImplementedException();
    }
}