// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ROLE_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ROLE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ROLE_EXCLUDED_PERMISSIONS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ROLE_PERMISSIONS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ROLE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ROLE_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.ROLE_TABLE_NAME)
@Slf4j
public class RoleEntity extends BaseVersionedEntity<Role> {

    @Column(name = ROLE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ROLE_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = ROLE_TYPE_PROPERTY)
    private RoleType type;

    @Column(name = ROLE_NAME_PROPERTY)
    private String name;

    @Convert(converter = JsonConverter.class)
    @Column(name = ROLE_PERMISSIONS_PROPERTY)
    private JsonNode permissions;

    @Convert(converter = JsonConverter.class)
    @Column(name = ROLE_EXCLUDED_PERMISSIONS_PROPERTY)
    private JsonNode excludedPermissions;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public RoleEntity() {
        super();
    }

    public RoleEntity(Role role) {
        super(role);
        if (role.getTenantId() != null) {
            this.tenantId = role.getTenantId().getId();
        }
        if (role.getCustomerId() != null) {
            this.customerId = role.getCustomerId().getId();
        }
        this.type = role.getType();
        this.name = role.getName();
        this.permissions = role.getPermissions();
        this.excludedPermissions = role.getExcludedPermissions();
        this.additionalInfo = role.getAdditionalInfo();
        if (role.getExternalId() != null) {
            this.externalId = role.getExternalId().getId();
        }
    }

    @Override
    public Role toData() {
        Role role = new Role(new RoleId(getUuid()));
        role.setCreatedTime(createdTime);
        role.setVersion(version);
        if (tenantId != null) {
            role.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            role.setCustomerId(new CustomerId(customerId));
        }
        role.setType(type);
        role.setName(name);
        role.setPermissions(permissions);
        role.setExcludedPermissions(excludedPermissions);
        role.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            role.setExternalId(new RoleId(externalId));
        }
        return role;
    }

}
