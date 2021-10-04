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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.ResumingClientHandshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.eclipse.californium.scandium.dtls.SessionAdapter;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.leshan.core.californium.DefaultEndpointFactory;

@Slf4j
public class TbLwM2MEndpointFactory extends DefaultEndpointFactory {

    public TbLwM2MEndpointFactory(String loggingTag) {
        super(loggingTag);
    }

    @Override
    protected Connector createSecuredConnector(DtlsConnectorConfig dtlsConfig) {

        return new DTLSConnector(dtlsConfig) {
            @Override
            protected void onInitializeHandshaker(Handshaker handshaker) {
                handshaker.addSessionListener(new SessionAdapter() {

                    private SessionId sessionIdentifier = null;

                    @Override
                    public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
                        if (handshaker instanceof ResumingServerHandshaker) {
                            if (sessionIdentifier != null && sessionIdentifier
                                    .equals(handshaker.getSession().getSessionIdentifier())) {
                                log.trace("DTLS abbreviated Handshake initiated by server : SUCCEED: [{}]", handshaker.getConnection().getPeerAddress());
                            } else {
                                sessionIdentifier = handshaker.getSession().getSessionIdentifier();
                                log.trace("DTLS abbreviated Handshake initiated by server : SUCCEED: [{}]", handshaker.getConnection().getPeerAddress());
                            }
                        } else if (handshaker instanceof ServerHandshaker) {
                            log.trace("DTLS Full Handshake initiated by server : SUCCEED: [{}]", handshaker.getConnection().getPeerAddress());
                        } else if (handshaker instanceof ResumingClientHandshaker) {
                        }
                    }

                    @Override
                    public void sessionEstablished(Handshaker handshaker, DTLSSession establishedSession) throws HandshakeException {}

                    @Override
                    public void handshakeFailed(Handshaker handshaker, Throwable error) {
                        // get cause
                        String cause;
                        if (error != null) {
                            if (error.getMessage() != null) {
                                cause = error.getMessage();
                            } else {
                                cause = error.getClass().getName();
                            }
                        } else {
                            cause = "unknown cause";
                        }

                        if (handshaker instanceof ResumingServerHandshaker) {
                            log.error("DTLS abbreviated Handshake initiated by server : FAILED [{}]", cause);
                        } else if (handshaker instanceof ServerHandshaker) {
                            PskPublicInformation pskPublicInformation = ((ServerHandshaker) handshaker).getPreSharedKeyIdentity();
                            if (pskPublicInformation != null) {
                                log.error("DTLS Full Handshake initiated by server FAILED: peerAddress [{}], pskIdentity: [{}], cause [{}]", handshaker.getPeerAddress(), pskPublicInformation, cause);
                            }
                            else {
                                log.error("DTLS Full Handshake initiated by server FAILED: peerAddress [{}], cause [{}]", handshaker.getPeerAddress(), cause);
                            }
                        }
                    }
                });
            }
        };

    }
}
