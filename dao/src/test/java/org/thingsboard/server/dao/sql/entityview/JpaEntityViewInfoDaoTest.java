// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.entityview;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.entityview.EntityViewInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JpaEntityViewInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EntityViewInfoDao entityViewInfoDao;

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    private CustomerDao customerDao;

    private List<EntityView> entityViews = new ArrayList<>();

    @After
    public void tearDown() {
        for (EntityView entityView : entityViews) {
            entityViewDao.removeById(entityView.getTenantId(), entityView.getUuidId());
        }
        entityViews.clear();
    }

    @Test
    public void testFindEntityViewInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            entityViews.add(createEntityView(tenantId1, null, i));
            entityViews.add(createEntityView(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "ENTITY_VIEW");
        PageData<EntityViewInfo> entityViewInfos1 = entityViewInfoDao.findEntityViewsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, entityViewInfos1.getData().size());

        PageData<EntityViewInfo> entityViewInfos2 = entityViewInfoDao.findEntityViewsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, entityViewInfos2.getData().size());
    }

    @Test
    public void testFindEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            entityViews.add(createEntityView(tenantId1, customer1.getUuidId(), i));
            entityViews.add(createEntityView(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "ENTITY_VIEW", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<EntityViewInfo> entityViewInfos1 = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, entityViewInfos1.getData().size());
        entityViewInfos1.getData().forEach(entityViewInfo -> Assert.assertNotEquals("CUSTOMER_0", entityViewInfo.getOwnerName()));

        PageData<EntityViewInfo> entityViewInfos2 = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, entityViewInfos2.getData().size());

        PageData<EntityViewInfo> entityViewInfos3 = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, entityViewInfos3.getData().size());
    }

    @Test
    public void testFindEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomersThreeLevels() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);
        Customer subSubCustomer3 = createCustomer(tenantId1, subCustomer2.getUuidId(), 2);

        for (int i = 0; i < 5; i++) {
            entityViews.add(createEntityView(tenantId1, customer1.getUuidId(), i));
            entityViews.add(createEntityView(tenantId1, subCustomer2.getUuidId(), 5 + i));
            entityViews.add(createEntityView(tenantId1, subSubCustomer3.getUuidId(), 10 + i));
        }

        PageLink pageLink = new PageLink(30, 0, "ENTITY_VIEW", new SortOrder("ownerName", SortOrder.Direction.ASC));

        // From the root: all 15 entity views (own + child + grandchild) are included
        PageData<EntityViewInfo> fromRoot = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(15, fromRoot.getData().size());
        fromRoot.getData().forEach(entityViewInfo -> Assert.assertNotEquals("CUSTOMER_0", entityViewInfo.getOwnerName()));

        // From the middle: own + grandchild only (10)
        PageData<EntityViewInfo> fromMiddle = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(10, fromMiddle.getData().size());

        // From the leaf (no children): only its own 5
        PageData<EntityViewInfo> fromLeaf = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subSubCustomer3.getUuidId(), pageLink);
        Assert.assertEquals(5, fromLeaf.getData().size());
    }

    @Test
    public void testFindEntityViewInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);

        String type = "thermostat";
        for (int i = 0; i < 10; i++) {
            entityViews.add(createEntityView(tenantId1, customer1.getUuidId(), type, i));
            entityViews.add(createEntityView(tenantId1, subCustomer2.getUuidId(), type, 10 + i));
            // Different type owned by the sub-customer: must be excluded by the type filter.
            entityViews.add(createEntityView(tenantId1, subCustomer2.getUuidId(), "other", 100 + i));
        }

        PageLink pageLink = new PageLink(30, 0, "ENTITY_VIEW", new SortOrder("ownerName", SortOrder.Direction.ASC));

        // From the parent: own + sub-customer entity views of the given type (20), excluding the "other" type.
        PageData<EntityViewInfo> fromParent = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId1, customer1.getUuidId(), type, pageLink);
        Assert.assertEquals(20, fromParent.getTotalElements());
        fromParent.getData().forEach(entityViewInfo -> Assert.assertEquals(type, entityViewInfo.getType()));
        fromParent.getData().forEach(entityViewInfo -> Assert.assertNotEquals("CUSTOMER_0", entityViewInfo.getOwnerName()));

        // From the sub-customer (a leaf): only its own 10 of the given type.
        PageData<EntityViewInfo> fromSubCustomer = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), type, pageLink);
        Assert.assertEquals(10, fromSubCustomer.getTotalElements());
        fromSubCustomer.getData().forEach(entityViewInfo -> Assert.assertEquals(type, entityViewInfo.getType()));
    }

    @Test
    public void testFindEntityViewInfosByTenantIdAndNonExistentCustomerIdAndTypeIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        // Customer that does not exist (covers the Citus empty-Optional short-circuit, avoiding an IN () query)
        UUID nonExistentCustomerId = Uuids.timeBased();

        for (int i = 0; i < 10; i++) {
            entityViews.add(createEntityView(tenantId1, null, "thermostat", i));
        }

        PageLink pageLink = new PageLink(30, 0, "ENTITY_VIEW", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<EntityViewInfo> entityViewInfos = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId1, nonExistentCustomerId, "thermostat", pageLink);
        Assert.assertTrue(entityViewInfos.getData().isEmpty());
        Assert.assertEquals(0, entityViewInfos.getTotalElements());
    }

    @Test
    public void testFindEntityViewInfosByTenantIdAndNonExistentCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        // Customer that does not exist (covers the Citus empty-Optional short-circuit, avoiding an IN () query)
        UUID nonExistentCustomerId = Uuids.timeBased();

        for (int i = 0; i < 10; i++) {
            entityViews.add(createEntityView(tenantId1, null, i));
        }

        PageLink pageLink = new PageLink(30, 0, "ENTITY_VIEW", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<EntityViewInfo> entityViewInfos = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, nonExistentCustomerId, pageLink);
        Assert.assertTrue(entityViewInfos.getData().isEmpty());
        Assert.assertEquals(0, entityViewInfos.getTotalElements());
    }

    private EntityView createEntityView(UUID tenantId, UUID customerId, int index) {
        return this.createEntityView(tenantId, customerId, null, index);
    }

    private EntityView createEntityView(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        EntityView entityView = new EntityView();
        entityView.setId(new EntityViewId(Uuids.timeBased()));
        entityView.setTenantId(TenantId.fromUUID(tenantId));
        entityView.setCustomerId(new CustomerId(customerId));
        entityView.setName("ENTITY_VIEW_" + index);
        entityView.setType(type);
        entityView.setEntityId(new DeviceId(Uuids.timeBased()));
        return entityViewDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, entityView);
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
