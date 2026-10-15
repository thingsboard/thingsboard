// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.DomainOauth2ClientCompositeKey;
import org.thingsboard.server.dao.model.sql.DomainOauth2ClientEntity;

import java.util.List;
import java.util.UUID;

public interface DomainOauth2ClientRepository extends JpaRepository<DomainOauth2ClientEntity, DomainOauth2ClientCompositeKey> {

    List<DomainOauth2ClientEntity> findAllByDomainId(UUID domainId);

}
