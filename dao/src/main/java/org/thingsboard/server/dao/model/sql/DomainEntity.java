// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DOMAIN_TABLE_NAME)
public class DomainEntity extends BaseSqlEntity<Domain> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = ModelConstants.DOMAIN_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.DOMAIN_OAUTH2_ENABLED_PROPERTY)
    private Boolean oauth2Enabled;

    @Column(name = ModelConstants.DOMAIN_PROPAGATE_TO_EDGE_PROPERTY)
    private Boolean propagateToEdge;

    public DomainEntity(Domain domain) {
        super(domain);
        if (domain.getTenantId() != null) {
            this.tenantId = domain.getTenantId().getId();
        }
        this.name = domain.getName();
        this.oauth2Enabled = domain.isOauth2Enabled();
        this.propagateToEdge = domain.isPropagateToEdge();
    }

    public DomainEntity() {
        super();
    }

    @Override
    public Domain toData() {
        Domain domain = new Domain();
        domain.setId(new DomainId(id));
        if (tenantId != null) {
            domain.setTenantId(TenantId.fromUUID(tenantId));
        }
        domain.setCreatedTime(createdTime);
        domain.setName(name);
        domain.setOauth2Enabled(oauth2Enabled);
        domain.setPropagateToEdge(propagateToEdge);
        return domain;
    }
}
