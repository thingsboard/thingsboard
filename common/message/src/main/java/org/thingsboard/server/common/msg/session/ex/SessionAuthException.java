// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.session.ex;

public class SessionAuthException extends SessionException {

    private static final long serialVersionUID = 1L;
    
    public SessionAuthException(String msg) {
        super(msg);
    }

}
