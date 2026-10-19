// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
