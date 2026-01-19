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
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public class TbLwM2MCancelAllObserveCallback extends AbstractTbLwM2MRequestCallback<TbLwM2MCancelAllRequest, Integer> {

    public TbLwM2MCancelAllObserveCallback(LwM2MTelemetryLogService logService, LwM2mClient client) {
        super(logService, client);
    }

    @Override
    public void onSuccess(TbLwM2MCancelAllRequest request, Integer canceledSubscriptionsCount) {
        log.trace("[{}] Cancel of all observations was successful: {}", client.getEndpoint(),  canceledSubscriptionsCount);
        logService.log(client, String.format("[%s]: Cancel of all observations was successful. Result: [%s]", LOG_LWM2M_INFO, canceledSubscriptionsCount));
    }

}
