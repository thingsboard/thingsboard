// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;

public interface OAuth2ClientRegistrationTemplateDao extends Dao<OAuth2ClientRegistrationTemplate> {

    Optional<OAuth2ClientRegistrationTemplate> findByProviderId(String providerId);

    List<OAuth2ClientRegistrationTemplate> findAll();
}
