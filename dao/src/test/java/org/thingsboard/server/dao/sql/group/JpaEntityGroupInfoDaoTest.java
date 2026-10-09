// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.group;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.group.EntityGroupInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JpaEntityGroupInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EntityGroupInfoDao entityGroupInfoDao;

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private CustomerDao customerDao;

    private List<EntityGroup> groups = new ArrayList<>();

    @After
    public void tearDown() {
        for (EntityGroup group : groups) {
            entityGroupDao.removeById(TenantId.SYS_TENANT_ID, group.getUuidId());
        }
        groups.clear();
    }

    @Test
    public void testFindEntityGroupInfosByTypeAndPageLink() {
        TenantId tenantId1 = new TenantId(Uuids.timeBased());
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getId(),1);

        Set<EntityId> tenantOwners = new LinkedHashSet<>();
        tenantOwners.add(tenantId1);

        Set<EntityId> customer1Owners = new LinkedHashSet<>();
        customer1Owners.add(customer1.getId());
        customer1Owners.add(tenantId1);

        Set<EntityId> subCustomer2Owners = new LinkedHashSet<>();
        subCustomer2Owners.add(subCustomer2.getId());
        subCustomer2Owners.add(customer1.getId());
        subCustomer2Owners.add(tenantId1);

        for (int i = 0; i < 20; i++) {
            groups.add(createEntityGroup(tenantId1, EntityType.DEVICE, i));
            groups.add(createEntityGroup(customer1.getId(), EntityType.DEVICE, i));
            groups.add(createEntityGroup(subCustomer2.getId(), EntityType.DEVICE, i));
        }

        PageLink pageLink = new PageLink(30, 0, "DEVICE", new SortOrder("name", SortOrder.Direction.ASC));
        PageData<EntityGroupInfo> tenantEntityGroupInfos1 = entityGroupInfoDao.findEntityGroupsByType(tenantId1.getId(), tenantId1.getId(),
                tenantId1.getEntityType(), EntityType.DEVICE, pageLink);
        Assert.assertEquals(20, tenantEntityGroupInfos1.getData().size());
        tenantEntityGroupInfos1.getData().forEach(entityGroupInfo -> this.validateOwners(entityGroupInfo, tenantOwners));

        PageData<EntityGroupInfo> tenantEntityGroupInfos2 = entityGroupInfoDao.findEntityGroupsByType(tenantId1.getId(), customer1.getId().getId(),
                customer1.getId().getEntityType(), EntityType.DEVICE, pageLink);
        Assert.assertEquals(20, tenantEntityGroupInfos2.getData().size());
        tenantEntityGroupInfos2.getData().forEach(entityGroupInfo -> this.validateOwners(entityGroupInfo, customer1Owners));

        PageData<EntityGroupInfo> tenantEntityGroupInfos3 = entityGroupInfoDao.findEntityGroupsByType(tenantId1.getId(), subCustomer2.getId().getId(),
                subCustomer2.getId().getEntityType(), EntityType.DEVICE, pageLink);
        Assert.assertEquals(20, tenantEntityGroupInfos3.getData().size());
        tenantEntityGroupInfos3.getData().forEach(entityGroupInfo -> this.validateOwners(entityGroupInfo, subCustomer2Owners));

    }

    private void validateOwners(EntityGroupInfo entityGroupInfo, Set<EntityId> ownerIds) {
        Assert.assertEquals(ownerIds, entityGroupInfo.getOwnerIds());
    }

    private EntityGroup createEntityGroup(EntityId ownerId, EntityType type, int index) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setId(new EntityGroupId(Uuids.timeBased()));
        entityGroup.setOwnerId(ownerId);
        entityGroup.setName(type.name() + "_GROUP_" + index);
        entityGroup.setType(type);
        return entityGroupDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, entityGroup);
    }

    private Customer createCustomer(TenantId tenantId, CustomerId parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(parentCustomerId);
        }
        customer.setTenantId(tenantId);
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(tenantId, customer);
    }

}
