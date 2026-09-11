// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        discriminatorProperty = "securityConfigClientMode",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "NO_SEC", schema = NoSecClientCredential.class),
                @DiscriminatorMapping(value = "PSK", schema = PSKClientCredential.class),
                @DiscriminatorMapping(value = "RPK", schema = RPKClientCredential.class),
                @DiscriminatorMapping(value = "X509", schema = X509ClientCredential.class)
        }
)
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
