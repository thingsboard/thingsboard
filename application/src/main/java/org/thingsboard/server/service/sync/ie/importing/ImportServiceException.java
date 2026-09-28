// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing;

public class ImportServiceException extends RuntimeException {
    private static final long serialVersionUID = -4932715239522125041L;

    public ImportServiceException() {
    }

    public ImportServiceException(String message) {
        super(message);
    }
}
