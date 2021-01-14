/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import org.apache.commons.codec.binary.Base64;
import org.thingsboard.rule.engine.credentials.BasicCredentials;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HttpBasicCredentials extends BasicCredentials implements HttpClientCredentials {
    @Override
    public Optional<String> getBasicAuthHeaderValue() {
        String authString = getUsername() + ":" + getPassword();
        String encodedAuthString = new String(Base64.encodeBase64(authString.getBytes(StandardCharsets.UTF_8)));
        return Optional.of("Basic " + encodedAuthString);
    }
}
