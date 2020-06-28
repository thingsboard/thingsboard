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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MGetSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("LwM2MBootstrapSecurityStore")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true'&& '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    private final EditableBootstrapConfigStore bootstrapConfigStore;
    private final String endpointSubIdent = "_identity";

    @Autowired
    LwM2MGetSecurityInfo lwM2MGetSecurityInfo;

    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore) {
        this.bootstrapConfigStore = bootstrapConfigStore;
    }

    @Override
    public List<SecurityInfo> getAllByEndpoint(String endpoint) {
        String endpointKey = endpoint;
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(endpointKey, TypeServer.BOOTSTRAP);
        if (store.getBootstrapJson() == null) {
            endpointKey = endpoint + this.endpointSubIdent;
            store = lwM2MGetSecurityInfo.getSecurityInfo(endpointKey, TypeServer.BOOTSTRAP);
        }
        if (store.getBootstrapJson() != null) {
            /** add value to store  from BootstrapJson */
            setBootstrapConfig_ScurityInfo(store);
//            loadFromFile(store, endpoint);
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
//                bootstrapConfigStore.getAll().keySet().removeIf(key -> key.equals(endpoint));
//                bootstrapConfigStore.getAll().entrySet().removeIf(entry -> entry.getKey().equals(endpoint));
                    for (String config : bootstrapConfigStore.getAll().keySet()) {
                        if (config.equals(endpoint)) {
                            bootstrapConfigStore.remove(config);
                        }
                    }
                    bootstrapConfigStore.add(endpoint, bsConfigNew);
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                return store.getSecurityInfo() == null ? null : Arrays.asList(store.getSecurityInfo());
            }
        }
        return null;
//        String jsonString =
//                "{\"server\":{\"x509\":{\"endpoint\":\"client_lwm2m_psk\",\"identity\":\"client_psk_identity\",\"key\":\"67f6aad1db5e9bdb9778a35e7f4f24f221c8646ce23cb8cf852fedee029cda9c\"}},\"bootstrap\":{\"client_lwm2m_psk\":{\"toDelete\":[\"/0\",\"/1\"],\"servers\":{\"0\":{\"shortId\":123,\"lifetime\":300,\"defaultMinPeriod\":1,\"notifIfDisabled\":true,\"binding\":\"U\"}},\"security\":{\"0\":{\"uri\":\"coaps://localhost:5688\",\"bootstrapServer\":true,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":111,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0},\"1\":{\"uri\":\"coaps://localhost:5686\",\"bootstrapServer\":false,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":123,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0}},\"acls\":{}}}}";
//            JSONObject jsonObject = new JSONObject(jsonString);
//            String json = "{ \"name\": \"Baeldung\", \"java\": true }";
//        JsonObject credentialDevice = new JsonParser().parse(jsonString).getAsJsonObject();
//        JsonObject bootstrapJson = credentialDevice.get("bootstrap").getAsJsonObject();
//        JsonObject identityJson = bootstrapJson.get("client_lwm2m_psk").getAsJsonObject();
//        Gson gson = new Gson ();
//        BootstrapConfig bsConfigNew = gson.fromJson(identityJson, BootstrapConfig.class);
//        try {
//            bootstrapConfigStore.add(endpoint, bsConfigNew);
//        } catch (InvalidConfigurationException e) {
//            e.printStackTrace();
//        }
//        BootstrapConfig bsConfig = bootstrapConfigStore.get(endpoint, null, null);
//
//        if (bsConfig == null || bsConfig.security == null)
//            return null;
//
//        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> bsEntry : bsConfig.security.entrySet()) {
//            BootstrapConfig.ServerSecurity value = bsEntry.getValue();

        // Extract PSK security info
//            if (store.getSecurityMode() == LwM2MSecurityMode.PSK.code) {
////                SecurityInfo securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
////                        new String(value.publicKeyOrId, StandardCharsets.UTF_8), value.secretKey);
//                return Arrays.asList(store.getSecurityInfo());
//            }
//            // Extract RPK security info
//            else if (store.getSecurityMode() == LwM2MSecurityMode.RPK.code) {
//                try {
//                    SecurityInfo securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint,
//                            SecurityUtil.publicKey.decode(value.publicKeyOrId));
//                    return Arrays.asList(securityInfo);
//                } catch (IOException | GeneralSecurityException e) {
//                    log.error("Unable to decode Client public key for [{}]  [{}]", endpoint, e.getMessage());
//                    return null;
//                }
//            }
//            // Extract X509 security info
//            else if (store.getSecurityMode() == LwM2MSecurityMode.X509.code) {
//                SecurityInfo securityInfo = SecurityInfo.newX509CertInfo(endpoint);
//                return Arrays.asList(securityInfo);
//            }
//        }

    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, TypeServer.BOOTSTRAP);
        /** add value to store  from BootstrapJson */
        setBootstrapConfig_ScurityInfo(store);
        if (store.getSecurityMode() < LwM2MSecurityMode.DEFAULT_MODE.code) {
            BootstrapConfig bsConfig = store.getBootstrapConfig();
            log.info("bsConfig: [{}]", bsConfig);
            if (bsConfig.security != null) {
                try {
                    bootstrapConfigStore.add(store.getEndPoint(), bsConfig);
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                return store.getSecurityInfo();
            }
        }

//        String jsonString =
//                "{\"server\":{\"psk\":{\"endpoint\":\"client_lwm2m_psk\",\"identity\":\"client_psk_identity\",\"key\":\"67f6aad1db5e9bdb9778a35e7f4f24f221c8646ce23cb8cf852fedee029cda9c\"}},\"bootstrap\":{\"client_lwm2m_psk\":{\"toDelete\":[\"/0\",\"/1\"],\"servers\":{\"0\":{\"shortId\":123,\"lifetime\":300,\"defaultMinPeriod\":1,\"notifIfDisabled\":true,\"binding\":\"U\"}},\"security\":{\"0\":{\"uri\":\"coaps://localhost:5688\",\"bootstrapServer\":true,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":111,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0},\"1\":{\"uri\":\"coaps://localhost:5686\",\"bootstrapServer\":false,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":123,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0}},\"acls\":{}}}}";
//            JSONObject jsonObject = new JSONObject(jsonString);
//            String json = "{ \"name\": \"Baeldung\", \"java\": true }";
//        JsonObject credentialDevice = new JsonParser().parse(jsonString).getAsJsonObject();
//        JsonObject bootstrapJson = credentialDevice.get("bootstrap").getAsJsonObject();
//        JsonObject identityJson = bootstrapJson.get("client_lwm2m_psk").getAsJsonObject();
//        Gson gson = new Gson ();
//        BootstrapConfig bsConfig = gson.fromJson(identityJson, BootstrapConfig.class);
//        log.info("bsConfig: [{}]", bsConfig);
//        if (bsConfig.security != null) {
//            for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> ec : bsConfig.security.entrySet()) {
//                BootstrapConfig.ServerSecurity serverSecurity = ec.getValue();
//                if (serverSecurity.bootstrapServer && serverSecurity.securityMode == SecurityMode.PSK
//                        && Arrays.equals(serverSecurity.publicKeyOrId, identityBytes)) {
//                    return SecurityInfo.newPreSharedKeyInfo("client_lwm2m_psk", identity, serverSecurity.secretKey);
//                }
//            }
//        }

//            ObjectMapper mapper = new ObjectMapper();
//
//            BootstrapConfig bsConfig = mapper.readValue(bootstrapJson, BootstrapConfig.class);

        //        BootstrapConfig bsConfig;
//        BootstrapConfig bsConfig = e.getValue();
        return null;
    }

    private void setBootstrapConfig_ScurityInfo(ReadResultSecurityStore store) {
        try {
            /** BootstrapConfig */
            JsonObject object = store.getBootstrapJson();
            ObjectMapper mapper = new ObjectMapper();
            LwM2MBootstrapConfig lwM2MBootstrapConfig = mapper.readValue(object.toString(), LwM2MBootstrapConfig.class);
            /** Security info */
            switch (SecurityMode.valueOf(lwM2MBootstrapConfig.getSecurityModeBootstrapBs())) {
                /** Use RPK only */
                case PSK:
                    lwM2MBootstrapConfig.setHostBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapSecureHost());
                    lwM2MBootstrapConfig.setPortBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapSecurePort());
                    lwM2MBootstrapConfig.setServerPublicBootstrapBs("");
                    store.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(store.getEndPoint(),
                            lwM2MBootstrapConfig.getClientPublicKeyOrIdBootstrapBs(),
                            Hex.decodeHex(lwM2MBootstrapConfig.getClientSecretKeyServerBs().toCharArray())));
//                            store.getBootstrapConfig().security.get(0).secretKey));
                    store.setSecurityMode(SecurityMode.PSK.code);
                    break;
                case RPK:
                    lwM2MBootstrapConfig.setHostBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapSecureHost());
                    lwM2MBootstrapConfig.setPortBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapSecurePort());
                    lwM2MBootstrapConfig.setServerPublicBootstrapBs(Hex.encodeHexString(lwM2MGetSecurityInfo.contextBS.getBootstrapPublicKey().getEncoded()));
                    try {
                        store.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(store.getEndPoint(),
                                SecurityUtil.publicKey.decode(Hex.decodeHex(lwM2MBootstrapConfig.getClientPublicKeyOrIdBootstrapBs().toCharArray()))));
                        store.setSecurityMode(SecurityMode.RPK.code);
                        break;
                    } catch (IOException | GeneralSecurityException e) {
                        log.error("Unable to decode Client public key for [{}]  [{}]", store.getEndPoint(), e.getMessage());
                    }
                case X509:
                    lwM2MBootstrapConfig.setHostBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapSecureHost());
                    lwM2MBootstrapConfig.setPortBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapSecurePort());
                    lwM2MBootstrapConfig.setServerPublicBootstrapBs(Hex.encodeHexString(lwM2MGetSecurityInfo.contextBS.getBootstrapCertificate().getEncoded()));
                    store.setSecurityInfo(SecurityInfo.newX509CertInfo(store.getEndPoint()));
                    store.setSecurityMode(SecurityMode.X509.code);
                    break;
                case NO_SEC:
                    lwM2MBootstrapConfig.setHostBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapHost());
                    lwM2MBootstrapConfig.setPortBootstrapBs(lwM2MGetSecurityInfo.contextBS.getBootstrapPort());
                    store.setSecurityMode(SecurityMode.NO_SEC.code);
                    store.setSecurityInfo(null);
                    break;
                default:
            }
            store.setBootstrapConfig(lwM2MBootstrapConfig.getLwM2MBootstrapConfig());

        } catch (JsonProcessingException | CertificateEncodingException e) {
            log.error("Unable to decode Json or Certificate for [{}]  [{}]", store.getEndPoint(), e.getMessage());
        }
    }

    // /////// For Integraation test File persistence
    // default location for persistence
    // public static final String DEFAULT_FILE = "data/bootstrap.json";
    private void loadFromFile(String endpoint) {
        try {
            File file = new File("data/bootstrap.json");
            if (file.exists()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();
                    Type gsonType = new TypeToken<Map<String, BootstrapConfig>>() {
                    }.getType();
                    Map<String, BootstrapConfig> configs = gson.fromJson(in, gsonType);
                    for (Map.Entry<String, BootstrapConfig> config : bootstrapConfigStore.getAll().entrySet()) {
                        if (config.getKey().equals(endpoint)) {
                            bootstrapConfigStore.remove(config.getKey());
                        }
                    }
                    for (Map.Entry<String, BootstrapConfig> config : configs.entrySet()) {
                        if (config.getKey().equals(endpoint)) {
                            bootstrapConfigStore.add(config.getKey(), config.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not load bootstrap infos from file [{}]", e.getMessage());
        }
    }

}
