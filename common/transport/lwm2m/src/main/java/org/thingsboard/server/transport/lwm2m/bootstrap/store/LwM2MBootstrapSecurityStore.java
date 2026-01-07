/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.BOOTSTRAP;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

@Slf4j
@Service("LwM2MBootstrapSecurityStore")
@TbLwM2mBootstrapTransportComponent
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    private final EditableBootstrapConfigStore bootstrapConfigStore;

    private final LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator;

    private final LwM2mTransportContext context;
    private final LwM2mTransportServerHelper helper;
    private final Map<String /* endpoint */, TransportProtos.SessionInfoProto> bsSessions = new ConcurrentHashMap<>();

    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore, LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator, LwM2mTransportContext context, LwM2mTransportServerHelper helper) {
        this.bootstrapConfigStore = bootstrapConfigStore;
        this.lwM2MCredentialsSecurityInfoValidator = lwM2MCredentialsSecurityInfoValidator;
        this.context = context;
        this.helper = helper;
    }

    @Override
    public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, BOOTSTRAP);
            SecurityInfo securityInfo = this.addValueToStore(store, endpoint);
            return securityInfo == null ? null : Collections.singletonList(store.getSecurityInfo()).iterator();
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        try {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(identity, BOOTSTRAP);
            if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
                /* add value to store  from BootstrapJson */
                this.setBootstrapConfigSecurityInfo(store);
                BootstrapConfig bsConfig = store.getBootstrapConfig();
                if (bsConfig.security != null) {
                    try {
                        bootstrapConfigStore.add(store.getEndpoint(), bsConfig);
                    } catch (InvalidConfigurationException e) {
                        log.trace("Invalid Bootstrap Configuration", e);
                        return null;
                    }
                }
            }
            return store.getSecurityInfo();
        } catch (LwM2MAuthException e) {
            log.trace("Bootstrap Registration failed: No pre-shared key found for [identity: {}]", identity);
            return null;
        }
    }

    @Override
    public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
        return null;
    }

    public TbLwM2MSecurityInfo getX509ByEndpoint(String endPoint) {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endPoint, BOOTSTRAP);
            this.addValueToStore(store, store.getEndpoint());
            return store;
    }


    private void setBootstrapConfigSecurityInfo(TbLwM2MSecurityInfo store) {
        /* BootstrapConfig */
        LwM2MBootstrapConfig lwM2MBootstrapConfig = this.getParametersBootstrap(store);
        if (lwM2MBootstrapConfig != null) {
            BootstrapConfig bootstrapConfig = lwM2MBootstrapConfig.getLwM2MBootstrapConfig();
            store.setBootstrapConfig(bootstrapConfig);
        }
    }

    private LwM2MBootstrapConfig getParametersBootstrap(TbLwM2MSecurityInfo store) {
        LwM2MBootstrapConfig lwM2MBootstrapConfig = store.getBootstrapCredentialConfig();
        if (lwM2MBootstrapConfig != null) {
            UUID sessionUUiD = UUID.randomUUID();
            TransportProtos.SessionInfoProto sessionInfo = helper.getValidateSessionInfo(store.getMsg(), sessionUUiD.getMostSignificantBits(), sessionUUiD.getLeastSignificantBits());
            bsSessions.put(store.getEndpoint(), sessionInfo);
            context.getTransportService().registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(null, null, null, sessionInfo, context.getTransportService()));
            if (this.getValidatedSecurityMode(lwM2MBootstrapConfig)) {
                return lwM2MBootstrapConfig;
            } else {
                log.error(" [{}] Different values SecurityMode between of client and profile.", store.getEndpoint());
                log.error("{} getParametersBootstrap: [{}] Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR, store.getEndpoint());
                String logMsg = String.format("%s: Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR);
                helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LWM2M_TELEMETRY, logMsg), sessionInfo, null);
                return null;
            }
        }
        log.error("Unable to decode Json or Certificate for [{}]", store.getEndpoint());
        return null;
    }

    /**
     * Bootstrap security have to sync between (bootstrapServer in credential and  bootstrapServer in profile)
     * and (lwm2mServer  in credential and lwm2mServer  in profile
     *
     * @return false if not sync between SecurityMode of Bootstrap credential and profile
     */
    private boolean getValidatedSecurityMode(LwM2MBootstrapConfig lwM2MBootstrapConfig) {
        LwM2MSecurityMode bootstrapServerSecurityMode = lwM2MBootstrapConfig.getBootstrapServer().getSecurityMode();
        LwM2MSecurityMode lwm2mServerSecurityMode = lwM2MBootstrapConfig.getLwm2mServer().getSecurityMode();
        AtomicBoolean validBs = new AtomicBoolean(true);
        AtomicBoolean validLw = new AtomicBoolean(true);
        lwM2MBootstrapConfig.getServerConfiguration().forEach(serverCredential -> {
            if (((AbstractLwM2MBootstrapServerCredential) serverCredential).isBootstrapServerIs()) {
                if (!bootstrapServerSecurityMode.equals(serverCredential.getSecurityMode())) {
                    validBs.set(false);
                }
            } else {
                if (!lwm2mServerSecurityMode.equals(serverCredential.getSecurityMode())) {
                    validLw.set(false);
                }
            }
        });
        return validBs.get() && validLw.get();
    }

    public TransportProtos.SessionInfoProto getSessionByEndpoint(String endpoint) {
        return bsSessions.get(endpoint);
    }

    public TransportProtos.SessionInfoProto removeSessionByEndpoint(String endpoint) {
        return bsSessions.remove(endpoint);
    }

    public BootstrapConfig getBootstrapConfigByEndpoint(String endpoint) {
        return bootstrapConfigStore.getAll().get(endpoint);
    }

    public SecurityInfo addValueToStore(TbLwM2MSecurityInfo store, String endpoint) {
        /* add value to store  from BootstrapJson */
        SecurityInfo securityInfo = null;
        if (store != null && store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            securityInfo = store.getSecurityInfo();
            this.setBootstrapConfigSecurityInfo(store);
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
                    boolean bootstrapServerUpdateEnable = ((Lwm2mDeviceProfileTransportConfiguration) store.getDeviceProfile().getProfileData().getTransportConfiguration()).isBootstrapServerUpdateEnable();
                    if (!bootstrapServerUpdateEnable) {
                        Optional<Map.Entry<Integer, BootstrapConfig.ServerSecurity>> securities = bsConfigNew.security.entrySet().stream().filter(sec -> sec.getValue().bootstrapServer).findAny();
                        if (securities.isPresent()) {
                            bsConfigNew.security.entrySet().remove(securities.get());
                            int serverSortId = securities.get().getValue().serverId;
                            Optional<Map.Entry<Integer, BootstrapConfig.ServerConfig>> serverConfigs = bsConfigNew.servers.entrySet().stream().filter(serv -> (serv.getValue()).shortId == serverSortId).findAny();
                            if (serverConfigs.isPresent()) {
                                bsConfigNew.servers.entrySet().remove(serverConfigs.get());
                            }
                        }
                    }
                    for (String config : bootstrapConfigStore.getAll().keySet()) {
                        if (config.equals(endpoint)) {
                            bootstrapConfigStore.remove(config);
                        }
                    }
                    bootstrapConfigStore.add(endpoint, bsConfigNew);
                } catch (InvalidConfigurationException e) {
                    if (e.getMessage().contains("Psk identity") && e.getMessage().contains("already used for this bootstrap server")) {
                        log.trace("Invalid Bootstrap Configuration", e);
                    } else {
                        log.error("Invalid Bootstrap Configuration", e);
                    }
                }
            }
        }
        return securityInfo;
    }
}
