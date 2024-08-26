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
package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.OAuth2ClientEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2ClientRepository extends JpaRepository<OAuth2ClientEntity, UUID> {

    @Query("SELECT с FROM OAuth2ClientEntity с WHERE с.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(с.title, concat('%', :searchText, '%')) = true)")
    Page<OAuth2ClientEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                            @Param("searchText") String searchText,
                                            Pageable pageable);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN DomainOauth2ClientEntity dc on c.id = dc.oauth2ClientId " +
            "LEFT JOIN DomainEntity domain on dc.domainId = domain.id " +
            "WHERE domain.name = :domainName AND domain.oauth2Enabled = true " +
            "AND (:platformFilter IS NULL OR c.platforms IS NULL OR c.platforms = '' OR ilike(c.platforms, CONCAT('%', :platformFilter, '%')) = true)")
    List<OAuth2ClientEntity> findEnabledByDomainNameAndPlatformType(@Param("domainName") String domainName,
                                                                    @Param("platformFilter") String platformFilter);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN MobileAppOauth2ClientEntity mc on c.id = mc.oauth2ClientId " +
            "LEFT JOIN MobileAppEntity app on mc.mobileAppId = app.id " +
            "WHERE app.pkgName = :pkgName AND app.oauth2Enabled = true " +
            "AND (:platformFilter IS NULL OR c.platforms IS NULL OR c.platforms = '' OR ilike(c.platforms, CONCAT('%', :platformFilter, '%')) = true)")
    List<OAuth2ClientEntity> findEnabledByPkgNameAndPlatformType(@Param("pkgName") String pkgName,
                                                                 @Param("platformFilter") String platformFilter);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN DomainOauth2ClientEntity dc on dc.oauth2ClientId = c.id " +
            "WHERE dc.domainId = :domainId ")
    List<OAuth2ClientEntity> findByDomainId(@Param("domainId") UUID domainId);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN MobileAppOauth2ClientEntity mc on mc.oauth2ClientId = c.id " +
            "WHERE mc.mobileAppId = :mobileAppId ")
    List<OAuth2ClientEntity> findByMobileAppId(@Param("mobileAppId") UUID mobileAppId);

    @Query("SELECT m.appSecret " +
            "FROM MobileAppEntity m " +
            "LEFT JOIN MobileAppOauth2ClientEntity mp on m.id = mp.mobileAppId " +
            "LEFT JOIN OAuth2ClientEntity p on mp.oauth2ClientId = p.id " +
            "WHERE p.id = :clientId " +
            "AND m.pkgName = :pkgName")
    String findAppSecret(@Param("clientId") UUID id,
                         @Param("pkgName") String pkgName);

    @Transactional
    @Modifying
    @Query("DELETE FROM OAuth2ClientEntity t WHERE t.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    List<OAuth2ClientEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> uuids);

    @Query("SELECT COUNT(d) > 0 FROM DomainEntity d " +
            "JOIN DomainOauth2ClientEntity doc ON d.id = doc.domainId " +
            "WHERE d.tenantId = :tenantId AND doc.oauth2ClientId = :oAuth2ClientId AND d.propagateToEdge = true")
    boolean isPropagateToEdge(@Param("tenantId") UUID tenantId, @Param("oAuth2ClientId") UUID oAuth2ClientId);

}
