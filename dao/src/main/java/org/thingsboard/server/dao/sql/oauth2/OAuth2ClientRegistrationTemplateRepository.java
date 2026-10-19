// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationTemplateEntity;

import java.util.UUID;

public interface OAuth2ClientRegistrationTemplateRepository extends JpaRepository<OAuth2ClientRegistrationTemplateEntity, UUID> {

    OAuth2ClientRegistrationTemplateEntity findByProviderId(String providerId);

}
