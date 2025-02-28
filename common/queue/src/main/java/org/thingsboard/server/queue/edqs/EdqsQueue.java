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
package org.thingsboard.server.queue.edqs;

import lombok.Getter;

@Getter
public enum EdqsQueue {

    EVENTS("edqs.events", false, false),
    STATE("edqs.state", true, true);

    private final String topic;
    private final boolean readFromBeginning;
    private final boolean stopWhenRead;

    EdqsQueue(String topic, boolean readFromBeginning, boolean stopWhenRead) {
        this.topic = topic;
        this.readFromBeginning = readFromBeginning;
        this.stopWhenRead = stopWhenRead;
    }

}
