// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.session.ex;

public class SessionException extends Exception {

    private static final long serialVersionUID = 1L;

    public SessionException(String msg){
        super(msg);
    }
    
    public SessionException(Exception cause){
        super(cause);
    }

    public SessionException(String msg, Exception cause){
        super(msg, cause);
    }


}
