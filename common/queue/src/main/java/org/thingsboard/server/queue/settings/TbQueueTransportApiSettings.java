// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
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
