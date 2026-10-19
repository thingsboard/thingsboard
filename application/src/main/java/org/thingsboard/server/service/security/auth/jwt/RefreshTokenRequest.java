// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.jwt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenRequest(String refreshToken) {

    @JsonCreator
    public RefreshTokenRequest(@JsonProperty("refreshToken") String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
