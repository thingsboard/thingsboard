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
import com.google.gson.JsonObject;
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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.List;

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
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
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
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, TypeServer.BOOTSTRAP);
        /** add value to store  from BootstrapJson */
        setBootstrapConfig_ScurityInfo(store);
        if (store.getSecurityMode() < LwM2MSecurityMode.DEFAULT_MODE.code) {
            BootstrapConfig bsConfig = store.getBootstrapConfig();
            if (bsConfig.security != null) {
                try {
                    bootstrapConfigStore.add(store.getEndPoint(), bsConfig);
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                return store.getSecurityInfo();
            }
        }
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
}
