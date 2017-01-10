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

import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.thingsboard.server.extensions.api.plugins.PluginConstants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;

@Data
public class RestRequest {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final RequestEntity<byte[]> requestEntity;
    private final HttpServletRequest request;

    public HttpMethod getMethod() {
        return requestEntity.getMethod();
    }

    public String getRequestBody() {
        return new String(requestEntity.getBody(), UTF8);
    }

    public String[] getPathParams() {
        String requestUrl = request.getRequestURL().toString();
        int index = requestUrl.indexOf(PluginConstants.PLUGIN_URL_PREFIX);
        String[] pathParams = requestUrl.substring(index + PluginConstants.PLUGIN_URL_PREFIX.length()).split("/");
        String[] result = new String[pathParams.length - 2];
        System.arraycopy(pathParams, 2, result, 0, result.length);
        return result;
    }

    public String getParameter(String paramName) throws ServletException {
        return getParameter(paramName, null);
    }

    public String getParameter(String paramName, String defaultValue) throws ServletException {
        String paramValue = request.getParameter(paramName);
        if (StringUtils.isEmpty(paramValue)) {
            if (defaultValue == null) {
                throw new MissingServletRequestParameterException(paramName, "String");
            } else {
                return defaultValue;
            }
        } else {
            return paramValue;
        }
    }

    public Optional<Long> getLongParamValue(String paramName) {
        return getParamValue(paramName, s -> Long.valueOf(s));
    }

    public Optional<Integer> getIntParamValue(String paramName) {
        return getParamValue(paramName, s -> Integer.valueOf(s));
    }

    public <T> Optional<T> getParamValue(String paramName, Function<String, T> function) {
        String paramValue = request.getParameter(paramName);
        if (paramValue != null) {
            return Optional.of(function.apply(paramValue));
        } else {
            return Optional.empty();
        }
    }
}
