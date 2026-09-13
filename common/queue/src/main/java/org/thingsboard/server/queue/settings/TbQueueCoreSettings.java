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
public class TbQueueCoreSettings {

    @Value("${queue.core.topic}")
    private String topic;

    @Value("${queue.core.ota.topic:tb_ota_package}")
    private String otaPackageTopic;

    @Value("${queue.core.usage-stats-topic:tb_usage_stats}")
    private String usageStatsTopic;

    @Value("${queue.core.housekeeper.topic:tb_housekeeper}")
    private String housekeeperTopic;

    @Value("${queue.core.housekeeper.reprocessing-topic:tb_housekeeper.reprocessing}")
    private String housekeeperReprocessingTopic;

    @Value("${queue.core.partitions}")
    private int partitions;
}
