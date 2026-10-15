// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.exception;

public class BufferLimitException extends RuntimeException {

    private static final long serialVersionUID = 4513762009041887588L;

    public BufferLimitException() {
        super("Rate Limit Buffer is full");
    }
}
