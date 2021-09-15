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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import java.util.Arrays;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2mTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public abstract class TbLwM2MTargetedCallback<R, T> extends AbstractTbLwM2MRequestCallback<R, T> {

    protected final String versionedId;
    protected final String[] versionedIds;

    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String versionedId) {
        super(logService, client);
        this.versionedId = versionedId;
        this.versionedIds = null;
    }

    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String[] versionedIds) {
        super(logService, client);
        this.versionedId = null;
        this.versionedIds = versionedIds;
    }

    @Override
    public void onSuccess(R request, T response) {
        //TODO convert camelCase to "camel case" using .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
        String requestName = request.getClass().getSimpleName();
        String responseStr =  response.toString();
        if (response instanceof ReadResponse) {
            responseStr = responseToString ((ReadResponse) response);
        }
        log.trace("[{}] {} [{}] successful: {}", client.getEndpoint(), requestName, versionedId != null ? versionedId : versionedIds, responseStr);
        logService.log(client, String.format("[%s]: %s [%s] successful. Result: [%s]", LOG_LWM2M_INFO, requestName, versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
    }

    private String responseToString (ReadResponse response) {
        if (response.getContent() instanceof LwM2mSingleResource) {
            if (((LwM2mSingleResource) response.getContent()).getType().name().equals("OPAQUE")) {
                if (((byte[])((LwM2mSingleResource) response.getContent()).getValue()).length > 0) {
                    int len = ((byte[])((LwM2mSingleResource) response.getContent()).getValue()).length;
                    String valueReplace = len + "Bytes";
                    String valueStr = Hex.encodeHexString((byte[]) (((LwM2mSingleResource) response.getContent()).getValue()));
                    return response.toString().replace(valueReplace, valueStr);
                }
            }
            return response.toString();
        }
        else {
           return response.toString();
        }
    }
}
