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
package org.thingsboard.server.queue.rabbitmq;

import org.thingsboard.server.queue.TbQueueAdmin;

import java.util.HashMap;
import java.util.Map;

public class TbRabbitMqAdmin implements TbQueueAdmin {

    private final TbRabbitMqSettings rabbitMqSettings;
    private final Map<String, String> attributes = new HashMap<>();

    public TbRabbitMqAdmin(TbRabbitMqSettings rabbitMqSettings) {
        this.rabbitMqSettings = rabbitMqSettings;

    }

    @Override
    public void createTopicIfNotExists(String topic) {

    }
}
