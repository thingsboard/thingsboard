// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
