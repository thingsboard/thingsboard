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
package org.thingsboard.server.dao.sql.rpc;

import org.thingsboard.server.dao.model.sql.RpcEntity;

/**
 * One queued RPC write, tagged with the statement it needs.
 */
public record RpcWrite(RpcEntity entity, RpcWrite.Op op) {

    public enum Op { INSERT, UPDATE }

    public static RpcWrite insert(RpcEntity entity) {
        return new RpcWrite(entity, Op.INSERT);
    }

    public static RpcWrite update(RpcEntity entity) {
        return new RpcWrite(entity, Op.UPDATE);
    }
}
