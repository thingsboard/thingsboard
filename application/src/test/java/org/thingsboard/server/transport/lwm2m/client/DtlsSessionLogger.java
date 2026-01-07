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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.dtls.ClientHandshaker;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.SessionAdapter;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState;

import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_READ_CONNECTION_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_WRITE_CONNECTION_ID;

@Slf4j
public class DtlsSessionLogger extends SessionAdapter {

    private final Set<LwM2MClientState> clientStates;
    private final Map<LwM2MClientState, Integer> clientDtlsCid;

    public DtlsSessionLogger(Set<LwM2MClientState> clientStates, Map<LwM2MClientState, Integer> clientDtlsCid) {
        this.clientStates = clientStates;
        this.clientDtlsCid = clientDtlsCid;
    }

    @Override
    public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
        if (handshaker instanceof ClientHandshaker) {
            log.info("DTLS Full Handshake initiated by client : STARTED ...");
        }
    }

    @Override
    public void contextEstablished(Handshaker handshaker, DTLSContext establishedContext) throws HandshakeException {
        if (handshaker instanceof ClientHandshaker) {
            log.warn("DTLS initiated by client: SUCCEED, WriteConnectionId: [{}], ReadConnectionId: [{}]", establishedContext.getWriteConnectionId(), establishedContext.getReadConnectionId());
            clientStates.add(ON_WRITE_CONNECTION_ID);
            clientStates.add(ON_READ_CONNECTION_ID);
            Integer lenWrite = establishedContext.getWriteConnectionId() == null ? null : establishedContext.getWriteConnectionId().getBytes().length;
            Integer lenRead = establishedContext.getReadConnectionId() == null ? null : establishedContext.getReadConnectionId().getBytes().length;
            clientDtlsCid.put(ON_WRITE_CONNECTION_ID, lenWrite);
            clientDtlsCid.put(ON_READ_CONNECTION_ID, lenRead);
        }
    }

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

        if (handshaker instanceof ClientHandshaker) {
            log.info("DTLS Full Handshake initiated by client : FAILED ({})", cause);
        }
    }
}
