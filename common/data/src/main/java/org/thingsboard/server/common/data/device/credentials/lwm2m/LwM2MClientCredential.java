// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "securityConfigClientMode")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoSecClientCredential.class, name = "NO_SEC"),
        @JsonSubTypes.Type(value = PSKClientCredential.class, name = "PSK"),
        @JsonSubTypes.Type(value = RPKClientCredential.class, name = "RPK"),
        @JsonSubTypes.Type(value = X509ClientCredential.class, name = "X509")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface LwM2MClientCredential {

    @JsonIgnore
    LwM2MSecurityMode getSecurityConfigClientMode();

    String getEndpoint();
}
