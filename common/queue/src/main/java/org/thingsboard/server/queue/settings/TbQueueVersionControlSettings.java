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
public class TbQueueVersionControlSettings {

    @Value("${queue.vc.topic:tb_version_control}")
    private String topic;

    @Value("${queue.vc.usage-stats-topic:tb_usage_stats}")
    private String usageStatsTopic;

    @Value("${queue.vc.partitions:10}")
    private int partitions;
}
