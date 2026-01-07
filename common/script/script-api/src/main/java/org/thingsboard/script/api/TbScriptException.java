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
