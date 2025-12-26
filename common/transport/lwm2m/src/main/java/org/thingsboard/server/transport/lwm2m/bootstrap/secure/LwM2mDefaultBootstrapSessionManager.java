/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapConfigStoreTaskProvider;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapTaskProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public class LwM2mDefaultBootstrapSessionManager extends DefaultBootstrapSessionManager {

    private final BootstrapSecurityStore bsSecurityStore;
    private final SecurityChecker securityChecker;
    private final LwM2MBootstrapTaskProvider tasksProvider;
    private final LwM2mBootstrapModelProvider modelProvider;
    private TransportService transportService;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore, TransportService transportService) {
        this(bsSecurityStore, configStore, new SecurityChecker(), new LwM2MBootstrapConfigStoreTaskProvider(configStore),
                new StandardBootstrapModelProvider());
        this.transportService = transportService;
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore, SecurityChecker securityChecker,
                                               LwM2MBootstrapTaskProvider tasksProvider, LwM2mBootstrapModelProvider modelProvider) {
        super(bsSecurityStore, configStore);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.tasksProvider = tasksProvider;
        this.modelProvider = modelProvider;
    }

    @Override
    public BootstrapSession begin(BootstrapRequest request, LwM2mPeer sender, URI endpointUsed) {
        boolean authorized = sender.getIdentity().isSecure();
        Iterator<SecurityInfo> securityInfos = null;
          try {
            if (bsSecurityStore != null && securityChecker != null) {
                if (((IpPeer) sender).isPSK()) {
                    SecurityInfo securityInfo = bsSecurityStore.getByIdentity(((PskIdentity) sender.getIdentity()).getPskIdentity());
                    securityInfos = Collections.singletonList(securityInfo).iterator();
                }
                else if (!((IpPeer) sender).isX509()) {
                    securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
                }
                authorized = this.checkSecurityInfo(request.getEndpointName(), sender, securityInfos);
            }
        } catch (LwM2MAuthException e) {
            authorized = false;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, sender, authorized, null, endpointUsed);
        if (authorized) {
            try {
                this.tasksProvider.put(session.getEndpoint());
            } catch (InvalidConfigurationException e){
                log.error("Failed put to lwM2MBootstrapSessionClients by endpoint [{}]", request.getEndpointName(), e);
            }
            String msg = String.format("Bootstrap session started... %s", ((Request) request.getCoapRequest()).getLocalAddress().toString());
            log.warn(String.format("%s: %s", request.getEndpointName(), msg));
            this.sendLogs(request.getEndpointName(),
                    String.format("%s: %s", LOG_LWM2M_INFO, msg));
        }
        return session;
    }

    @Override
    public boolean hasConfigFor(BootstrapSession session) {
        BootstrapTaskProvider.Tasks firstTasks = this.tasksProvider.getTasks(session, null);
        if (firstTasks == null) {
            return false;
        }
        initTasks(session, firstTasks);
        return true;
    }

    protected void initTasks(BootstrapSession bssession, BootstrapTaskProvider.Tasks tasks) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bssession;
        // set models
        if (tasks.supportedObjects != null)
            session.setModel(modelProvider.getObjectModel(session, tasks.supportedObjects));

        // set Requests to Send
        log.warn("tasks.requestsToSend = [{}]", tasks.requestsToSend);
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
                BootstrapTaskProvider.Tasks nextTasks = this.tasksProvider.getTasks(session, session.getResponses());
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
            String msg = String.format("%s: receives success response for:  %s  %s %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(), request.getPath().toString(), response.toString());
            log.warn(msg);
            this.sendLogs(bsSession.getEndpoint(), msg);

            // on success for NOT bootstrap finish request we send next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on success for bootstrap finish request we stop the session
            String msg = String.format("%s: receives success response for bootstrap finish.", LOG_LWM2M_INFO);
            log.info(msg);
            this.sendLogs(bsSession.getEndpoint(), msg);
            this.tasksProvider.remove(bsSession.getEndpoint());
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
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: %s %s receives error response %s ", LOG_LWM2M_INFO,
                            request.getClass().getSimpleName(),
                            request.getPath().toString(), response.toString()));
            // on response error for NOT bootstrap finish request we continue any sending next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on response error for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: error response for request bootstrap finish. Stop the session: %s", LOG_LWM2M_ERROR, bsSession.toString()));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.failed();
        }
    }

    @Override
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
                                            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        this.sendLogs(bsSession.getEndpoint(),
                String.format("%s: %s %s failed because of %s", LOG_LWM2M_ERROR, request.getClass().getSimpleName(),
                        request.getPath().toString(), cause.toString()));
        return BootstrapPolicy.failed();
    }

    @Override
    public void end(BootstrapSession bsSession) {
        String msg = String.format("%s: Bootstrap session finished.", LOG_LWM2M_INFO);
        log.warn(msg);
        this.sendLogs(bsSession.getEndpoint(), msg);
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session failed because of %s", LOG_LWM2M_ERROR,
                cause.toString()));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    private void sendLogs(String endpointName, String logMsg) {
        log.info("Endpoint: [{}] [{}]", endpointName, logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(endpointName), logMsg);
    }

    private boolean checkSecurityInfo(String endpoint, LwM2mPeer clientIdentity, Iterator<SecurityInfo> securityInfos) {
        if (((IpPeer) clientIdentity).isX509()) {
            return ((X509Identity)clientIdentity.getIdentity()).getX509CommonName().equals(endpoint)
                    & ((LwM2MBootstrapSecurityStore) bsSecurityStore).getBootstrapConfigByEndpoint(endpoint) != null;
        } else {
            return securityChecker.checkSecurityInfos(endpoint, clientIdentity, securityInfos);
        }
    }
}
