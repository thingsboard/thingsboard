// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Configuration(proxyBeanMethods = false)
@TbCoreComponent
class TbAiConfiguration {

    // TB_AI_BASE_URL is read directly from an environment variable, not a yaml property: it is a
    // managed-service setting, not operator-tunable configuration.
    @Bean
    TbAiClient tbAiClient(@Value("${TB_AI_BASE_URL:https://ai.thingsboard.cloud}") String baseUrl) {
        return new TbAiClient(baseUrl);
    }

}
