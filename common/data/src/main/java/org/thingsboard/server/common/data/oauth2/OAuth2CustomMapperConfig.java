package org.thingsboard.server.common.data.oauth2;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString(exclude = {"password"})
public class OAuth2CustomMapperConfig {
    private final String url;
    private final String username;
    private final String password;
}
