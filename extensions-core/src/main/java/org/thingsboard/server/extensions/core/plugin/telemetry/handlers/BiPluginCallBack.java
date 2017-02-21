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
package org.thingsboard.server.extensions.core.plugin.telemetry.handlers;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;

/**
 * Created by ashvayka on 21.02.17.
 */
@Slf4j
public abstract class BiPluginCallBack<V1, V2> {

    private V1 v1;
    private V2 v2;

    public PluginCallback<V1> getV1Callback() {
        return new PluginCallback<V1>() {
            @Override
            public void onSuccess(PluginContext ctx, V1 value) {
                synchronized (BiPluginCallBack.this) {
                    BiPluginCallBack.this.v1 = value;
                    if (v2 != null) {
                        BiPluginCallBack.this.onSuccess(ctx, v1, v2);
                    }
                }
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                BiPluginCallBack.this.onFailure(ctx, e);
            }
        };
    }

    public PluginCallback<V2> getV2Callback() {
        return new PluginCallback<V2>() {
            @Override
            public void onSuccess(PluginContext ctx, V2 value) {
                synchronized (BiPluginCallBack.this) {
                    BiPluginCallBack.this.v2 = value;
                    if (v1 != null) {
                        BiPluginCallBack.this.onSuccess(ctx, v1, v2);
                    }
                }

            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                BiPluginCallBack.this.onFailure(ctx, e);
            }
        };
    }

    abstract public void onSuccess(PluginContext ctx, V1 v1, V2 v2);

    abstract public void onFailure(PluginContext ctx, Exception e);

}
