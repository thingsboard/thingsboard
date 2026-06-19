/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
