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
package org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "securityMode")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoSecBootstrapServerConfig.class, name = "NO_SEC"),
        @JsonSubTypes.Type(value = PSKBootstrapServerConfig.class, name = "PSK"),
        @JsonSubTypes.Type(value = RPKBootstrapServerConfig.class, name = "RPK"),
        @JsonSubTypes.Type(value = X509BootstrapServerConfig.class, name = "X509")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface BootstrapServerConfig {
    @JsonIgnore
    LwM2MSecurityMode getSecurityMode();
}
