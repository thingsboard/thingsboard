/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.plugin;

import com.hazelcast.util.function.Consumer;
import org.thingsboard.server.extensions.api.exception.AccessDeniedException;
import org.thingsboard.server.extensions.api.exception.EntityNotFoundException;
import org.thingsboard.server.extensions.api.exception.InternalErrorException;
import org.thingsboard.server.extensions.api.exception.UnauthorizedException;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;

/**
 * Created by ashvayka on 21.02.17.
 */
public class ValidationCallback implements PluginCallback<ValidationResult> {

    private final PluginCallback<?> callback;
    private final Consumer<PluginContext> action;

    public ValidationCallback(PluginCallback<?> callback, Consumer<PluginContext> action) {
        this.callback = callback;
        this.action = action;
    }

    @Override
    public void onSuccess(PluginContext ctx, ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        if (resultCode == ValidationResultCode.OK) {
            action.accept(ctx);
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
            onFailure(ctx, e);
        }
    }

    @Override
    public void onFailure(PluginContext ctx, Exception e) {
        callback.onFailure(ctx, e);
    }
}
