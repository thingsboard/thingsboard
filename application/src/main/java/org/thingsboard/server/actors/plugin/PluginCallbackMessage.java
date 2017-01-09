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

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@ToString
public final class PluginCallbackMessage<V> {
    @Getter
    private final PluginCallback<V> callback;
    @Getter
    private final boolean success;
    @Getter
    private final V v;
    @Getter
    private final Exception e;

    public static <V> PluginCallbackMessage<V> onSuccess(PluginCallback<V> callback, V data) {
        return new PluginCallbackMessage<V>(true, callback, data, null);
    }

    public static <V> PluginCallbackMessage<V> onError(PluginCallback<V> callback, Exception e) {
        return new PluginCallbackMessage<V>(false, callback, null, e);
    }

    private PluginCallbackMessage(boolean success, PluginCallback<V> callback, V v, Exception e) {
        this.success = success;
        this.callback = callback;
        this.v = v;
        this.e = e;
    }
}
