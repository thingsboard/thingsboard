/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.notification.targets.platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import javax.validation.constraints.AssertTrue;
import java.util.UUID;

@Data
public class CustomerUsersFilter implements UsersFilter {

    private UUID customerId; // might not be set if using with notification rule
    private boolean getCustomerIdFromOriginatorEntity; // e.g. from alarm

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.CUSTOMER_USERS;
    }

    @AssertTrue(message = "customerId is required")
    @JsonIgnore
    public boolean isValid() {
        if (!getCustomerIdFromOriginatorEntity) {
            return customerId != null && !customerId.equals(EntityId.NULL_UUID);
        } else {
            return true;
        }
    }

}
