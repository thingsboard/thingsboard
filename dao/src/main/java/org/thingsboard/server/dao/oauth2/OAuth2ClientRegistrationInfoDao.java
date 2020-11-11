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

import org.thingsboard.server.common.data.oauth2.ExtendedOAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Set;

public interface OAuth2ClientRegistrationInfoDao extends Dao<OAuth2ClientRegistrationInfo> {
    List<OAuth2ClientRegistrationInfo> findAll();

    List<ExtendedOAuth2ClientRegistrationInfo> findAllExtended();

    List<OAuth2ClientRegistrationInfo> findByDomainSchemesAndDomainName(List<SchemeType> domainSchemes, String domainName);

    void deleteAll();
}
