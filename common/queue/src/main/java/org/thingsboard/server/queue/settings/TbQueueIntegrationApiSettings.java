// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class TbQueueIntegrationApiSettings {

    @Value("${queue.integration_api.requests_topic}")
    private String requestsTopic;

    @Value("${queue.integration_api.responses_topic}")
    private String responsesTopic;

    @Value("${queue.integration_api.max_pending_requests}")
    private int maxPendingRequests;

    @Value("${queue.integration_api.max_requests_timeout}")
    private int maxRequestsTimeout;

    @Value("${queue.integration_api.max_callback_threads}")
    private int maxCallbackThreads;

    @Value("${queue.integration_api.request_poll_interval}")
    private long requestPollInterval;

    @Value("${queue.integration_api.response_poll_interval}")
    private long responsePollInterval;

}
