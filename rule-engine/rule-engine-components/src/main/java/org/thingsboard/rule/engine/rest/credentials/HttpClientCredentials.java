/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.rule.engine.rest.credentials;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.handler.ssl.SslContext;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpAnonymousCredentials.class, name = "anonymous"),
        @JsonSubTypes.Type(value = HttpBasicCredentials.class, name = "basic"),
        @JsonSubTypes.Type(value = HttpCertPemCredentials.class, name = "cert.PEM")})
public interface HttpClientCredentials {
    default Optional<SslContext> initSslContext() {
        return Optional.empty();
    }

    default Optional<String> getBasicAuthHeaderValue() {
        return Optional.empty();
    }
}

