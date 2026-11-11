// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;

/**
 * Created by ashvayka on 16.01.17.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnonymousIdentityProviderConfiguration.class, name = "anonymous"),
        @JsonSubTypes.Type(value = UsernameIdentityProviderConfiguration.class, name = "username")})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface IdentityProviderConfiguration {

    IdentityProvider toProvider();

}
