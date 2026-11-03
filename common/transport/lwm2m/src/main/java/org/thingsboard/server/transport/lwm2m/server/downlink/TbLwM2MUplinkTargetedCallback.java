// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

@Slf4j
public abstract class TbLwM2MUplinkTargetedCallback<R, T> extends TbLwM2MTargetedCallback<R, T> {

    protected LwM2mUplinkMsgHandler handler;

    public TbLwM2MUplinkTargetedCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String versionedId) {
        super(logService, client, versionedId);
        this.handler = handler;
    }

    public TbLwM2MUplinkTargetedCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String[] versionedIds) {
        super(logService, client, versionedIds);
        this.handler = handler;
    }

}
