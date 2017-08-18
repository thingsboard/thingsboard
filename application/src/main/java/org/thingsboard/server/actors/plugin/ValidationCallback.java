/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.thingsboard.server.extensions.api.exception.UnauthorizedException;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;

/**
 * Created by ashvayka on 21.02.17.
 */
public class ValidationCallback implements PluginCallback<Boolean> {

    private final PluginCallback<?> callback;
    private final Consumer<PluginContext> action;

    public ValidationCallback(PluginCallback<?> callback, Consumer<PluginContext> action) {
        this.callback = callback;
        this.action = action;
    }

    @Override
    public void onSuccess(PluginContext ctx, Boolean value) {
        if (value) {
            action.accept(ctx);
        } else {
            onFailure(ctx, new UnauthorizedException("Permission denied."));
        }
    }

    @Override
    public void onFailure(PluginContext ctx, Exception e) {
        callback.onFailure(ctx, e);
    }
}
