// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class OAuth2ClientRegistrationTemplateId extends UUIDBased {

    @JsonCreator
    public OAuth2ClientRegistrationTemplateId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OAuth2ClientRegistrationTemplateId fromString(String clientRegistrationTemplateId) {
        return new OAuth2ClientRegistrationTemplateId(UUID.fromString(clientRegistrationTemplateId));
    }
}
