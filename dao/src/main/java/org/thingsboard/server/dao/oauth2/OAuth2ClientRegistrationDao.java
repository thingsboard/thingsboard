package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;

import java.util.List;

public interface OAuth2ClientRegistrationDao {
    List<OAuth2ClientRegistration> find();

    OAuth2ClientRegistration findById(String registrationId);

    OAuth2ClientRegistration save(OAuth2ClientRegistration clientRegistration);

    boolean removeById(String registrationId);
}
