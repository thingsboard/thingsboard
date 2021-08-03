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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;

import java.util.Collections;
import java.util.Iterator;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_TELEMETRY;

@Slf4j
public class LwM2mDefaultBootstrapSessionManager extends DefaultBootstrapSessionManager {

    private BootstrapSecurityStore bsSecurityStore;
    private SecurityChecker securityChecker;
    private TransportService transportService;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, TransportService transportService) {
        this(bsSecurityStore, new SecurityChecker());
        this.transportService = transportService;
    }

    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker) {
        super(bsSecurityStore);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
    }

    @SuppressWarnings("deprecation")
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
        boolean authorized;
        String logMsg;
        String securityInfosStr = "null";
        if (bsSecurityStore != null) {
            Iterator<SecurityInfo> securityInfos = (clientIdentity.getPskIdentity() != null && !clientIdentity.getPskIdentity().isEmpty()) ?
                    Collections.singletonList(bsSecurityStore.getByIdentity(clientIdentity.getPskIdentity())).iterator() : bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            securityInfosStr = securityInfos == null ? "null" : securityInfos.toString();
            authorized = securityChecker.checkSecurityInfos(request.getEndpointName(), clientIdentity, securityInfos);
        } else {
            authorized = true;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        if (authorized) {
            logMsg = String.format("%s: Bootstrap session started securityInfos: %s, session: %s ", LOG_LWM2M_INFO, securityInfosStr,
                    session.toString());
            log.trace(logMsg);
            transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(request.getEndpointName()), logMsg);
        }
        return session;
    }


    @Override
    public void end(BootstrapSession bsSession) {
        String logMsg = String.format("%s: Bootstrap session finished session: %s ", LOG_LWM2M_INFO, bsSession.toString());
        log.warn(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
    }

    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        String logMsg = String.format("%s: Bootstrap session failed by  %s: %s ", LOG_LWM2M_INFO, cause.toString(), bsSession.toString());
        log.warn(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
    }

    @Override
    public void onResponseSuccess(BootstrapSession bsSession,
                                  BootstrapDownlinkRequest<? extends LwM2mResponse> request) {
        String logMsg = "";
        if (request instanceof BootstrapFinishRequest) {
            logMsg = String.format("%s: %s receives success response  session: %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(), bsSession.toString());
        }
        else if(request instanceof BootstrapDeleteRequest) {
            logMsg = String.format("%s: %s %s receives success response for %s: %s", LOG_LWM2M_INFO, request.getClass().getSimpleName(),
                    request.getPath().toString(), bsSession.toString(), request.toString());
        }
        else if(request instanceof BootstrapWriteRequest) {
            logMsg = String.format("%s: %s %s receives success response for %s: %s", LOG_LWM2M_INFO, request.getClass().getSimpleName(),
                    request.getPath().toString(), bsSession.toString(), request.toString());
        }
        log.warn(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
    }

    @Override
    public BootstrapPolicy onResponseError(BootstrapSession bsSession,
                                           BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        String logMsg = String.format("%s: %s %s  receives error response %s for %s : %s", LOG_LWM2M_INFO,request.getClass().getSimpleName(),
                request.getPath().toString(), response.toString(), bsSession.toString(), request.toString());
        log.warn(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
        if (request instanceof BootstrapFinishRequest) {
            logMsg = String.format("%s: Bootstrap session stopped: %s", LOG_LWM2M_INFO, bsSession.toString());
            transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
            ((LwM2MBootstrapSecurityStore) bsSecurityStore).removeSessionByEndpoint(request.getPath().toString());
            return BootstrapPolicy.STOP;
        }
        return BootstrapPolicy.CONTINUE;
    }

    @Override
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
                                            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        String logMsg = String.format("%s: %s %s failed because of %s for %s: %s", LOG_LWM2M_INFO,
                request.getClass().getSimpleName(), request.getPath().toString(), cause.toString(), bsSession.toString(),
                request.toString());
        log.warn(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
        ((LwM2MBootstrapSecurityStore) bsSecurityStore).removeSessionByEndpoint(request.getPath().toString());
        return BootstrapPolicy.STOP;
    }
}
