// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.AbstractTbLwM2MRequestCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public class TbLwM2MCancelObserveCompositeCallback extends AbstractTbLwM2MRequestCallback<TbLwM2MCancelObserveCompositeRequest, Integer> {

    private final String [] versionedIds;

    public TbLwM2MCancelObserveCompositeCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String [] versionedIds) {
        super(logService, client);
        this.versionedIds = versionedIds;
    }

    @Override
    public void onSuccess(TbLwM2MCancelObserveCompositeRequest request, Integer canceledSubscriptionsCount) {
        log.trace("[{}] Cancel composite observation of [{}] successful: {}", client.getEndpoint(),  this.versionedIds, canceledSubscriptionsCount);
        logService.log(client, String.format("[%s]: Cancel Composite Observe for [%s] successful. Result: [%s]", LOG_LWM2M_INFO, this.versionedIds, canceledSubscriptionsCount));
    }
}
