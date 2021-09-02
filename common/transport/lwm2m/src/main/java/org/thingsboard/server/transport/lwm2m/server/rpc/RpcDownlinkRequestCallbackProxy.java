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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

public abstract class RpcDownlinkRequestCallbackProxy<R, T> implements DownlinkRequestCallback<R, T> {

    private final TransportService transportService;
    private final TransportProtos.ToDeviceRpcRequestMsg request;
    private final DownlinkRequestCallback<R, T> callback;

    protected final LwM2mClient client;

    public RpcDownlinkRequestCallbackProxy(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        this.transportService = transportService;
        this.client = client;
        this.request = requestMsg;
        this.callback = callback;
    }

    @Override
    public void onSent(R request) {
        transportService.process(client.getSession(), this.request, RpcStatus.SENT, TransportServiceCallback.EMPTY);
    }

    @Override
    public void onSuccess(R request, T response) {
        transportService.process(client.getSession(), this.request, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
        sendRpcReplyOnSuccess(response);
        if (callback != null) {
            callback.onSuccess(request, response);
        }
    }

    @Override
    public void onValidationError(String params, String msg) {
        sendRpcReplyOnValidationError(msg);
        if (callback != null) {
            callback.onValidationError(params, msg);
        }
    }

    @Override
    public void onError(String params, Exception e) {
        if (e instanceof java.util.concurrent.TimeoutException || e instanceof org.eclipse.leshan.core.request.exception.TimeoutException) {
            transportService.process(client.getSession(), this.request, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
        } else if (!(e instanceof ClientSleepingException)) {
            sendRpcReplyOnError(e);
        }
        if (callback != null) {
            callback.onError(params, e);
        }
    }

    protected void reply(LwM2MRpcResponseBody response) {
        TransportProtos.ToDeviceRpcResponseMsg.Builder msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(request.getRequestId());
        String responseAsString = JacksonUtil.toString(response);
        if (StringUtils.isEmpty(response.getError())) {
            msg.setPayload(responseAsString);
        } else {
            msg.setError(responseAsString);
        }
        transportService.process(client.getSession(), msg.build(), null);
    }

    abstract protected void sendRpcReplyOnSuccess(T response);

    protected void sendRpcReplyOnValidationError(String msg) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.BAD_REQUEST.getName()).error(msg).build());
    }

    protected void sendRpcReplyOnError(Exception e) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.INTERNAL_SERVER_ERROR.getName()).error(e.getMessage()).build());
    }

}
