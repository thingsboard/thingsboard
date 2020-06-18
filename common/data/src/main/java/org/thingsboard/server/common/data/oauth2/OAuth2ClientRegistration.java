package org.thingsboard.server.common.data.oauth2;

import lombok.*;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2IntegrationId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
public class OAuth2ClientRegistration extends BaseData<OAuth2IntegrationId> {

    private String registrationId;
    private OAuth2IntegrationId mapperConfigId;
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String tokenUri;
    private String redirectUriTemplate;
    private String scope;
    private String authorizationGrantType;
    private String userInfoUri;
    private String userNameAttribute;
    private String jwkSetUri;
    private String clientAuthenticationMethod;
    private String clientName;
    private String loginButtonLabel;
    private String loginButtonIcon;

    public OAuth2ClientRegistration() {
        super();
    }

    public OAuth2ClientRegistration(OAuth2IntegrationId id) {
        super(id);
    }

    @Builder(toBuilder = true)
    public OAuth2ClientRegistration(OAuth2IntegrationId id, String registrationId, String clientId, String clientSecret, String authorizationUri, String tokenUri, String redirectUriTemplate, String scope, String authorizationGrantType, String userInfoUri, String userNameAttribute, String jwkSetUri, String clientAuthenticationMethod, String clientName, String loginButtonLabel, String loginButtonIcon, OAuth2IntegrationId mapperConfigId) {
        super(id);
        this.registrationId = registrationId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.redirectUriTemplate = redirectUriTemplate;
        this.scope = scope;
        this.authorizationGrantType = authorizationGrantType;
        this.userInfoUri = userInfoUri;
        this.userNameAttribute = userNameAttribute;
        this.jwkSetUri = jwkSetUri;
        this.clientAuthenticationMethod = clientAuthenticationMethod;
        this.clientName = clientName;
        this.loginButtonLabel = loginButtonLabel;
        this.loginButtonIcon = loginButtonIcon;
        this.mapperConfigId = mapperConfigId;
    }
}
