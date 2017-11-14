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
package org.thingsboard.server.extensions.rest.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.rest.action.RestApiCallPluginAction;

import java.util.Base64;

@Plugin(name = "REST API Call Plugin", actions = {RestApiCallPluginAction.class},
        descriptor = "RestApiCallPluginDescriptor.json", configuration = RestApiCallPluginConfiguration.class)
@Slf4j
public class RestApiCallPlugin extends AbstractPlugin<RestApiCallPluginConfiguration> {

    private static final String BASIC_AUTH_METHOD = "BASIC_AUTH";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String AUTHORIZATION_HEADER_FORMAT = "Basic %s";
    private static final String CREDENTIALS_TEMPLATE = "%s:%s";
    private static final String BASE_URL_TEMPLATE = "http://%s:%d%s";
    private RestApiCallMsgHandler handler;
    private String baseUrl;
    private HttpHeaders headers = new HttpHeaders();

    @Override
    public void init(RestApiCallPluginConfiguration configuration) {
        this.baseUrl = String.format(
                BASE_URL_TEMPLATE,
                configuration.getHost(),
                configuration.getPort(),
                configuration.getBasePath());

        if (configuration.getAuthMethod().equals(BASIC_AUTH_METHOD)) {
            String userName = configuration.getUserName();
            String password = configuration.getPassword();
            String credentials = String.format(CREDENTIALS_TEMPLATE, userName, password);
            byte[] token = Base64.getEncoder().encode(credentials.getBytes());
            this.headers.add(AUTHORIZATION_HEADER_NAME, String.format(AUTHORIZATION_HEADER_FORMAT, new String(token)));
        }

        if (configuration.getHeaders() != null) {
            configuration.getHeaders().forEach(h -> {
                log.debug("Adding header to request object. Key = {}, Value = {}", h.getKey(), h.getValue());
                this.headers.add(h.getKey(), h.getValue());
            });
        }

        init();
    }

    private void init() {
        this.handler = new RestApiCallMsgHandler(baseUrl, headers);
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
