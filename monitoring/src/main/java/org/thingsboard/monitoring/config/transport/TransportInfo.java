/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.monitoring.config.transport;

import lombok.Data;

@Data
public class TransportInfo {

    private final TransportType transportType;
    private final String baseUrl;
    private final String queue;

    @Override
    public String toString() {
        if (queue.equals("Main")) {
            return String.format("*%s* (%s)", transportType.getName(), baseUrl);
        } else {
            return String.format("*%s* (%s) _%s_", transportType.getName(), baseUrl, queue);
        }
    }

}
