// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractUserEntity<T extends User> extends BaseVersionedEntity<T> {

    public static final Map<String, String> userColumnMap = new HashMap<>();

    static {
        userColumnMap.put("name", "email");
    }

    @Column(name = ModelConstants.USER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.USER_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.USER_AUTHORITY_PROPERTY)
    private Authority authority;

    @Column(name = ModelConstants.USER_EMAIL_PROPERTY, unique = true)
    private String email;

    @Column(name = ModelConstants.USER_FIRST_NAME_PROPERTY)
    private String firstName;

    @Column(name = ModelConstants.USER_LAST_NAME_PROPERTY)
    private String lastName;

    @Column(name = ModelConstants.PHONE_PROPERTY)
    private String phone;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.USER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.USER_CUSTOM_MENU_ID_PROPERTY)
    private UUID customMenuId;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractUserEntity() {
        super();
    }

    public AbstractUserEntity(T user) {
        super(user);
        this.authority = user.getAuthority();
        if (user.getTenantId() != null) {
            this.tenantId = user.getTenantId().getId();
        }
        if (user.getCustomerId() != null) {
            this.customerId = user.getCustomerId().getId();
        }
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
        this.additionalInfo = user.getAdditionalInfo();
        if (user.getCustomMenuId() != null) {
            this.customMenuId = user.getCustomMenuId().getId();
        }
        if (user.getExternalId() != null) {
            this.externalId = user.getExternalId().getId();
        }
    }

    public AbstractUserEntity(UserEntity userEntity) {
        super(userEntity);
        this.tenantId = userEntity.getTenantId();
        this.customerId = userEntity.getCustomerId();
        this.authority = userEntity.getAuthority();
        this.email = userEntity.getEmail();
        this.firstName = userEntity.getFirstName();
        this.lastName = userEntity.getLastName();
        this.phone = userEntity.getPhone();
        this.additionalInfo = userEntity.getAdditionalInfo();
        this.customMenuId = userEntity.getCustomMenuId();
        this.externalId = userEntity.getExternalId();
    }

    protected User toUser() {
        User user = new User(new UserId(this.getUuid()));
        user.setCreatedTime(createdTime);
        user.setVersion(version);
        user.setAuthority(authority);
        if (tenantId != null) {
            user.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            user.setCustomerId(new CustomerId(customerId));
        }
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAdditionalInfo(additionalInfo);
        if (customMenuId != null) {
            user.setCustomMenuId(new CustomMenuId(customMenuId));
        }
        if (externalId != null) {
            user.setExternalId(new UserId(externalId));
        }
        return user;
    }

}
