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
package org.thingsboard.server.common.msg.cluster;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
@Data
@EqualsAndHashCode
public class ServerAddress implements Comparable<ServerAddress>, Serializable {

    private final String host;
    private final int port;

    @Override
    public int compareTo(ServerAddress o) {
        int result = this.host.compareTo(o.host);
        if (result == 0) {
            result = this.port - o.port;
        }
        return result;
    }

    @Override
    public String toString() {
        return '[' + host + ':' + port + ']';
    }
}
