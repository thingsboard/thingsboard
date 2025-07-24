package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LogoutResponse {
    @Schema(description = "The logout endpoint that logs out the authenticated user from SSO. Available if user logged in via oAuth2.", accessMode = Schema.AccessMode.READ_ONLY)
    private String endSessionEndpoint;

    @Schema(description = "The URI where End-User's User Agent will be redirected after a logout has been performed.", accessMode = Schema.AccessMode.READ_ONLY)
    private String redirectUri;

    public LogoutResponse() {

    }

    public LogoutResponse(String endSessionEndpoint, String redirectUri) {
        this.endSessionEndpoint = endSessionEndpoint;
        this.redirectUri = redirectUri;
    }
}