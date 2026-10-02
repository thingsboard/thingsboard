// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Configuration(proxyBeanMethods = false)
@TbCoreComponent
class TbAiConfiguration {

    @Bean
    TbAiClient tbAiClient(@Value("${ai.base_url:https://ai.thingsboard.cloud}") String baseUrl) {
        return new TbAiClient(baseUrl);
    }

    @Bean
    TbAiChannelClient tbAiChannelClient(@Value("${ai.base_url:https://ai.thingsboard.cloud}") String baseUrl) {
        return new TbAiChannelClient(baseUrl);
    }

}
