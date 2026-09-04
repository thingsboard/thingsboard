// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.domain;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.OAuth2ClientId;

import java.util.List;

public interface TbDomainService {

    Domain save(Domain domain, List<OAuth2ClientId> oAuth2Clients, User user) throws Exception;

    void updateOauth2Clients(Domain domain, List<OAuth2ClientId> oAuth2ClientIds, User user);

    void delete(Domain domain, User user);

}
