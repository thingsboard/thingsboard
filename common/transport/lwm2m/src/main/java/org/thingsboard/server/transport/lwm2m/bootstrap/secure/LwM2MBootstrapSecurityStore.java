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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.BOOTSTRAP_SERVER;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LWM2M_SERVER;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SERVERS;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.getBootstrapParametersFromThingsboard;

@Slf4j
@Service("LwM2MBootstrapSecurityStore")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' && '${transport.lwm2m.bootstrap.enable:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true' && '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    private final EditableBootstrapConfigStore bootstrapConfigStore;

    private final LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator;

    private final LwM2mTransportContext context;
    private final LwM2mTransportServerHelper helper;

    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore, LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator, LwM2mTransportContext context, LwM2mTransportServerHelper helper) {
        this.bootstrapConfigStore = bootstrapConfigStore;
        this.lwM2MCredentialsSecurityInfoValidator = lwM2MCredentialsSecurityInfoValidator;
        this.context = context;
        this.helper = helper;
    }

    @Override
    public Iterator<SecurityInfo> getAllByEndpoint(String endPoint) {
        TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endPoint, LwM2mTransportUtil.LwM2mTypeServer.BOOTSTRAP);
        if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            /* add value to store  from BootstrapJson */
            this.setBootstrapConfigScurityInfo(store);
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
                    for (String config : bootstrapConfigStore.getAll().keySet()) {
                        if (config.equals(endPoint)) {
                            bootstrapConfigStore.remove(config);
                        }
                    }
                    bootstrapConfigStore.add(endPoint, bsConfigNew);
                } catch (InvalidConfigurationException e) {
                    log.error("", e);
                }
                return store.getSecurityInfo() == null ? null : Collections.singletonList(store.getSecurityInfo()).iterator();
            }
        }
        return null;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(identity, LwM2mTransportUtil.LwM2mTypeServer.BOOTSTRAP);
        if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            /* add value to store  from BootstrapJson */
            this.setBootstrapConfigScurityInfo(store);
            BootstrapConfig bsConfig = store.getBootstrapConfig();
            if (bsConfig.security != null) {
                try {
                    bootstrapConfigStore.add(store.getEndpoint(), bsConfig);
                } catch (InvalidConfigurationException e) {
                    log.error("", e);
                }
                return store.getSecurityInfo();
            }
        }
        return null;
    }

    private void setBootstrapConfigScurityInfo(TbLwM2MSecurityInfo store) {
        /* BootstrapConfig */
        LwM2MBootstrapConfig lwM2MBootstrapConfig = this.getParametersBootstrap(store);
        if (lwM2MBootstrapConfig != null) {
            /* Security info */
            switch (lwM2MBootstrapConfig.getBootstrapServer().getSecurityMode()) {
                /* Use RPK only */
                case PSK:
                    store.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(store.getEndpoint(),
                            lwM2MBootstrapConfig.getBootstrapServer().getClientPublicKeyOrId(),
                            Hex.decodeHex(lwM2MBootstrapConfig.getBootstrapServer().getClientSecretKey().toCharArray())));
                    store.setSecurityMode(SecurityMode.PSK);
                    break;
                case RPK:
                    try {
                        store.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(store.getEndpoint(),
                                SecurityUtil.publicKey.decode(Hex.decodeHex(lwM2MBootstrapConfig.getBootstrapServer().getClientPublicKeyOrId().toCharArray()))));
                        store.setSecurityMode(SecurityMode.RPK);
                        break;
                    } catch (IOException | GeneralSecurityException e) {
                        log.error("Unable to decode Client public key for [{}]  [{}]", store.getEndpoint(), e.getMessage());
                    }
                case X509:
                    store.setSecurityInfo(SecurityInfo.newX509CertInfo(store.getEndpoint()));
                    store.setSecurityMode(SecurityMode.X509);
                    break;
                case NO_SEC:
                    store.setSecurityMode(SecurityMode.NO_SEC);
                    store.setSecurityInfo(null);
                    break;
                default:
            }
            BootstrapConfig bootstrapConfig = lwM2MBootstrapConfig.getLwM2MBootstrapConfig();
            store.setBootstrapConfig(bootstrapConfig);
        }
    }

    private LwM2MBootstrapConfig getParametersBootstrap(TbLwM2MSecurityInfo store) {
        try {
            LwM2MBootstrapConfig lwM2MBootstrapConfig = store.getBootstrapCredentialConfig();
            if (lwM2MBootstrapConfig != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonObject bootstrapObject = getBootstrapParametersFromThingsboard(store.getDeviceProfile());
                lwM2MBootstrapConfig.servers = mapper.readValue(bootstrapObject.get(SERVERS).toString(), LwM2MBootstrapServers.class);
                LwM2MServerBootstrap profileServerBootstrap = mapper.readValue(bootstrapObject.get(BOOTSTRAP_SERVER).toString(), LwM2MServerBootstrap.class);
                LwM2MServerBootstrap profileLwm2mServer = mapper.readValue(bootstrapObject.get(LWM2M_SERVER).toString(), LwM2MServerBootstrap.class);
                UUID sessionUUiD = UUID.randomUUID();
                TransportProtos.SessionInfoProto sessionInfo = helper.getValidateSessionInfo(store.getMsg(), sessionUUiD.getMostSignificantBits(), sessionUUiD.getLeastSignificantBits());
                context.getTransportService().registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(null, sessionInfo));
                if (this.getValidatedSecurityMode(lwM2MBootstrapConfig.bootstrapServer, profileServerBootstrap, lwM2MBootstrapConfig.lwm2mServer, profileLwm2mServer)) {
                    lwM2MBootstrapConfig.bootstrapServer = new LwM2MServerBootstrap(lwM2MBootstrapConfig.bootstrapServer, profileServerBootstrap);
                    lwM2MBootstrapConfig.lwm2mServer = new LwM2MServerBootstrap(lwM2MBootstrapConfig.lwm2mServer, profileLwm2mServer);
                    String logMsg = String.format("%s: getParametersBootstrap: %s Access connect client with bootstrap server.", LOG_LW2M_INFO, store.getEndpoint());
                    helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LW2M_TELEMETRY, logMsg), sessionInfo);
                    return lwM2MBootstrapConfig;
                } else {
                    log.error(" [{}] Different values SecurityMode between of client and profile.", store.getEndpoint());
                    log.error("{} getParametersBootstrap: [{}] Different values SecurityMode between of client and profile.", LOG_LW2M_ERROR, store.getEndpoint());
                    String logMsg = String.format("%s: getParametersBootstrap: %s Different values SecurityMode between of client and profile.", LOG_LW2M_ERROR, store.getEndpoint());
                    helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LW2M_TELEMETRY, logMsg), sessionInfo);
                    return null;
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to decode Json or Certificate for [{}]  [{}]", store.getEndpoint(), e.getMessage());
            return null;
        }
        log.error("Unable to decode Json or Certificate for [{}]", store.getEndpoint());
        return null;
    }

    /**
     * Bootstrap security have to sync between (bootstrapServer in credential and  bootstrapServer in profile)
     * and (lwm2mServer  in credential and lwm2mServer  in profile
     *
     * @param bootstrapFromCredential - Bootstrap -> Security of bootstrapServer in credential
     * @param profileServerBootstrap  - Bootstrap -> Security of bootstrapServer in profile
     * @param lwm2mFromCredential     - Bootstrap -> Security of lwm2mServer in credential
     * @param profileLwm2mServer      - Bootstrap -> Security of lwm2mServer in profile
     * @return false if not sync between SecurityMode of Bootstrap credential and profile
     */
    private boolean getValidatedSecurityMode(LwM2MServerBootstrap bootstrapFromCredential, LwM2MServerBootstrap profileServerBootstrap, LwM2MServerBootstrap lwm2mFromCredential, LwM2MServerBootstrap profileLwm2mServer) {
        return (bootstrapFromCredential.getSecurityMode().equals(profileServerBootstrap.getSecurityMode()) &&
                lwm2mFromCredential.getSecurityMode().equals(profileLwm2mServer.getSecurityMode()));
    }
}
