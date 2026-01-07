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

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Schema
public class ThingsboardCredentialsViolationResponse extends ThingsboardErrorResponse {

    protected ThingsboardCredentialsViolationResponse(String message) {
        super(message, ThingsboardErrorCode.PASSWORD_VIOLATION, HttpStatus.UNAUTHORIZED);
    }

    public static ThingsboardCredentialsViolationResponse of(final String message) {
        return new ThingsboardCredentialsViolationResponse(message);
    }

}
