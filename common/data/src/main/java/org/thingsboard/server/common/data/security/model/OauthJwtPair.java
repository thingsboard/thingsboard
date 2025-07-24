package org.thingsboard.server.common.data.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class OauthJwtPair extends JwtPair {
    @Schema(description = "The JWT oAuth 2.0 OpenID identity token. Used to confirms the identity of the user (i.e., who logged in) in oAuth 2.0 provider.", example = "AAB254FF67D..")
    private String idToken;

    public OauthJwtPair(String token, String refreshToken, String idToken) {
        super(token, refreshToken);
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }
}
