/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.*;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/21/2017.
 */
@Slf4j
@Data
@Entity
@Table(name = ModelConstants.USER_COLUMN_FAMILY_NAME)
public class UserEntity implements SearchTextEntity<User> {
    @Transient
    private static final long serialVersionUID = -271106508790582977L;

    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = ModelConstants.USER_TENANT_ID_PROPERTY, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = ModelConstants.USER_CUSTOMER_ID_PROPERTY, columnDefinition = "BINARY(16)")
    private UUID customerId;

    @Column(name = ModelConstants.USER_AUTHORITY_PROPERTY)
    private Authority authority;

    @Column(name = ModelConstants.USER_EMAIL_PROPERTY, unique = true)
    private String email;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.USER_FIRST_NAME_PROPERTY)
    private String firstName;

    @Column(name = ModelConstants.USER_LAST_NAME_PROPERTY)
    private String lastName;

    @Column(name = ModelConstants.USER_ADDITIONAL_INFO_PROPERTY)
    private String additionalInfo;

    public UserEntity() {
    }

    public UserEntity(User user) {
        if (user.getId() != null) {
            this.id = user.getId().getId();
        }
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
        if (user.getAdditionalInfo() != null) {
            this.additionalInfo = user.getAdditionalInfo().toString();
        }
    }

    @Override
    public String getSearchTextSource() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserEntity [id=");
        builder.append(id);
        builder.append(", authority=");
        builder.append(authority);
        builder.append(", tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", email=");
        builder.append(email);
        builder.append(", firstName=");
        builder.append(firstName);
        builder.append(", lastName=");
        builder.append(lastName);
        builder.append(", additionalInfo=");
        builder.append(additionalInfo);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public User toData() {
        User user = new User(new UserId(id));
        user.setCreatedTime(UUIDs.unixTimestamp(id));
        user.setAuthority(authority);
        if (tenantId != null) {
            user.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            user.setCustomerId(new CustomerId(customerId));
        }
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        ObjectMapper mapper = new ObjectMapper();
        if (additionalInfo != null) {
            try {
                JsonNode jsonNode = mapper.readTree(additionalInfo);
                user.setAdditionalInfo(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", additionalInfo, e.getMessage()), e);
            }
        }
        return user;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((additionalInfo == null) ? 0 : additionalInfo.hashCode());
        result = prime * result + ((authority == null) ? 0 : authority.hashCode());
        result = prime * result + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserEntity other = (UserEntity) obj;
        if (additionalInfo == null) {
            if (other.additionalInfo != null)
                return false;
        } else if (!additionalInfo.equals(other.additionalInfo))
            return false;
        if (authority != other.authority)
            return false;
        if (customerId == null) {
            if (other.customerId != null)
                return false;
        } else if (!customerId.equals(other.customerId))
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (tenantId == null) {
            if (other.tenantId != null)
                return false;
        } else if (!tenantId.equals(other.tenantId))
            return false;
        return true;
    }
}
