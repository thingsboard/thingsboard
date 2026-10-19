// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MUplinkTargetedCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

public class TbLwM2MWriteResponseCompositeCallback extends TbLwM2MUplinkTargetedCallback<WriteCompositeRequest, WriteCompositeResponse> {

    public TbLwM2MWriteResponseCompositeCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    @Override
    public void onSuccess(WriteCompositeRequest request, WriteCompositeResponse response) {
        super.onSuccess(request, response);
        handler.onWriteCompositeResponseOk(client, request, response.getCode().getCode());
    }

}
