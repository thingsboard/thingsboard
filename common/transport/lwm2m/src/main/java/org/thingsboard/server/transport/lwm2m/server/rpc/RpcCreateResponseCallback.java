// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.rpc;

import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.Optional;

public class RpcCreateResponseCallback<R extends LwM2mRequest<T>, T extends CreateResponse> extends RpcLwM2MDownlinkCallback<R, T> {

    public RpcCreateResponseCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected Optional<String> serializeSuccessfulResponse(T response) {
        String value = response.getLocation() != null ? "location=" + response.getLocation() : "";
        return Optional.of(value);
    }
}
