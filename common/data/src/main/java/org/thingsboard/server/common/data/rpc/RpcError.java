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
package org.thingsboard.server.common.data.rpc;

/**
 * @author Andrew Shvayka
 */
public enum RpcError {
    NOT_FOUND, FORBIDDEN, NO_ACTIVE_CONNECTION, TIMEOUT, INTERNAL;

    private static final RpcError[] VALUES = values();

    /**
     * Resolves an {@link RpcError} from the proto {@code error} ordinal.
     * Returns {@code null} both for the "no error" sentinel (negative value) and for unknown ordinals
     * that a newer node in a mixed-version cluster might emit, so callers never hit an
     * {@link ArrayIndexOutOfBoundsException}.
     */
    public static RpcError fromProtoErrorCode(int errorCode) {
        return errorCode >= 0 && errorCode < VALUES.length ? VALUES[errorCode] : null;
    }
}
