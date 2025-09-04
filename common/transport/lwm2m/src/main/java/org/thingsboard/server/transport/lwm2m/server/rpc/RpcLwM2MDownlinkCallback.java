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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.Optional;

public abstract class RpcLwM2MDownlinkCallback<R extends LwM2mRequest<T>, T extends LwM2mResponse> extends RpcDownlinkRequestCallbackProxy<R, T> {

    public RpcLwM2MDownlinkCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(T response) {
        LwM2MRpcResponseBody.LwM2MRpcResponseBodyBuilder builder = LwM2MRpcResponseBody.builder().result(response.getCode().getName());
        if (response.isSuccess()) {
            Optional<String> responseValue = serializeSuccessfulResponse(response);
            if (responseValue.isPresent() && StringUtils.isNotEmpty(responseValue.get())) {
                builder.value(responseValue.get());
            }
        } else {
            if (StringUtils.isNotEmpty(response.getErrorMessage())) {
                builder.error(response.getErrorMessage());
            }
        }
        reply(builder.build());
    }

    protected abstract Optional<String> serializeSuccessfulResponse(T response);
}
