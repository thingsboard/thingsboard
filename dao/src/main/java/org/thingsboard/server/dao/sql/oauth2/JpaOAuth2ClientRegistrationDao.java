/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientRegistrationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaOAuth2ClientRegistrationDao extends JpaAbstractDao<OAuth2ClientRegistrationEntity, OAuth2ClientRegistration> implements OAuth2ClientRegistrationDao {
    private final OAuth2ClientRegistrationRepository repository;

    @Override
    protected Class<OAuth2ClientRegistrationEntity> getEntityClass() {
        return OAuth2ClientRegistrationEntity.class;
    }

    @Override
    protected CrudRepository<OAuth2ClientRegistrationEntity, UUID> getCrudRepository() {
        return repository;
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
