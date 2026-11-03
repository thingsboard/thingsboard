// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

public class TbRuleNodeUpdateException extends RuntimeException {

    private static final long serialVersionUID = 8209771144711980882L;

    public TbRuleNodeUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}

