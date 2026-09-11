// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class OAuth2ParamsId extends UUIDBased {

    @JsonCreator
    public OAuth2ParamsId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OAuth2ParamsId fromString(String oauth2ParamsId) {
        return new OAuth2ParamsId(UUID.fromString(oauth2ParamsId));
    }
}
