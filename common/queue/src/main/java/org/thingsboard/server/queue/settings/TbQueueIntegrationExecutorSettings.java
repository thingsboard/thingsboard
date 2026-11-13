// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.queue.util.PropertyUtils;

import javax.annotation.PostConstruct;
import java.util.Map;

@Lazy
@Data
@Component
public class TbQueueIntegrationExecutorSettings {

    @Value("${queue.integration.notifications_topic:tb_ie.notifications}")
    private String notificationsTopic;

    @Value("${queue.integration.poll_interval:25}")
    private long pollInterval;

    @Value("${queue.integration.pack-processing-timeout:10000}")
    private long packProcessingTimeout;

    @Value("${queue.integration.downlink_topic:tb_ie.downlink}")
    private String downlinkTopic;

    @Value("${queue.integration.downlink_topics:}")
    private String downlinkTopicProperties;

    @Value("${queue.integration.uplink_topic:tb_ie.uplink}")
    private String uplinkTopic;

    private Map<String, String> downlinkTopics;

    @PostConstruct
    private void init() {
        downlinkTopics = PropertyUtils.getProps(downlinkTopicProperties);
    }

    public String getIntegrationDownlinkTopic(IntegrationType it) {
        String defaultTopic = downlinkTopic + "." + it.name().toLowerCase();
        return downlinkTopics.getOrDefault(it.name(), defaultTopic);
    }

}
