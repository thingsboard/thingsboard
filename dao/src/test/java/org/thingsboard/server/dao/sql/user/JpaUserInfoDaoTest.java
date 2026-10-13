// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.user;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserInfoDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JpaUserInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private UserInfoDao userInfoDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CustomerDao customerDao;

    private List<User> users = new ArrayList<>();

    @After
    public void tearDown() {
        for (User user : users) {
            userDao.removeById(user.getTenantId(), user.getUuidId());
        }
        users.clear();
    }

    @Test
    public void testFindUserInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            users.add(createUser(tenantId1, null, i));
            users.add(createUser(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "thingsboard.org");
        PageData<UserInfo> userInfos1 = userInfoDao.findUsersByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, userInfos1.getData().size());

        PageData<UserInfo> userInfos2 = userInfoDao.findUsersByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, userInfos2.getData().size());
    }

    @Test
    public void testFindUserInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            users.add(createUser(tenantId1, customer1.getUuidId(), i));
            users.add(createUser(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "thingsboard.org", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<UserInfo> userInfos1 = userInfoDao.findUsersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, userInfos1.getData().size());
        userInfos1.getData().forEach(userInfo -> Assert.assertNotEquals("CUSTOMER_0", userInfo.getOwnerName()));

        PageData<UserInfo> userInfos2 = userInfoDao.findUsersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, userInfos2.getData().size());

        PageData<UserInfo> userInfos3 = userInfoDao.findUsersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, userInfos3.getData().size());
    }

    private User createUser(UUID tenantId, UUID customerId, int index) {
        User user = new User();
        UUID id = Uuids.timeBased();
        user.setId(new UserId(id));
        user.setTenantId(TenantId.fromUUID(tenantId));
        user.setCustomerId(new CustomerId(customerId));
        if (customerId == null) {
            user.setAuthority(Authority.TENANT_ADMIN);
        } else {
            user.setAuthority(Authority.CUSTOMER_USER);
        }
        String idString = id.toString();
        String email = idString.substring(0, idString.indexOf('-')) + "@thingsboard.org";
        user.setEmail(email);
        return userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

}
