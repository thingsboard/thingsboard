/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.dao.model.sql.OAuth2ParamsEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ParamsDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaOAuth2ParamsDao extends JpaAbstractDao<OAuth2ParamsEntity, OAuth2Params> implements OAuth2ParamsDao {
    private final OAuth2ParamsRepository repository;

    @Override
    protected Class<OAuth2ParamsEntity> getEntityClass() {
        return OAuth2ParamsEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2ParamsEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
