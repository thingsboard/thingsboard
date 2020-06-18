package org.thingsboard.server.common.data.oauth2;

import lombok.*;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2IntegrationId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class OAuth2MapperConfig extends BaseData<OAuth2IntegrationId> {
    private boolean allowUserCreation;
    private boolean activateUser;
    private MapperType type;
    private OAuth2BasicMapperConfig basicConfig;
    private OAuth2CustomMapperConfig customConfig;

    public OAuth2MapperConfig() {
        super();
    }

    public OAuth2MapperConfig(OAuth2IntegrationId id) {
        super(id);
    }

    @Builder(toBuilder = true)
    public OAuth2MapperConfig(OAuth2IntegrationId id, boolean allowUserCreation, boolean activateUser, MapperType type, OAuth2BasicMapperConfig basicConfig, OAuth2CustomMapperConfig customConfig) {
        super(id);
        this.allowUserCreation = allowUserCreation;
        this.activateUser = activateUser;
        this.type = type;
        this.basicConfig = basicConfig;
        this.customConfig = customConfig;
    }
}
