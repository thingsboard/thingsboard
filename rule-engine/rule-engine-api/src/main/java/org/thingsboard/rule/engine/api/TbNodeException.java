// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import lombok.Getter;
import org.thingsboard.common.util.RecoveryAware;

public class TbNodeException extends Exception implements RecoveryAware {

    @Getter
    private final boolean unrecoverable;

    public TbNodeException(String message) {
        this(message, false);
    }

    public TbNodeException(String message, boolean unrecoverable) {
        super(message);
        this.unrecoverable = unrecoverable;
    }

    public TbNodeException(Exception e) {
        this(e, false);
    }

    public TbNodeException(Exception e, boolean unrecoverable) {
        super(e);
        this.unrecoverable = unrecoverable;
    }

}
