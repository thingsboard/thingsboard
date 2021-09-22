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
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2MUplinkMsgHandler;

@Slf4j
public class TbLwM2MReadCallback extends TbLwM2MUplinkTargetedCallback<ReadRequest, ReadResponse> {

    public TbLwM2MReadCallback(LwM2MUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2MClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    @Override
    public void onSuccess(ReadRequest request, ReadResponse response) {
        logForBadResponse(response.getCode().getCode(), responseToString (response), request.getClass().getSimpleName());
        super.onSuccess(request, response);
        handler.onUpdateValueAfterReadResponse(client.getRegistration(), versionedId, response);
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
