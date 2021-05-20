/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class TbQueueTransportApiSettings {

    @Value("${queue.transport_api.requests_topic}")
    private String requestsTopic;

    @Value("${queue.transport_api.responses_topic}")
    private String responsesTopic;

    @Value("${queue.transport_api.max_pending_requests}")
    private int maxPendingRequests;

    @Value("${queue.transport_api.max_requests_timeout}")
    private int maxRequestsTimeout;

    @Value("${queue.transport_api.max_callback_threads}")
    private int maxCallbackThreads;

    @Value("${queue.transport_api.request_poll_interval}")
    private long requestPollInterval;

    @Value("${queue.transport_api.response_poll_interval}")
    private long responsePollInterval;

}
