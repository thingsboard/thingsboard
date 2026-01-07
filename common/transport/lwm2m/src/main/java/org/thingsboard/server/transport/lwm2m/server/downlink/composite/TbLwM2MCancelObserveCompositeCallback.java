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
