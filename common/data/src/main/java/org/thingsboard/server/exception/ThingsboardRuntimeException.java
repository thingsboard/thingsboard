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
package org.thingsboard.server.exception;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

public class ThingsboardRuntimeException extends RuntimeException {

    private ThingsboardErrorCode errorCode;

    public ThingsboardRuntimeException() {
        super();
    }

    public ThingsboardRuntimeException(ThingsboardErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ThingsboardRuntimeException(String message, ThingsboardErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ThingsboardRuntimeException(String message, Throwable cause, ThingsboardErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ThingsboardRuntimeException(Throwable cause, ThingsboardErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public ThingsboardErrorCode getErrorCode() {
        return errorCode;
    }

}
