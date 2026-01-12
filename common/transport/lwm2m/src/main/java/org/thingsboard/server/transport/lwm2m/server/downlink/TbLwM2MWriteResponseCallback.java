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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

@Slf4j
public class TbLwM2MWriteResponseCallback extends TbLwM2MUplinkTargetedCallback<WriteRequest, WriteResponse> {

    public TbLwM2MWriteResponseCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    @Override
    public void onSuccess(WriteRequest request, WriteResponse response) {
        super.onSuccess(request, response);
        handler.onWriteResponseOk(client, versionedId, request, response.getCode().getCode());
    }

}
