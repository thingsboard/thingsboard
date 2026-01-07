/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
