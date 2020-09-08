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
package org.thingsboard.server.transport.mqtt.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class MqttTopicRegexUtil {

    public static Pattern toRegex(String topicFilter) {
        String regex = topicFilter
                .replace("\\", "\\\\")
                .replace("+", "[^/]+")
                .replace("/#", "($|/.*)");
        log.debug("Converting [{}] to [{}]", topicFilter, regex);
        return Pattern.compile(regex);
    }

}
