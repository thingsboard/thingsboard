// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.subscription.TbAttributeSubscriptionScope;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EdgeTest extends AbstractEdgeTest {

    @Test
    public void testChangeOwner_fromTenantToCustomer_andFromCustomerToTenant() throws Exception {
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        // create sub customer
        saveCustomer("Edge Sub Customer", savedCustomer.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        EntityGroup deviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "EdgeDeviceGroup", tenantId);
        EntityGroup assetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "EdgeAssetGroup", tenantId);
        EntityGroup entityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "EdgeEntityViewGroup", tenantId);
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "EdgeDashboardGroup", tenantId);
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "EdgeUserGroup", tenantId);

        // change edge owner from tenant to customer
        changeEdgeOwnerToCustomer(savedCustomer);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        createEntityGroupAndAssignToEdge(EntityType.DEVICE, "CustomerEdgeDeviceGroup", savedCustomer.getId());
        createEntityGroupAndAssignToEdge(EntityType.ASSET, "CustomerEdgeAssetGroup", savedCustomer.getId());
        createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "CustomerEdgeEntityViewGroup", savedCustomer.getId());
        createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "CustomerEdgeDashboardGroup", savedCustomer.getId());
        createEntityGroupAndAssignToEdge(EntityType.USER, "CustomerEdgeUserGroup", savedCustomer.getId());

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 7);

        // remove tenant entity groups
        doDelete("/api/entityGroup/" + deviceEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + assetEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityViewEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + dashboardEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + userEntityGroup.getUuidId()).andExpect(status().isOk());

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testChangeOwner_fromTenantToSubCustomer_andFromSubCustomerToTenant() throws Exception {
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        // create sub customer
        Customer savedSubCustomer = saveCustomer("Edge Sub Customer", savedCustomer.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer", savedSubCustomer.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        EntityGroup deviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "EdgeDeviceGroup", tenantId);
        EntityGroup assetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "EdgeAssetGroup", tenantId);
        EntityGroup entityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "EdgeEntityViewGroup", tenantId);
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "EdgeDashboardGroup", tenantId);
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "EdgeUserGroup", tenantId);

        // change edge owner from tenant to sub customer
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomer, savedSubCustomer);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        EntityGroup customerDeviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "CustomerEdgeDeviceGroup", savedCustomer.getId());
        EntityGroup customerAssetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "CustomerEdgeAssetGroup", savedCustomer.getId());
        EntityGroup customerEntityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "CustomerEdgeEntityViewGroup", savedCustomer.getId());
        EntityGroup customerDashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "CustomerEdgeDashboardGroup", savedCustomer.getId());
        EntityGroup customerUserEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "CustomerEdgeUserGroup", savedCustomer.getId());

        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        EntityGroup subCustomerDeviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "SubCustomerEdgeDeviceGroup", savedSubCustomer.getId());
        EntityGroup subCustomerAssetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "SubCustomerEdgeAssetGroup", savedSubCustomer.getId());
        EntityGroup subCustomerEntityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "SubCustomerEdgeEntityViewGroup", savedSubCustomer.getId());
        EntityGroup subCustomerDashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "SubCustomerEdgeDashboardGroup", savedSubCustomer.getId());
        EntityGroup subCustomerUserEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "SubCustomerEdgeUserGroup", savedSubCustomer.getId());

        // change owner to tenant
        changeEdgeOwnerFromSubCustomerToTenant(savedCustomer, savedSubCustomer, 14);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // validate that customer and sub customer were unassigned from edge
        validateThatEntityGroupNotAssignedToEdge(customerDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(customerAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(customerEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(customerDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(customerUserEntityGroup.getId(), EntityType.USER);

        validateThatEntityGroupNotAssignedToEdge(subCustomerDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(subCustomerAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(subCustomerEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(subCustomerDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(subCustomerUserEntityGroup.getId(), EntityType.USER);

        // remove tenant entity groups
        doDelete("/api/entityGroup/" + deviceEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + assetEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityViewEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + dashboardEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + userEntityGroup.getUuidId()).andExpect(status().isOk());

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testChangeOwner_fromCustomerToSubCustomer_andFromSubCustomerToCustomer() throws Exception {
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        // create sub customer
        Customer savedSubCustomer = saveCustomer("Edge Sub Customer", savedCustomer.getId());
        // create sub sub customer
        saveCustomer("Edge Sub Sub Customer", savedSubCustomer.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        EntityGroup deviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "EdgeDeviceGroup", tenantId);
        EntityGroup assetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "EdgeAssetGroup", tenantId);
        EntityGroup entityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "EdgeEntityViewGroup", tenantId);
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "EdgeDashboardGroup", tenantId);
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "EdgeUserGroup", tenantId);

        // change owner from tenant to parent customer
        changeEdgeOwnerToCustomer(savedCustomer);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        EntityGroup customerDeviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "CustomerEdgeDeviceGroup", savedCustomer.getId());
        EntityGroup customerAssetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "CustomerEdgeAssetGroup", savedCustomer.getId());
        EntityGroup customerEntityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "CustomerEdgeEntityViewGroup", savedCustomer.getId());
        EntityGroup customerDashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "CustomerEdgeDashboardGroup", savedCustomer.getId());
        EntityGroup customerUserEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "CustomerEdgeUserGroup", savedCustomer.getId());

        // change owner to sub customer
        changeEdgeOwnerToCustomer(savedSubCustomer);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // validate that customer groups are still on edge
        validateThatEntityGroupAssignedToEdge(customerDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(customerAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(customerEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(customerDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(customerUserEntityGroup.getId(), EntityType.USER);

        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        EntityGroup subCustomerDeviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "SubCustomerEdgeDeviceGroup", savedSubCustomer.getId());
        EntityGroup subCustomerAssetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "SubCustomerEdgeAssetGroup", savedSubCustomer.getId());
        EntityGroup subCustomerEntityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "SubCustomerEdgeEntityViewGroup", savedSubCustomer.getId());
        EntityGroup subCustomerDashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "SubCustomerEdgeDashboardGroup", savedSubCustomer.getId());
        EntityGroup subCustomerUserEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "SubCustomerEdgeUserGroup", savedSubCustomer.getId());

        // change owner from sub customer to parent customer
        changeEdgeOwnerFromSubCustomerToCustomer(savedCustomer, savedSubCustomer, 7);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // validate that customer groups are still on edge
        validateThatEntityGroupAssignedToEdge(customerDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(customerAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(customerEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(customerDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(customerUserEntityGroup.getId(), EntityType.USER);

        // validate that sub customer groups unassigned from edge
        validateThatEntityGroupNotAssignedToEdge(subCustomerDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(subCustomerAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(subCustomerEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(subCustomerDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(subCustomerUserEntityGroup.getId(), EntityType.USER);

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 7);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // validate that customer entity group were deleted from edge
        validateThatEntityGroupNotAssignedToEdge(customerDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(customerAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(customerEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(customerDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(customerUserEntityGroup.getId(), EntityType.USER);

        // remove tenant entity groups
        doDelete("/api/entityGroup/" + deviceEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + assetEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityViewEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + dashboardEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + userEntityGroup.getUuidId()).andExpect(status().isOk());

        // validate no tenant groups on edge
        validateThatEntityGroupNotAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testChangeOwner_fromSubCustomerAToCustomerB_andFromCustomerBToCustomerA() throws Exception {
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create customer B
        Customer savedCustomerB = saveCustomer("Edge Customer B", null);
        // create sub customer B
        saveCustomer("Edge Sub Customer B", savedCustomerB.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        EntityGroup deviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "EdgeDeviceGroup", tenantId);
        EntityGroup assetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "EdgeAssetGroup", tenantId);
        EntityGroup entityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "EdgeEntityViewGroup", tenantId);
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "EdgeDashboardGroup", tenantId);
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "EdgeUserGroup", tenantId);

        // change owner from tenant to customer A
        changeEdgeOwnerToCustomer(savedCustomerA);

        // create device, asset, entity view, dashboard, user entity groups on customer A level and assign to edge
        EntityGroup customerADeviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "CustomerAEdgeDeviceGroup", savedCustomerA.getId());
        EntityGroup customerAAssetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "CustomerAEdgeAssetGroup", savedCustomerA.getId());
        EntityGroup customerAEntityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "CustomerAEdgeEntityViewGroup", savedCustomerA.getId());
        EntityGroup customerADashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "CustomerAEdgeDashboardGroup", savedCustomerA.getId());
        EntityGroup customerAUserEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "CustomerAEdgeUserGroup", savedCustomerA.getId());

        // change owner from tenant to customer B
        changeEdgeOwnerFromCustomerToCustomer(savedCustomerA, savedCustomerB, 7);

        // validate that customer A groups unassigned from edge
        validateThatEntityGroupNotAssignedToEdge(customerADeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(customerAAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(customerAEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(customerADashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(customerAUserEntityGroup.getId(), EntityType.USER);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // create device, asset, entity view, dashboard, user entity groups on customer B level and assign to edge
        EntityGroup customerBDeviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "CustomerBEdgeDeviceGroup", savedCustomerB.getId());
        EntityGroup customerBAssetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "CustomerBEdgeAssetGroup", savedCustomerB.getId());
        EntityGroup customerBEntityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "CustomerBEdgeEntityViewGroup", savedCustomerB.getId());
        EntityGroup customerBDashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "CustomerBEdgeDashboardGroup", savedCustomerB.getId());
        EntityGroup customerBUserEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "CustomerBEdgeUserGroup", savedCustomerB.getId());

        // change owner from customer B to customer A
        changeEdgeOwnerFromCustomerToCustomer(savedCustomerB, savedCustomerA, 7);

        // validate that customer B groups unassigned from edge
        validateThatEntityGroupNotAssignedToEdge(customerBDeviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(customerBAssetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(customerBEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(customerBDashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(customerBUserEntityGroup.getId(), EntityType.USER);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomerA, 2);

        // validate that tenant groups are still on edge
        validateThatEntityGroupAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // remove tenant entity groups
        doDelete("/api/entityGroup/" + deviceEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + assetEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityViewEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + dashboardEntityGroup.getUuidId()).andExpect(status().isOk());
        doDelete("/api/entityGroup/" + userEntityGroup.getUuidId()).andExpect(status().isOk());

        // validate no tenant groups on edge
        validateThatEntityGroupNotAssignedToEdge(deviceEntityGroup.getId(), EntityType.DEVICE);
        validateThatEntityGroupNotAssignedToEdge(assetEntityGroup.getId(), EntityType.ASSET);
        validateThatEntityGroupNotAssignedToEdge(entityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);
        validateThatEntityGroupNotAssignedToEdge(dashboardEntityGroup.getId(), EntityType.DASHBOARD);
        validateThatEntityGroupNotAssignedToEdge(userEntityGroup.getId(), EntityType.USER);

        // delete customers
        doDelete("/api/customer/" + savedCustomerA.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/customer/" + savedCustomerB.getUuidId())
                .andExpect(status().isOk());

    }

    @Test
    public void testSyncEdge_attributeUpdated() throws Exception {
        getWsClient().subscribeForAttributes(edge.getId(), TbAttributeSubscriptionScope.SERVER_SCOPE.name(), List.of(DataConstants.EDGE_SYNC_IN_PROGRESS_ATTR_KEY));

        doPost("/api/edge/sync/" + edge.getId());

        // wait for sync to start
        waitForEdgeSyncInProgressEqualsValue(true);

        // wait for sync to end
        waitForEdgeSyncInProgressEqualsValue(false);
    }

    private void waitForEdgeSyncInProgressEqualsValue(Boolean value) {
        getWsClient().registerWaitForUpdate();
        JsonNode update = JacksonUtil.toJsonNode(getWsClient().waitForUpdate());
        assertThat(update.get("data").get(DataConstants.EDGE_SYNC_IN_PROGRESS_ATTR_KEY).get(0).get(1).asBoolean()).isEqualTo(value);
    }
}
