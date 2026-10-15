// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class LoginRequest {

    private String username;

    private String password;

    @JsonCreator
    public LoginRequest(@JsonProperty("username") String username, @JsonProperty("password") String password) {
        this.username = username;
        this.password = password;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "User email", example = "tenant@thingsboard.org")
    public String getUsername() {
        return username;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "User password", example = "tenant")
    public String getPassword() {
        return password;
    }
}
