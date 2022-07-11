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
package org.thingsboard.server.dao.service.validator;

import org.thingsboard.server.common.data.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserService;

@Component
public class UserDataValidator extends DataValidator<User> {

    @Autowired
    private UserDao userDao;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, User user) {
        if (!user.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxUsers = profileConfiguration.getMaxUsers();
            validateNumberOfEntitiesPerTenant(tenantId, userDao, maxUsers, EntityType.USER);
        }
    }

    @Override
    protected void validateDataImpl(TenantId requestTenantId, User user) {
        if (StringUtils.isEmpty(user.getEmail())) {
            throw new DataValidationException("User email should be specified!");
        }

        validateEmail(user.getEmail());

        Authority authority = user.getAuthority();
        if (authority == null) {
            throw new DataValidationException("User authority isn't defined!");
        }
        TenantId tenantId = user.getTenantId();
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
            user.setTenantId(tenantId);
        }
        CustomerId customerId = user.getCustomerId();
        if (customerId == null) {
            customerId = new CustomerId(ModelConstants.NULL_UUID);
            user.setCustomerId(customerId);
        }

        switch (authority) {
            case SYS_ADMIN:
                if (!tenantId.getId().equals(ModelConstants.NULL_UUID)
                        || !customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("System administrator can't be assigned neither to tenant nor to customer!");
                }
                break;
            case TENANT_ADMIN:
                if (tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Tenant administrator should be assigned to tenant!");
                } else if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Tenant administrator can't be assigned to customer!");
                }
                break;
            case CUSTOMER_USER:
                if (tenantId.getId().equals(ModelConstants.NULL_UUID)
                        || customerId.getId().equals(ModelConstants.NULL_UUID)) {
                    throw new DataValidationException("Customer user should be assigned to customer!");
                }
                break;
            default:
                break;
        }

        User existentUserWithEmail = userService.findUserByEmail(tenantId, user.getEmail());
        if (existentUserWithEmail != null && !isSameData(existentUserWithEmail, user)) {
            throw new DataValidationException("User with email '" + user.getEmail() + "' "
                    + " already present in database!");
        }
        if (!tenantId.getId().equals(ModelConstants.NULL_UUID)) {
            Tenant tenant = tenantDao.findById(tenantId, user.getTenantId().getId());
            if (tenant == null) {
                throw new DataValidationException("User is referencing to non-existent tenant!");
            }
        }
        if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, user.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("User is referencing to non-existent customer!");
            } else if (!customer.getTenantId().getId().equals(tenantId.getId())) {
                throw new DataValidationException("User can't be assigned to customer from different tenant!");
            }
        }
    }
}
