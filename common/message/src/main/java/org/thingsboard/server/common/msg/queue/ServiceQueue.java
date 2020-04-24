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
package org.thingsboard.server.common.msg.queue;

import lombok.ToString;

import java.util.Objects;

@ToString
public class ServiceQueue {

    public static final String MAIN = "Main";

    private final ServiceType type;
    private final String queue;

    public ServiceQueue(ServiceType type) {
        this.type = type;
        this.queue = MAIN;
    }

    public ServiceQueue(ServiceType type, String queue) {
        this.type = type;
        this.queue = queue;
    }

    public ServiceType getType() {
        return type;
    }

    public String getQueue() {
        return queue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceQueue that = (ServiceQueue) o;
        return type == that.type &&
                queue.equals(that.queue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, queue);
    }

}
