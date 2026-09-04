// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.domain;

import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainOauth2Client;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface DomainDao extends Dao<Domain> {

    PageData<Domain> findByTenantId(TenantId tenantId, PageLink pageLink);

    int countDomainByTenantIdAndOauth2Enabled(TenantId tenantId, boolean oauth2Enabled);

    List<DomainOauth2Client> findOauth2ClientsByDomainId(TenantId tenantId, DomainId domainId);

    void addOauth2Client(DomainOauth2Client domainOauth2Client);

    void removeOauth2Client(DomainOauth2Client domainOauth2Client);

    void deleteByTenantId(TenantId tenantId);
}
