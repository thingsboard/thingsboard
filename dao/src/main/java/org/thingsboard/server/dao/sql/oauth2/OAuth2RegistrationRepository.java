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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.OAuth2RegistrationEntity;
import org.thingsboard.server.dao.model.sql.OAuth2RegistrationInfoEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2RegistrationRepository extends JpaRepository<OAuth2RegistrationEntity, UUID> {

    List<OAuth2RegistrationEntity> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.OAuth2RegistrationInfoEntity(r.id, r.createdTime, r.platforms, r.title) " +
            "FROM OAuth2RegistrationEntity r " +
            "WHERE r.tenantId = :tenantId")
    List<OAuth2RegistrationInfoEntity> findInfosByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT r " +
            "FROM oauth2_registration r " +
            "LEFT JOIN domain_oauth2_registration dr on dr.oauth2_registration_id = r.id " +
            "LEFT JOIN domain d on dr.domain_id = d.id " +
            "WHERE d.oauth2_enabled = true " +
            "AND d.domain_name = :domainName " +
            "AND (:platformFilter IS NULL OR r.platforms IS NULL OR r.platforms = '' OR r.platforms LIKE :platformFilter)", nativeQuery = true)
    List<OAuth2RegistrationEntity> findEnabledByDomainNameAndPlatformType(@Param("domainName") String domainName,
                                                                          @Param("platformFilter") String platformFilter);

    @Query(value = "SELECT r " +
            "FROM oauth2_registration r " +
            "LEFT JOIN mobile_app_oauth2_registration mr on mr.oauth2_registration_id = r.id " +
            "LEFT JOIN mobile_app m on mr.mobile_app_id = m.id " +
            "WHERE m.oauth2_enabled = true " +
            "AND m.pck_name = :pkgName " +
            "AND (:platformFilter IS NULL OR r.platforms IS NULL OR r.platforms = '' OR r.platforms LIKE :platformFilter)", nativeQuery = true)
    List<OAuth2RegistrationEntity> findEnabledByPkgNameAndPlatformType(@Param("pkgName") String pkgName,
                                                                       @Param("platformFilter") String platformFilter);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.OAuth2RegistrationInfoEntity(r.id, r.createdTime, r.platforms, r.title) " +
            "FROM OAuth2RegistrationEntity r " +
            "LEFT JOIN DomainOauth2RegistrationEntity dr on dr.oauth2RegistrationId = r.id " +
            "WHERE dr.domainId = :domainId ")
    List<OAuth2RegistrationInfoEntity> findInfosByDomainId(UUID domainId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.OAuth2RegistrationInfoEntity(r.id, r.createdTime, r.platforms, r.title) " +
            "FROM OAuth2RegistrationEntity r " +
            "LEFT JOIN MobileAppOauth2RegistrationEntity mr on mr.oauth2RegistrationId = r.id " +
            "WHERE mr.mobileAppId = :mobileAppId ")
    List<OAuth2RegistrationInfoEntity> findInfosByMobileAppId(UUID mobileAppId);

    @Query("SELECT m.appSecret " +
            "FROM MobileAppEntity m " +
            "LEFT JOIN MobileAppOauth2RegistrationEntity mp on m.id = mp.mobileAppId " +
            "LEFT JOIN OAuth2RegistrationEntity p on mp.oauth2RegistrationId = p.id " +
            "WHERE p.id = :registrationId " +
            "AND m.pkgName = :pkgName")
    String findAppSecret(@Param("registrationId") UUID id,
                         @Param("pkgName") String pkgName);
}
