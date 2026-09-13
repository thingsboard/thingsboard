// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;

import java.util.List;
import java.util.Optional;

public interface OAuth2ConfigTemplateService {
    OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate);

    Optional<OAuth2ClientRegistrationTemplate> findClientRegistrationTemplateByProviderId(String providerId);

    OAuth2ClientRegistrationTemplate findClientRegistrationTemplateById(OAuth2ClientRegistrationTemplateId templateId);

    List<OAuth2ClientRegistrationTemplate> findAllClientRegistrationTemplates();

    void deleteClientRegistrationTemplateById(OAuth2ClientRegistrationTemplateId templateId);
}
