// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MUplinkTargetedCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

@Slf4j
public class TbLwM2MReadCompositeCallback extends TbLwM2MUplinkTargetedCallback<ReadCompositeRequest, ReadCompositeResponse> {

    public TbLwM2MReadCompositeCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String[] versionedIds) {
        super(handler, logService, client, versionedIds);
    }

    @Override
    public void onSuccess(ReadCompositeRequest request, ReadCompositeResponse response) {
        super.onSuccess(request, response);
        handler.onUpdateValueAfterReadCompositeResponse(client.getRegistration(), response);
    }

}
