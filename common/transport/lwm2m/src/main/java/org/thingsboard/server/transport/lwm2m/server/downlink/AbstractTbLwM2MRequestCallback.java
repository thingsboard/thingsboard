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

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;

@Slf4j
public abstract class AbstractTbLwM2MRequestCallback<R, T> implements DownlinkRequestCallback<R, T> {

    protected final LwM2MTelemetryLogService logService;
    protected final LwM2mClient client;

    protected AbstractTbLwM2MRequestCallback(LwM2MTelemetryLogService logService, LwM2mClient client) {
        this.logService = logService;
        this.client = client;
    }

    @Override
    public void onValidationError(String params, String msg) {
        log.trace("[{}] Request [{}] validation failed. Reason: {}", client.getEndpoint(), params, msg);
        logService.log(client, String.format("[%s]: Request [%s] validation failed. Reason: %s", LOG_LWM2M_ERROR, params, msg));
    }

    @Override
    public void onError(String params, Exception e) {
        log.trace("[{}] Request [{}] processing failed", client.getEndpoint(), params, e);
        logService.log(client, String.format("[%s]: Request [%s] processing failed. Reason: %s", LOG_LWM2M_ERROR, params, e));
    }
}
