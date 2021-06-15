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

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.Optional;

public class RpcReadResponseCallback<R extends LwM2mRequest<T>, T extends ReadResponse> extends RpcLwM2MDownlinkCallback<R, T> {

    private final String versionedId;

    public RpcReadResponseCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
        this.versionedId = versionedId;
    }

    @Override
    protected Optional<String> serializeSuccessfulResponse(T response) {
        Object value = null;
        if (response.getContent() instanceof LwM2mObject) {
            value = client.objectToString((LwM2mObject) response.getContent(), this.converter, versionedId);
        } else if (response.getContent() instanceof LwM2mObjectInstance) {
            value = client.instanceToString((LwM2mObjectInstance) response.getContent(), this.converter, versionedId);
        } else if (response.getContent() instanceof LwM2mResource) {
            value = client.resourceToString((LwM2mResource) response.getContent(), this.converter, versionedId);
        }
        return Optional.of(String.format("%s", value));
    }
}
