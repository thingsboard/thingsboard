package org.thingsboard.server.common.data.oauth2;

import lombok.*;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2IntegrationId;

@EqualsAndHashCode
@Data
@ToString(exclude = {"clientSecret"})
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ClientRegistration {

    private String registrationId;
    private OAuth2MapperConfig mapperConfig;
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String tokenUri;
    private String redirectUriTemplate;
    private String scope;
    private String authorizationGrantType;
    private String userInfoUri;
    private String userNameAttributeName;
    private String jwkSetUri;
    private String clientAuthenticationMethod;
    private String clientName;
    private String loginButtonLabel;
    private String loginButtonIcon;
}
