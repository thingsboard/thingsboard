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
package org.thingsboard.server.common.msg.rpc;

import lombok.Data;
import org.thingsboard.server.common.data.rpc.RpcError;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class FromDeviceRpcResponse implements Serializable {

    private static final long serialVersionUID = -3799452502112373491L;

    private final UUID id;
    private final String response;
    private final RpcError error;

    public Optional<String> getResponse() {
        return Optional.ofNullable(response);
    }

    public Optional<RpcError> getError() {
        return Optional.ofNullable(error);
    }

}
