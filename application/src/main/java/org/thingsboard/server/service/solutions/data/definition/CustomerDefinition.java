// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.service.solutions.data.names.RandomNameData;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class CustomerDefinition extends BaseEntityDefinition {

    private String group;
    private String email;
    private String country;
    private String city;
    private String state;
    private String zip;
    private String address;

    private List<String> assetGroups = Collections.emptyList();
    private List<String> deviceGroups = Collections.emptyList();
    private List<UserGroupDefinition> userGroups = Collections.emptyList();
    private List<UserDefinition> users = Collections.emptyList();

    @JsonIgnore
    private RandomNameData randomNameData;

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

    public void setAssetGroups(List<String> assetGroups) {
        if (assetGroups != null) {
            this.assetGroups = assetGroups;
        }
    }

    public void setDeviceGroups(List<String> deviceGroups) {
        if (deviceGroups != null) {
            this.deviceGroups = deviceGroups;
        }
    }

    public void setUserGroups(List<UserGroupDefinition> userGroups) {
        if (userGroups != null) {
            this.userGroups = userGroups;
        }
    }

    public void setUsers(List<UserDefinition> users) {
        if (users != null) {
            this.users = users;
        }
    }
}
