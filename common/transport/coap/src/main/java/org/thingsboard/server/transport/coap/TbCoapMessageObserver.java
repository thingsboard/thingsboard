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
package org.thingsboard.server.transport.coap;

import lombok.RequiredArgsConstructor;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.EndpointContext;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class TbCoapMessageObserver implements MessageObserver {

    private final int msgId;
    private final Consumer<Integer> onAcknowledge;
    private final Consumer<Integer> onTimeout;

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public void onRetransmission() {

    }

    @Override
    public void onResponse(Response response) {

    }

    @Override
    public void onAcknowledgement() {
        onAcknowledge.accept(msgId);
    }

    @Override
    public void onReject() {

    }

    @Override
    public void onTimeout() {
        if (onTimeout != null) {
            onTimeout.accept(msgId);
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onReadyToSend() {

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onDtlsRetransmission(int flight) {

    }

    @Override
    public void onSent(boolean retransmission) {

    }

    @Override
    public void onSendError(Throwable error) {

    }

    @Override
    public void onResponseHandlingError(Throwable cause) {

    }

    @Override
    public void onContextEstablished(EndpointContext endpointContext) {

    }

    @Override
    public void onTransferComplete() {

    }
}
