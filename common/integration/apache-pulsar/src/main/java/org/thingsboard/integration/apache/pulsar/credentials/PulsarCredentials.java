// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar.credentials;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.pulsar.client.api.Authentication;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnonymousPulsarCredentials.class, name = "anonymous"),
        @JsonSubTypes.Type(value = TokenPulsarCredentials.class, name = "token")})
public interface PulsarCredentials {

    Authentication getAuthentication();
}
