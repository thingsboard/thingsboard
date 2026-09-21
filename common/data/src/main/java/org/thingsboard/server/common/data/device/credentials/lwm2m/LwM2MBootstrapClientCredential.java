// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "securityMode")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoSecBootstrapClientCredential.class, name = "NO_SEC"),
        @JsonSubTypes.Type(value = PSKBootstrapClientCredential.class, name = "PSK"),
        @JsonSubTypes.Type(value = RPKBootstrapClientCredential.class, name = "RPK"),
        @JsonSubTypes.Type(value = X509BootstrapClientCredential.class, name = "X509")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface LwM2MBootstrapClientCredential {

    @JsonIgnore
    LwM2MSecurityMode getSecurityMode();
}
