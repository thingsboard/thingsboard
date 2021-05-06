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
package org.thingsboard.server.transport.lwm2m.secure.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.leshan.core.SecurityMode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "securityConfigClientMode")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoSecClientCredentialsConfig.class, name = "NO_SEC"),
        @JsonSubTypes.Type(value = PSKClientCredentialsConfig.class, name = "PSK"),
        @JsonSubTypes.Type(value = RPKClientCredentialsConfig.class, name = "RPK"),
        @JsonSubTypes.Type(value = X509ClientCredentialsConfig.class, name = "X509")})
public interface LwM2MClientCredentialsConfig {

    @JsonIgnore
    SecurityMode getSecurityConfigClientMode();
}
