/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;

@Slf4j
public class RuleEngineException extends Exception {
    protected static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    private final long ts;

    public RuleEngineException(String message) {
        this(message, null);
    }

    public RuleEngineException(String message, Throwable t) {
        super(message != null ? message : "Unknown", t);
        ts = System.currentTimeMillis();
    }

    public String toJsonString(int maxMessageLength) {
        try {
            return mapper.writeValueAsString(mapper.createObjectNode()
                    .put("message", truncateIfNecessary(getMessage(), maxMessageLength)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exception ", e);
            throw new RuntimeException(e);
        }
    }

    protected String truncateIfNecessary(String message, int maxMessageLength) {
        return StringUtils.truncate(message, maxMessageLength);
    }

}
