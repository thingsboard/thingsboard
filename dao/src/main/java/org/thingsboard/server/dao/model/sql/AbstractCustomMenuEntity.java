// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_ASSIGNEE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_SCOPE;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractCustomMenuEntity<T extends CustomMenuInfo> extends BaseSqlEntity<T> {

    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = CUSTOM_MENU_NAME)
    private String name;

    @Column(name = CUSTOM_MENU_SCOPE)
    @Enumerated(EnumType.STRING)
    private CMScope scope;

    @Column(name = CUSTOM_MENU_ASSIGNEE_TYPE)
    @Enumerated(EnumType.STRING)
    private CMAssigneeType assigneeType;

    @Type(StringArrayType.class)
    @Column(name = ModelConstants.CUSTOM_MENU_USER_GROUP_NAMES, columnDefinition = "text[]")
    private String[] userGroupNames;

    public AbstractCustomMenuEntity() {
        super();
    }

    public AbstractCustomMenuEntity(T customMenuInfo) {
        super(customMenuInfo);
        if (customMenuInfo.getTenantId() != null) {
            this.tenantId = customMenuInfo.getTenantId().getId();
        }
        if (customMenuInfo.getCustomerId() != null) {
            this.customerId = customMenuInfo.getCustomerId().getId();
        }
        this.name = customMenuInfo.getName();
        this.scope = customMenuInfo.getScope();
        this.assigneeType = customMenuInfo.getAssigneeType();
        this.userGroupNames = customMenuInfo.getUserGroupNames();
    }

    public AbstractCustomMenuEntity(CustomMenuInfoEntity customMenuInfoEntity) {
        super(customMenuInfoEntity);
        this.tenantId = customMenuInfoEntity.getTenantId();
        this.customerId = customMenuInfoEntity.getCustomerId();
        this.name = customMenuInfoEntity.getName();
        this.scope = customMenuInfoEntity.getScope();
        this.assigneeType = customMenuInfoEntity.getAssigneeType();
        this.userGroupNames = customMenuInfoEntity.getUserGroupNames();
    }

    protected CustomMenuInfo toCustomMenuInfo() {
        CustomMenuInfo customMenuInfo = new CustomMenuInfo(new CustomMenuId(id));
        customMenuInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            customMenuInfo.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            customMenuInfo.setCustomerId(new CustomerId(customerId));
        }
        customMenuInfo.setName(name);
        customMenuInfo.setScope(scope);
        customMenuInfo.setAssigneeType(assigneeType);
        customMenuInfo.setUserGroupNames(userGroupNames);
        return customMenuInfo;
    }

}
