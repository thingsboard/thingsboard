/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;

import org.thingsboard.server.common.data.id.SessionId;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Andrew Shvayka
 */
public class MqttSessionId implements SessionId {

    private static final AtomicLong idSeq = new AtomicLong();

    private final long id;

    public MqttSessionId() {
        this.id = idSeq.incrementAndGet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MqttSessionId that = (MqttSessionId) o;

        return id == that.id;

    }

    @Override
    public String toString() {
        return "MqttSessionId{" +
                "id=" + id +
                '}';
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toUidStr() {
        return "mqtt" + id;
    }
}
