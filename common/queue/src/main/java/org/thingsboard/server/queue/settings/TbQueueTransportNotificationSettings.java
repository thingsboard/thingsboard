// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class TbQueueTransportNotificationSettings {

    @Value("${queue.transport.notifications_topic:tb_transport.notifications}")
    private String notificationsTopic;

    @Value("${queue.transport.poll_interval:25}")
    private long transportPollInterval;

}
