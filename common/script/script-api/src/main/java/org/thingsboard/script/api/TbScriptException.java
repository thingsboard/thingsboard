// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import lombok.Getter;
import org.thingsboard.common.util.RecoveryAware;

import java.io.Serial;
import java.util.UUID;

public class TbScriptException extends RuntimeException implements RecoveryAware {

    @Serial
    private static final long serialVersionUID = -1958193538782818284L;

    public enum ErrorCode {

        COMPILATION,
        TIMEOUT,
        RUNTIME,
        OTHER

    }

    @Getter
    private final UUID scriptId;
    @Getter
    private final ErrorCode errorCode;
    @Getter
    private final String body;

    public TbScriptException(UUID scriptId, ErrorCode errorCode, String body, Exception cause) {
        super(cause);
        this.scriptId = scriptId;
        this.errorCode = errorCode;
        this.body = body;
    }

    @Override
    public boolean isUnrecoverable() {
        return errorCode == ErrorCode.COMPILATION;
    }

}
