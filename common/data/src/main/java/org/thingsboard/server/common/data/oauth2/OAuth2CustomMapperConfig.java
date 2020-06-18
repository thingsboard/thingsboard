package org.thingsboard.server.common.data.oauth2;

import lombok.*;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString(exclude = {"password"})
public class OAuth2CustomMapperConfig {
    private final String url;
    private final String username;
    private final String password;
}
