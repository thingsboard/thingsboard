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
package org.thingsboard.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2ClientEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2ClientDao extends JpaAbstractDao<OAuth2ClientEntity, OAuth2Client> implements OAuth2ClientDao {

    private final OAuth2ClientRepository repository;

    @Override
    protected Class<OAuth2ClientEntity> getEntityClass() {
        return OAuth2ClientEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2ClientEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public PageData<OAuth2Client> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<OAuth2Client> findEnabledByDomainName(String domainName) {
        return DaoUtil.convertDataList(repository.findEnabledByDomainNameAndPlatformType(domainName, PlatformType.WEB.name()));
    }

    @Override
    public List<OAuth2Client> findEnabledByPkgNameAndPlatformType(String pkgName, PlatformType platformType) {
        List<OAuth2ClientEntity> clientEntities;
        if (platformType != null) {
            clientEntities = switch (platformType) {
                case ANDROID -> repository.findEnabledByAndroidPkgNameAndPlatformType(pkgName, platformType.name());
                case IOS -> repository.findEnabledByIosPkgNameAndPlatformType(pkgName, platformType.name());
                default -> Collections.emptyList();
            };
        } else {
            clientEntities = Collections.emptyList();
        }
        return DaoUtil.convertDataList(clientEntities);
    }

    @Override
    public List<OAuth2Client> findByDomainId(UUID oauth2ParamsId) {
        return DaoUtil.convertDataList(repository.findByDomainId(oauth2ParamsId));
    }

    @Override
    public List<OAuth2Client> findByMobileAppBundleId(UUID mobileAppBundleId) {
        return DaoUtil.convertDataList(repository.findByMobileAppBundleId(mobileAppBundleId));
    }

    @Override
    public String findAppSecret(UUID id, String pkgName, PlatformType platformType) {
        return repository.findAppSecret(id, pkgName, platformType);
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        repository.deleteByTenantId(tenantId);
    }

    @Override
    public List<OAuth2Client> findByIds(UUID tenantId, List<OAuth2ClientId> oAuth2ClientIds) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndIdIn(tenantId, toUUIDs(oAuth2ClientIds)));
    }

    @Override
    public boolean isPropagateToEdge(TenantId tenantId, UUID oAuth2ClientId) {
        return repository.isPropagateToEdge(tenantId.getId(), oAuth2ClientId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }

}
