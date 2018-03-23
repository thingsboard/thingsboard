/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.actors.plugin.ValidationResult;
import org.thingsboard.server.actors.plugin.ValidationResultCode;
import org.thingsboard.server.extensions.api.exception.AccessDeniedException;
import org.thingsboard.server.extensions.api.exception.EntityNotFoundException;
import org.thingsboard.server.extensions.api.exception.InternalErrorException;
import org.thingsboard.server.extensions.api.exception.UnauthorizedException;

/**
 * Created by ashvayka on 21.02.17.
 */
public class ValidationCallback implements FutureCallback<ValidationResult> {

    private final DeferredResult<ResponseEntity> response;
    private final FutureCallback<DeferredResult<ResponseEntity>> action;

    public ValidationCallback(DeferredResult<ResponseEntity> response, FutureCallback<DeferredResult<ResponseEntity>> action) {
        this.response = response;
        this.action = action;
    }

    @Override
    public void onSuccess(ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        if (resultCode == ValidationResultCode.OK) {
            action.onSuccess(response);
        } else {
            Exception e;
            switch (resultCode) {
                case ENTITY_NOT_FOUND:
                    e = new EntityNotFoundException(result.getMessage());
                    break;
                case UNAUTHORIZED:
                    e = new UnauthorizedException(result.getMessage());
                    break;
                case ACCESS_DENIED:
                    e = new AccessDeniedException(result.getMessage());
                    break;
                case INTERNAL_ERROR:
                    e = new InternalErrorException(result.getMessage());
                    break;
                default:
                    e = new UnauthorizedException("Permission denied.");
                    break;
            }
            onFailure(e);
        }
    }

    @Override
    public void onFailure(Throwable e) {
        action.onFailure(e);
    }
}
