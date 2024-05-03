/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.rest;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;

import java.util.Collections;
import java.util.Map;

@Data
public class TbAzureFunctionsNodeConfiguration extends TbRestApiCallNodeConfiguration {

    private Map<String, String> queryParams;
    private Map<String, String> inputKeys;

    @Override
    public TbAzureFunctionsNodeConfiguration defaultConfiguration() {
        TbAzureFunctionsNodeConfiguration configuration = new TbAzureFunctionsNodeConfiguration();
        configuration.setRestEndpointUrlPattern("http://localhost:<port>/api/<function-name>");
        configuration.setRequestMethod("POST");
        configuration.setHeaders(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        configuration.setQueryParams(Collections.emptyMap());
        configuration.setInputKeys(Collections.emptyMap());
        configuration.setCredentials(new AnonymousCredentials());
        return configuration;
    }
}
