// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EdgeProfileEdgeTest extends AbstractEdgeTest {

    @Test
    public void testProfileSwitchToDifferentRootPushesNewRootToEdge() throws Exception {
        RuleChain rc = new RuleChain();
        rc.setName("Root switch RC");
        rc.setType(RuleChainType.EDGE);
        RuleChain newRoot = doPost("/api/ruleChain", rc, RuleChain.class);
        EdgeProfile profile = new EdgeProfile();
        profile.setName("Root switch profile");
        profile.getProfileData().getRuleChainIds().add(newRoot.getId());
        profile.setRootRuleChainId(newRoot.getId());
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        edgeImitator.expectMessageAmount(1);
        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(profile.getId());
        doPost("/api/edge", reloaded, Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class).stream().anyMatch(msg ->
                        newRoot.getUuidId().equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))
                                && JacksonUtil.fromString(msg.getEntity(), RuleChain.class, true).isRoot()));
    }

    @Test
    public void testProfileSwitchDeletesOldRootOnlyAfterNewRootArrived() throws Exception {
        RuleChain first = new RuleChain();
        first.setName("Ordered switch RC 1");
        first.setType(RuleChainType.EDGE);
        RuleChain oldRoot = doPost("/api/ruleChain", first, RuleChain.class);
        RuleChain second = new RuleChain();
        second.setName("Ordered switch RC 2");
        second.setType(RuleChainType.EDGE);
        RuleChain newRoot = doPost("/api/ruleChain", second, RuleChain.class);

        EdgeProfile oldProfile = new EdgeProfile();
        oldProfile.setName("Ordered switch profile 1");
        oldProfile.getProfileData().getRuleChainIds().add(oldRoot.getId());
        oldProfile.setRootRuleChainId(oldRoot.getId());
        oldProfile = doPost("/api/edgeProfile", oldProfile, EdgeProfile.class);
        EdgeProfile newProfile = new EdgeProfile();
        newProfile.setName("Ordered switch profile 2");
        newProfile.getProfileData().getRuleChainIds().add(newRoot.getId());
        newProfile.setRootRuleChainId(newRoot.getId());
        newProfile = doPost("/api/edgeProfile", newProfile, EdgeProfile.class);

        UUID oldRootUuid = oldRoot.getUuidId();
        UUID newRootUuid = newRoot.getUuidId();

        edgeImitator.expectMessageAmount(1);
        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(oldProfile.getId());
        doPost("/api/edge", reloaded, Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> indexOfRootRuleChain(oldRootUuid) >= 0);

        edgeImitator.expectMessageAmount(1); // also clears everything recorded so far
        reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(newProfile.getId());
        doPost("/api/edge", reloaded, Edge.class);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                indexOfRootRuleChain(newRootUuid) >= 0 && indexOfRuleChainDelete(oldRootUuid) >= 0);

        // the edge cannot delete the chain it is rooted on, so the new root must arrive first
        Assert.assertTrue(indexOfRootRuleChain(newRootUuid) < indexOfRuleChainDelete(oldRootUuid));
    }

    @Test
    public void testEdgeRootOverridePushesRootFlagToEdge() throws Exception {
        RuleChain rc = new RuleChain();
        rc.setName("Edge root override RC");
        rc.setType(RuleChainType.EDGE);
        RuleChain ownRoot = doPost("/api/ruleChain", rc, RuleChain.class);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId() + "/" + ownRoot.getUuidId() + "/root", "", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class).stream().anyMatch(msg ->
                        ownRoot.getUuidId().equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))
                                && JacksonUtil.fromString(msg.getEntity(), RuleChain.class, true).isRoot()));
        Assert.assertEquals(ownRoot.getId(), doGet("/api/edge/" + edge.getUuidId(), Edge.class).getRootRuleChainId());
    }

    @Test
    public void testOwnerChangeUpToTenantKeepsGroupsTheTenantCanStillSee() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Upward Move Customer");
        customer = doPost("/api/customer", customer, Customer.class);

        EntityGroup group = new EntityGroup();
        group.setName("Upward Move group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customer.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        RuleChain rc = new RuleChain();
        rc.setName("Upward Move RC");
        rc.setType(RuleChainType.EDGE);
        rc = doPost("/api/ruleChain", rc, RuleChain.class);

        EdgeProfile profile = new EdgeProfile();
        profile.setName("Upward Move profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, savedGroup.getId()));
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(profile.getId());
        doPost("/api/edge", reloaded, Edge.class);

        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> groupOnEdge(savedGroup.getId()));

        // moving back up to the tenant removes no access, so the change-owner cleanup must leave the group alone
        doPost("/api/owner/TENANT/" + tenantId.getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(25, TimeUnit.SECONDS)
                .until(() -> groupOnEdge(savedGroup.getId()));
    }

    @Test
    public void testOwnerChangeSidewaysDropsProfileGroupsTheNewOwnerCannotSee() throws Exception {
        Customer customerA = new Customer();
        customerA.setTitle("Sideways Move Customer A");
        customerA = doPost("/api/customer", customerA, Customer.class);
        Customer customerB = new Customer();
        customerB.setTitle("Sideways Move Customer B");
        customerB = doPost("/api/customer", customerB, Customer.class);
        EntityGroup group = new EntityGroup();
        group.setName("Sideways Move group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customerA.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);
        RuleChain rc = new RuleChain();
        rc.setName("Sideways Move RC");
        rc.setType(RuleChainType.EDGE);
        rc = doPost("/api/ruleChain", rc, RuleChain.class);
        EdgeProfile profile = new EdgeProfile();
        profile.setName("Sideways Move profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, savedGroup.getId()));
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);
        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(profile.getId());
        doPost("/api/edge", reloaded, Edge.class);
        doPost("/api/owner/CUSTOMER/" + customerA.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> groupOnEdge(savedGroup.getId()));
        // customer B cannot see customer A's group, so the profile stamp alone must not keep it on the edge
        doPost("/api/owner/CUSTOMER/" + customerB.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        Awaitility.await().atMost(25, TimeUnit.SECONDS).until(() -> !groupOnEdge(savedGroup.getId()));
    }

    @Test
    public void testOwnerChangeUpToTenantResendsGroupsTheCustomerDeleteRemovesFromEdge() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Resend Move Customer");
        customer = doPost("/api/customer", customer, Customer.class);

        EntityGroup group = new EntityGroup();
        group.setName("Resend Move group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customer.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        RuleChain rc = new RuleChain();
        rc.setName("Resend Move RC");
        rc.setType(RuleChainType.EDGE);
        rc = doPost("/api/ruleChain", rc, RuleChain.class);

        EdgeProfile profile = new EdgeProfile();
        profile.setName("Resend Move profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, savedGroup.getId()));
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(profile.getId());
        doPost("/api/edge", reloaded, Edge.class);

        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> groupOnEdge(savedGroup.getId()));

        UUID customerUuid = customer.getId().getId();
        UUID groupUuid = savedGroup.getId().getId();
        edgeImitator.expectMessageAmount(1); // also clears everything recorded so far

        doPost("/api/owner/TENANT/" + tenantId.getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> indexOfCustomerDelete(customerUuid) >= 0);

        // The edge deletes the customer and everything it owns, so the group the cloud deliberately keeps assigned
        // has to be pushed again afterwards, or the two sides disagree until the next full sync.
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() ->
                indexOfGroupUpsert(groupUuid) > indexOfCustomerDelete(customerUuid));
    }

    @Test
    public void testOwnerChangeUpToTenantResendsSubCustomerGroupsTheCustomerDeleteRemovesFromEdge() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Subtree Move Customer");
        customer = doPost("/api/customer", customer, Customer.class);
        Customer subCustomer = new Customer();
        subCustomer.setTitle("Subtree Move Sub-Customer");
        subCustomer.setParentCustomerId(customer.getId());
        subCustomer = doPost("/api/customer", subCustomer, Customer.class);

        EntityGroup group = new EntityGroup();
        group.setName("Subtree Move group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(subCustomer.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        RuleChain rc = new RuleChain();
        rc.setName("Subtree Move RC");
        rc.setType(RuleChainType.EDGE);
        rc = doPost("/api/ruleChain", rc, RuleChain.class);

        EdgeProfile profile = new EdgeProfile();
        profile.setName("Subtree Move profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, savedGroup.getId()));
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(profile.getId());
        doPost("/api/edge", reloaded, Edge.class);

        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        UUID groupUuid = savedGroup.getId().getId();
        // wait for delivery, not just the relation, or the assignment itself shows up as a re-send once the log is cleared
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> groupOnEdge(savedGroup.getId()) && indexOfGroupUpsert(groupUuid) >= 0);

        UUID customerUuid = customer.getId().getId();
        UUID subCustomerUuid = subCustomer.getId().getId();
        edgeImitator.expectMessageAmount(1); // also clears everything recorded so far

        doPost("/api/owner/TENANT/" + tenantId.getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> indexOfCustomerDelete(customerUuid) >= 0);
        // the edge deletes the sub-customer and its group together with the customer, while the tenant edge still carries the
        // group; it has to come back afterwards, together with both customers, or the edge holds a row the cloud says it has
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() ->
                indexOfGroupUpsert(groupUuid) > indexOfCustomerDelete(customerUuid)
                        && indexOfCustomerUpsert(subCustomerUuid) > indexOfCustomerDelete(customerUuid));
        Assert.assertTrue(groupOnEdge(savedGroup.getId()));
    }

    @Test
    public void testOwnerChangeUpToTenantDropsCustomerSchedulerInsteadOfResendingIt() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Scheduler Move Customer");
        customer = doPost("/api/customer", customer, Customer.class);

        SchedulerEvent event = new SchedulerEvent();
        event.setName("Scheduler Move event");
        event.setType("irrigation");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", "UTC");
        schedule.set("repeat", JacksonUtil.valueToTree(new MonthlyRepeat()));
        event.setSchedule(schedule);
        event.setConfiguration(JacksonUtil.newObjectNode().put("msgType", TbMsgType.POST_ATTRIBUTES_REQUEST.name()));
        event.setOriginatorId(customer.getId());
        event.setCustomerId(customer.getId());
        event.setOwnerId(customer.getId());
        SchedulerEvent savedEvent = doPost("/api/schedulerEvent", event, SchedulerEvent.class);

        RuleChain rc = new RuleChain();
        rc.setName("Scheduler Move RC");
        rc.setType(RuleChainType.EDGE);
        rc = doPost("/api/ruleChain", rc, RuleChain.class);

        EdgeProfile profile = new EdgeProfile();
        profile.setName("Scheduler Move profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getSchedulerEventIds().add(savedEvent.getId());
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        Edge reloaded = doGet("/api/edge/" + edge.getUuidId(), Edge.class);
        reloaded.setEdgeProfileId(profile.getId());
        doPost("/api/edge", reloaded, Edge.class);
        // the edge is still tenant-owned, and a tenant edge never holds the customer the event needs
        Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> !schedulerOnEdge(savedEvent.getUuidId()));

        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        UUID customerUuid = customer.getId().getId();
        UUID eventUuid = savedEvent.getUuidId();
        // wait for delivery, not just the relation, or the assignment itself shows up as a re-send once the log is cleared
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> schedulerOnEdge(eventUuid) && indexOfSchedulerUpsert(eventUuid) >= 0);
        edgeImitator.expectMessageAmount(1); // also clears everything recorded so far

        doPost("/api/owner/TENANT/" + tenantId.getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> indexOfCustomerDelete(customerUuid) >= 0);
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !schedulerOnEdge(eventUuid));
        // unlike a device group, the event cannot outlive its customer on the edge, so nothing may re-send it after the delete
        Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .until(() -> indexOfSchedulerUpsert(eventUuid) < 0);
    }

    private int indexOfRootRuleChain(UUID ruleChainId) {
        List<AbstractMessage> msgs = edgeImitator.getDownlinkMsgs();
        for (int i = 0; i < msgs.size(); i++) {
            if (msgs.get(i) instanceof RuleChainUpdateMsg msg
                    && !UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE.equals(msg.getMsgType())
                    && !msg.getEntity().isEmpty()
                    && ruleChainId.equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))
                    && JacksonUtil.fromString(msg.getEntity(), RuleChain.class, true).isRoot()) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfRuleChainDelete(UUID ruleChainId) {
        List<AbstractMessage> msgs = edgeImitator.getDownlinkMsgs();
        for (int i = 0; i < msgs.size(); i++) {
            if (msgs.get(i) instanceof RuleChainUpdateMsg msg
                    && UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE.equals(msg.getMsgType())
                    && ruleChainId.equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfCustomerDelete(UUID customerId) {
        List<AbstractMessage> msgs = edgeImitator.getDownlinkMsgs();
        for (int i = 0; i < msgs.size(); i++) {
            if (msgs.get(i) instanceof CustomerUpdateMsg msg
                    && UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE.equals(msg.getMsgType())
                    && customerId.equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfCustomerUpsert(UUID customerId) {
        List<AbstractMessage> msgs = edgeImitator.getDownlinkMsgs();
        for (int i = msgs.size() - 1; i >= 0; i--) {
            if (msgs.get(i) instanceof CustomerUpdateMsg msg
                    && !UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE.equals(msg.getMsgType())
                    && customerId.equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfGroupUpsert(UUID groupId) {
        List<AbstractMessage> msgs = edgeImitator.getDownlinkMsgs();
        for (int i = msgs.size() - 1; i >= 0; i--) {
            if (msgs.get(i) instanceof EntityGroupUpdateMsg msg
                    && !UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE.equals(msg.getMsgType())
                    && groupId.equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfSchedulerUpsert(UUID schedulerEventId) {
        List<AbstractMessage> msgs = edgeImitator.getDownlinkMsgs();
        for (int i = msgs.size() - 1; i >= 0; i--) {
            if (msgs.get(i) instanceof SchedulerEventUpdateMsg msg
                    && !UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE.equals(msg.getMsgType())
                    && schedulerEventId.equals(new UUID(msg.getIdMSB(), msg.getIdLSB()))) {
                return i;
            }
        }
        return -1;
    }

    private boolean schedulerOnEdge(UUID schedulerEventId) throws Exception {
        List<SchedulerEventInfo> events = doGetTyped("/api/edge/" + edge.getId().getId() + "/allSchedulerEvents", new TypeReference<>() {});
        return events.stream().anyMatch(e -> e.getUuidId().equals(schedulerEventId));
    }

    private boolean groupOnEdge(EntityGroupId groupId) throws Exception {
        PageData<EntityGroupInfo> groups = doGetTypedWithPageLink("/api/entityGroups/edge/" + edge.getId().getId() + "/DEVICE?",
                new TypeReference<>() {}, new PageLink(100));
        return groups.getData().stream().anyMatch(g -> g.getId().equals(groupId));
    }

}
