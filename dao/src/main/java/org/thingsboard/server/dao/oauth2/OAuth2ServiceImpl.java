/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    @Autowired(required = false)
    OAuth2Configuration oauth2Configuration;

    @Override
    public List<OAuth2ClientInfo> getOAuth2Clients() {
        if (!oauth2Configuration.isEnabled()) {
            return Collections.emptyList();
        }
        List<OAuth2ClientInfo> result = new ArrayList<>();
        for (OAuth2Client c : oauth2Configuration.getClients().values()) {
            OAuth2ClientInfo client = new OAuth2ClientInfo();
            client.setName(c.getLoginButtonLabel());
            client.setUrl(String.format("/oauth2/authorization/%s", c.getRegistrationId()));
            client.setIcon(c.getLoginButtonIcon());
            result.add(client);
        }
        return result;
    }
}
