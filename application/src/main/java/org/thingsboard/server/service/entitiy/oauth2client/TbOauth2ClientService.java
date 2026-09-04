// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.oauth2client;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;

public interface TbOauth2ClientService {

    OAuth2Client save(OAuth2Client oAuth2Client, User user) throws Exception;

    void delete(OAuth2Client oAuth2Client, User user);

}
