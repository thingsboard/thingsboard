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
package org.thingsboard.monitoring.config.transport;

import lombok.Data;
import org.thingsboard.monitoring.data.notification.ShortNameProvider;

@Data
public class RpcInfo implements ShortNameProvider {

    public static final String RPC_SUFFIX = " RPC";

    private final TransportInfo transportInfo;

    @Override
    public String getShortName() {
        return transportInfo.getShortName() + RPC_SUFFIX;
    }

    @Override
    public String toString() {
        return transportInfo.toString() + RPC_SUFFIX;
    }

}
