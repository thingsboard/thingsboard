// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
