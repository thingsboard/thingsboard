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
package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.model.sql.ExtendedOAuth2ClientRegistrationInfoEntity;
import org.thingsboard.server.dao.model.sql.OAuth2ClientRegistrationInfoEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2ClientRegistrationInfoRepository extends CrudRepository<OAuth2ClientRegistrationInfoEntity, UUID> {
    @Query("SELECT new OAuth2ClientRegistrationInfoEntity(cr_info) " +
            "FROM OAuth2ClientRegistrationInfoEntity cr_info " +
            "LEFT JOIN OAuth2ClientRegistrationEntity cr on cr_info.id = cr.clientRegistrationInfoId " +
            "WHERE cr.domainName = :domainName " +
            "AND cr.domainScheme IN (:domainSchemes)")
    List<OAuth2ClientRegistrationInfoEntity> findAllByDomainSchemesAndName(@Param("domainSchemes") List<SchemeType> domainSchemes,
                                                                           @Param("domainName") String domainName);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.ExtendedOAuth2ClientRegistrationInfoEntity(cr_info, cr.domainName, cr.domainScheme) " +
            "FROM OAuth2ClientRegistrationInfoEntity cr_info " +
            "LEFT JOIN OAuth2ClientRegistrationEntity cr on cr_info.id = cr.clientRegistrationInfoId ")
    List<ExtendedOAuth2ClientRegistrationInfoEntity> findAllExtended();
}
