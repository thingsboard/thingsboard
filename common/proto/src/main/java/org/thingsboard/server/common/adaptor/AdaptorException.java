// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.adaptor;

public class AdaptorException extends Exception {

    private static final long serialVersionUID = 1L;

    public AdaptorException() {
        super();
    }

    public AdaptorException(String cause) {
        super(cause);
    }

    public AdaptorException(Exception cause) {
        super(cause);
    }

    public AdaptorException(String message, Exception cause) {
        super(message, cause);
    }

}
