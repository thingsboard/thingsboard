/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.event;

import lombok.Data;

@Data
public class LifeCycleEvent implements EventFilter {
    private boolean isError;
    private String server;
    private String status;
    private String event;

    public void setIsError(boolean isError) {
        this.isError = isError;
    }

    @Override
    public EventType getEventType() {
        return EventType.LC_EVENT;
    }
}
