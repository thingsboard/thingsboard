/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
public class TbQueueRemoteJsInvokeSettings {
    @Value("${queue.js.request_topic}")
    private String requestTopic;

    @Value("${queue.js.response_topic_prefix}")
    private String responseTopic;

    @Value("${queue.js.max_pending_requests}")
    private long maxPendingRequests;

    @Value("${queue.js.response_poll_interval}")
    private int responsePollInterval;

    @Value("${queue.js.max_requests_timeout}")
    private long maxRequestsTimeout;
}
