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
package org.thingsboard.server.extensions.livy.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.livy.action.SampleSparkAppAction;

@Plugin(name = "Livy Plugin", actions = {SampleSparkAppAction.class},
        descriptor = "LivyPluginDescriptor.json", configuration = LivyPluginConfiguration.class)
@Slf4j
public class LivyPlugin extends AbstractPlugin<LivyPluginConfiguration> {

    private static final String BASE_URL_TEMPLATE = "http://%s:%d";
    private LivyPluginConfiguration configuration;
    private LivyMessageHandler handler;
    private HttpHeaders headers = new HttpHeaders();
    private String baseUrl;

    @Override
    public void init(LivyPluginConfiguration configuration) {
        this.configuration = configuration;
        this.baseUrl = String.format(
                BASE_URL_TEMPLATE,
                configuration.getHost(),
                configuration.getPort());
        if (configuration.getHeaders() != null) {
            configuration.getHeaders().forEach(h -> {
                log.debug("Adding header to request object. Key = {}, Value = {}", h.getKey(), h.getValue());
                this.headers.add(h.getKey(), h.getValue());
            });
        }
        init();
    }

    private void init() {
        this.handler = new LivyMessageHandler(baseUrl, headers);
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        log.debug("Suspend method was called, but no impl provided!");
    }

    @Override
    public void stop(PluginContext ctx) {
        log.debug("Stop method was called, but no impl provided!");
    }
}
