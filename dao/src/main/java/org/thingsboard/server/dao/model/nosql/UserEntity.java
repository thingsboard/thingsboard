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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.AuthorityCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_AUTHORITY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.USER_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_EMAIL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_FIRST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_LAST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_TENANT_ID_PROPERTY;

@Table(name = USER_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class UserEntity implements SearchTextEntity<User> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = USER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = USER_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @PartitionKey(value = 3)
    @Column(name = USER_AUTHORITY_PROPERTY, codec = AuthorityCodec.class)
    private Authority authority;

    @Column(name = USER_EMAIL_PROPERTY)
    private String email;
    
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;
    
    @Column(name = USER_FIRST_NAME_PROPERTY)
    private String firstName;
    
    @Column(name = USER_LAST_NAME_PROPERTY)
    private String lastName;

    @Column(name = USER_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public UserEntity() {
        super();
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
        this.additionalInfo = user.getAdditionalInfo();
    }
    
	public UUID getUuid() {
        return id;
    }

    public void setUuid(UUID id) {
        this.id = id;
    }

    public Authority getAuthority() {
		return authority;
	}

	public void setAuthority(Authority authority) {
		this.authority = authority;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public JsonNode getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(JsonNode additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	
    @Override
    public String getSearchTextSource() {
        return getEmail();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    
    public String getSearchText() {
        return searchText;
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
		user.setAdditionalInfo(additionalInfo);
        return user;
    }

}