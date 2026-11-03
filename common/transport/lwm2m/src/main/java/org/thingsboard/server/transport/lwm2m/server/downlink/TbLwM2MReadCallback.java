// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

@Slf4j
public class TbLwM2MReadCallback extends TbLwM2MUplinkTargetedCallback<ReadRequest, ReadResponse> {

    public TbLwM2MReadCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    @Override
    public void onSuccess(ReadRequest request, ReadResponse response) {
        logForBadResponse(response.getCode().getCode(), responseToString(response), request.getClass().getSimpleName());
        handler.onUpdateValueAfterReadResponse(client.getRegistration(), versionedId, response);
    }

    private String responseToString(ReadResponse response) {
        if (response.getContent() instanceof LwM2mSingleResource) {
            LwM2mSingleResource singleResource = (LwM2mSingleResource) response.getContent();
            if (ResourceModel.Type.OPAQUE.equals(singleResource.getType())) {
                byte[] valueInBytes = (byte[]) singleResource.getValue();
                int len = valueInBytes.length;
                if (len > 0) {
                    String valueReplace = len + "Bytes";
                    String valueStr = Hex.encodeHexString(valueInBytes);
                    return response.toString().replace(valueReplace, valueStr);
                }
            }
        }
        return response.toString();
    }

}
