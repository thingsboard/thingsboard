/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainOauth2Registration;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.domain.DomainDao;
import org.thingsboard.server.dao.model.sql.DomainEntity;
import org.thingsboard.server.dao.model.sql.DomainOauth2RegistrationCompositeKey;
import org.thingsboard.server.dao.model.sql.DomainOauth2RegistrationEntity;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaDomainDao extends JpaAbstractDao<DomainEntity, Domain> implements DomainDao {

    private final DomainRepository domainRepository;
    private final DomainOauth2RegistrationRepository domainOauth2RegistrationRepository;

    @Override
    protected Class<DomainEntity> getEntityClass() {
        return DomainEntity.class;
    }

    @Override
    protected JpaRepository<DomainEntity, UUID> getRepository() {
        return domainRepository;
    }

    @Override
    public List<Domain> findByTenantId(TenantId tenantId) {
        return DaoUtil.convertDataList(domainRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public int countDomainByTenantIdAndOauth2Enabled(TenantId tenantId, boolean enabled) {
        return domainRepository.countByTenantIdAndOauth2Enabled(tenantId.getId(), enabled);
    }

    @Override
    public List<DomainOauth2Registration> findOauth2ClientsByDomainId(TenantId tenantId, DomainId domainId) {
        return  DaoUtil.convertDataList(domainOauth2RegistrationRepository.findAllByDomainId(domainId.getId()));
    }

    @Override
    public void saveOauth2Clients(DomainOauth2Registration domainOauth2Registration) {
        domainOauth2RegistrationRepository.save(new DomainOauth2RegistrationEntity(domainOauth2Registration));
    }

    @Override
    public void removeOauth2Clients(DomainId domainId, OAuth2RegistrationId oAuth2RegistrationId) {
        domainOauth2RegistrationRepository.deleteById(new DomainOauth2RegistrationCompositeKey(domainId.getId(), oAuth2RegistrationId.getId()));
    }

}

