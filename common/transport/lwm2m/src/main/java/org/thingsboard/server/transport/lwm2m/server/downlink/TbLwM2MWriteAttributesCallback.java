// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

public class TbLwM2MWriteAttributesCallback extends TbLwM2MTargetedCallback<WriteAttributesRequest, WriteAttributesResponse> {

    public TbLwM2MWriteAttributesCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(logService, client, targetId);
    }

}
