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
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStoreTaskProvider;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.transport.TransportService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public class LwM2mDefaultBootstrapSessionManager extends DefaultBootstrapSessionManager {

    private BootstrapSecurityStore bsSecurityStore;
    private SecurityChecker securityChecker;
    private BootstrapTaskProvider tasksProvider;
    private LwM2mBootstrapModelProvider modelProvider;
    private TransportService transportService;
    private String logMsg;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore, TransportService transportService) {
        this(bsSecurityStore, new SecurityChecker(), new BootstrapConfigStoreTaskProvider(configStore),
                new StandardBootstrapModelProvider());
        this.transportService = transportService;
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker,
                                               BootstrapTaskProvider tasksProvider, LwM2mBootstrapModelProvider modelProvider) {
        super(bsSecurityStore, securityChecker, tasksProvider, modelProvider);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.tasksProvider = tasksProvider;
        this.modelProvider = modelProvider;
    }

    @Override
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
        boolean authorized;
        Iterator<SecurityInfo> securityInfos;
        if (bsSecurityStore != null && securityChecker != null) {
            if (clientIdentity.isSecure() && clientIdentity.isPSK()) {
                securityInfos = bsSecurityStore.getAllByEndpoint(clientIdentity.getPskIdentity());
            } else {
                securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
            }
            authorized = securityChecker.checkSecurityInfos(request.getEndpointName(), clientIdentity, securityInfos);
        } else {
            authorized = true;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        if (authorized) {
            logMsg = String.format("%s: Bootstrap session started endpoint: %s, session: %s ", LOG_LWM2M_INFO, request.getEndpointName(),
                    session.toString());
            log.info(logMsg);
            transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(request.getEndpointName()), logMsg);
        }
        return session;
    }

    @Override
    public boolean hasConfigFor(BootstrapSession session) {
        BootstrapTaskProvider.Tasks firstTasks = tasksProvider.getTasks(session, null);
        if (firstTasks == null) return false;
        initTasks(session, firstTasks);
        return true;
    }

    protected void initTasks(BootstrapSession bssession, BootstrapTaskProvider.Tasks tasks) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bssession;
        // set models
        if (tasks.supportedObjects != null)
            session.setModel(modelProvider.getObjectModel(session, tasks.supportedObjects));

        // set Requests to Send
        session.setRequests(tasks.requestsToSend);

        // prepare list where we will store Responses
        session.setResponses(new ArrayList<LwM2mResponse>(tasks.requestsToSend.size()));

        // is last Tasks ?
        session.setMoreTasks(!tasks.last);
    }

    @Override
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession) {
        return nextRequest(bsSession);
    }

    protected BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest(BootstrapSession bsSession) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend = session.getRequests();

        if (!requestsToSend.isEmpty()) {
            // get next requests
            return requestsToSend.remove(0);
        } else {
            if (session.hasMoreTasks()) {
                BootstrapTaskProvider.Tasks nextTasks = tasksProvider.getTasks(session, session.getResponses());
                if (nextTasks == null) {
                    session.setMoreTasks(false);
                    return new BootstrapFinishRequest();
                }

                initTasks(session, nextTasks);
                return nextRequest(bsSession);
            } else {
                return new BootstrapFinishRequest();
            }
        }
    }

    @Override
    public BootstrapPolicy onResponseSuccess(BootstrapSession bsSession,
                                             BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            logMsg = String.format("%s: %s %s receives success response %s for %s : %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(), request.getPath().toString(), response.toString(), bsSession.toString(), request.toString());
            log.info(logMsg);
            transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
            // on success for NOT bootstrap finish request we send next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on success for bootstrap finish request we stop the session
            logMsg = String.format("%s: %s receives success response for bootstrap finish request and stop the session: %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(), bsSession.toString());
            log.info(logMsg);
            transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
            return BootstrapPolicy.finished();
        }
    }

    @Override
    public BootstrapPolicy onResponseError(BootstrapSession bsSession,
                                           BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            logMsg = String.format("%s: %s %s receives error response %s for %s : %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(),
                    request.getPath().toString(), response.toString(), bsSession.toString(), request.toString());
            log.info(logMsg);
            transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
            // on response error for NOT bootstrap finish request we continue any sending next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on response error for bootstrap finish request we stop the session
            logMsg = String.format("%s: %s %s error response %s for request %s bootstrap finish. Stop the session: %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(),
                    request.getPath().toString(), response.toString(), request.toString(), bsSession.toString());
            log.info(logMsg);
            return BootstrapPolicy.failed();
        }
    }

    @Override
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
                                            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        logMsg = String.format("%s: %s %s failed because of %s for %s : %s", LOG_LWM2M_INFO,
                request.getClass().getSimpleName(), request.getPath().toString(), cause.toString(), bsSession.toString(), request.toString());
        log.info(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
        return BootstrapPolicy.failed();
    }

    @Override
    public void end(BootstrapSession bsSession) {
        logMsg = String.format("%s: Bootstrap session finished : %s", LOG_LWM2M_INFO, bsSession.toString());
        log.info(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
    }

    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        logMsg = String.format("%s: Bootstrap session failed by %s: %s", LOG_LWM2M_INFO, cause.toString(), bsSession.toString());
        log.info(logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(bsSession.getEndpoint()), logMsg);
    }
}
