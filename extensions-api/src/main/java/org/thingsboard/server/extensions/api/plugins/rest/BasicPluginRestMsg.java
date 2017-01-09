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
package org.thingsboard.server.extensions.api.plugins.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;

@SuppressWarnings("rawtypes")
public class BasicPluginRestMsg implements PluginRestMsg {

    private final PluginApiCallSecurityContext securityCtx;
    private final RestRequest request;
    private final DeferredResult<ResponseEntity> responseHolder;

    public BasicPluginRestMsg(PluginApiCallSecurityContext securityCtx, RestRequest request,
                              DeferredResult<ResponseEntity> responseHolder) {
        super();
        this.securityCtx = securityCtx;
        this.request = request;
        this.responseHolder = responseHolder;
    }

    @Override
    public PluginApiCallSecurityContext getSecurityCtx() {
        return securityCtx;
    }

    @Override
    public RestRequest getRequest() {
        return request;
    }

    @Override
    public DeferredResult<ResponseEntity> getResponseHolder() {
        return responseHolder;
    }

    @Override
    public PluginId getPluginId() {
        return securityCtx.getPluginId();
    }

    @Override
    public TenantId getPluginTenantId() {
        return securityCtx.getPluginTenantId();
    }
}
