package org.thingsboard.server.common.data.oauth2;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
public class OAuth2MapperConfig {
    private boolean allowUserCreation;
    private boolean activateUser;
    private MapperType type;
    private OAuth2BasicMapperConfig basicConfig;
    private OAuth2CustomMapperConfig customConfig;
}
