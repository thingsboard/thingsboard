package org.thingsboard.server.dao.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;

@ConditionalOnProperty(prefix = "security.oauth2", value = "enabled", havingValue = "true")
@Component
public class HybridClientRegistrationRepository implements ClientRegistrationRepository {

    @Autowired
    private OAuth2Service oAuth2Service;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        OAuth2ClientRegistration localClientRegistration = oAuth2Service.getClientRegistrationByRegistrationId(registrationId);
        return localClientRegistration == null ?
                null : toSpringClientRegistration(localClientRegistration);
    }

    private ClientRegistration toSpringClientRegistration(OAuth2ClientRegistration localClientRegistration){
        return ClientRegistration.withRegistrationId(localClientRegistration.getRegistrationId())
                .clientId(localClientRegistration.getClientId())
                .authorizationUri(localClientRegistration.getAuthorizationUri())
                .clientSecret(localClientRegistration.getClientSecret())
                .tokenUri(localClientRegistration.getTokenUri())
                .redirectUriTemplate(localClientRegistration.getRedirectUriTemplate())
                .scope(localClientRegistration.getScope().split(","))
                .clientName(localClientRegistration.getClientName())
                .authorizationGrantType(new AuthorizationGrantType(localClientRegistration.getAuthorizationGrantType()))
                .userInfoUri(localClientRegistration.getUserInfoUri())
                .userNameAttributeName(localClientRegistration.getUserNameAttribute())
                .jwkSetUri(localClientRegistration.getJwkSetUri())
                .clientAuthenticationMethod(new ClientAuthenticationMethod(localClientRegistration.getClientAuthenticationMethod()))
                .build();
    }
}
