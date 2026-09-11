// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.service.solutions.data.names.RandomNameData;

import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CustomerDefinition extends BaseEntityDefinition {

    private String email;
    private String country;
    private String city;
    private String state;
    private String zip;
    private String address;
    private List<UserDefinition> users = Collections.emptyList();

    @JsonIgnore
    private RandomNameData randomNameData;

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

    public void setUsers(List<UserDefinition> users) {
        if (users != null) {
            this.users = users;
        }
    }

}
