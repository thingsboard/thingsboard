// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.AbstractMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.menu.CustomMenuDao;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomMenuProto;
import org.thingsboard.server.gen.edge.v1.CustomTranslationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class WhiteLabelingEdgeTest extends AbstractEdgeTest {

    private static List<UUID> idsToRemove = new ArrayList<>();

    @Autowired
    private CustomMenuDao customMenuDao;

    @After
    public void teardown() throws Exception {
        customMenuDao.removeAllByIds(idsToRemove);
        idsToRemove = new ArrayList<>();
    }

    @Test
    public void testWhiteLabeling() throws Exception {
        testWhiteLabeling_sysAdmin();
        testWhiteLabeling_tenant();
        testWhiteLabeling_customer();
        resetSysAdminWhiteLabelingSettings();
    }

    private void testWhiteLabeling_sysAdmin() throws Exception {
        loginSysAdmin();
        updateAndVerifyWhiteLabelingUpdate("Sys Admin TB Updated");
    }

    private void testWhiteLabeling_tenant() throws Exception {
        loginTenantAdmin();

        updateAndVerifyWhiteLabelingUpdate("Tenant TB Updated");
    }

    private void testWhiteLabeling_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyWhiteLabelingUpdate("Customer A TB Updated");

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyWhiteLabelingUpdate("Sub Customer A TB Updated");
    }

    private void updateAndVerifyWhiteLabelingUpdate(String updatedAppTitle) throws Exception {
        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
        edgeImitator.expectMessageAmount(1);
        whiteLabelingParams.setAppTitle(updatedAppTitle);
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WhiteLabelingProto);
        WhiteLabelingProto login = (WhiteLabelingProto) latestMessage;
        WhiteLabeling whiteLabeling = JacksonUtil.fromString(login.getEntity(), WhiteLabeling.class, true);
        Assert.assertNotNull(whiteLabeling);
        WhiteLabelingParams result = JacksonUtil.treeToValue(whiteLabeling.getSettings(), WhiteLabelingParams.class);
        Assert.assertEquals(updatedAppTitle, result.getAppTitle());

        resetWlAndVerify("/api/whiteLabel/currentWhiteLabelParams");
    }

    @Test
    public void testLoginWhiteLabeling() throws Exception {
        testLoginWhiteLabeling_sysAdmin();
        testLoginWhiteLabeling_tenant();
        testLoginWhiteLabeling_customer();
        resetSysAdminWhiteLabelingSettings();
    }

    private void testLoginWhiteLabeling_sysAdmin() throws Exception {
        loginSysAdmin();
        updateAndVerifySystemLoginWhiteLabelingUpdate();
    }

    private void testLoginWhiteLabeling_tenant() throws Exception {
        loginTenantAdmin();

        updateAndVerifyLoginWhiteLabelingUpdate(StringUtils.randomAlphanumeric(5).toLowerCase() + "tenant.updated.org");
    }

    private void testLoginWhiteLabeling_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyLoginWhiteLabelingUpdate(savedCustomerA.getId() + "customer.a.updated.org");

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyLoginWhiteLabelingUpdate(savedSubCustomerA.getId() + "sub.customer.a.updated.org");
    }

    private void updateAndVerifySystemLoginWhiteLabelingUpdate() throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        edgeImitator.expectMessageAmount(1);
        loginWhiteLabelingParams.setPageBackgroundColor("pink");
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WhiteLabelingProto);
        WhiteLabelingProto login = (WhiteLabelingProto) latestMessage;
        WhiteLabeling whiteLabeling = JacksonUtil.fromString(login.getEntity(), WhiteLabeling.class, true);
        Assert.assertNotNull(whiteLabeling);
        LoginWhiteLabelingParams result = JacksonUtil.treeToValue(whiteLabeling.getSettings(), LoginWhiteLabelingParams.class);
        Assert.assertEquals("pink", result.getPageBackgroundColor());
    }

    private void updateAndVerifyLoginWhiteLabelingUpdate(String updatedDomainName) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        edgeImitator.expectMessageAmount(1);

        Domain domain = constructDomain(updatedDomainName);
        Domain savedDomain = doPost("/api/domain", domain, Domain.class);
        loginWhiteLabelingParams.setDomainId(savedDomain.getId());
        loginWhiteLabelingParams.setBaseUrl("https://" + updatedDomainName);
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WhiteLabelingProto);
        WhiteLabelingProto login = (WhiteLabelingProto) latestMessage;
        WhiteLabeling whiteLabeling = JacksonUtil.fromString(login.getEntity(), WhiteLabeling.class, true);
        Assert.assertNotNull(whiteLabeling);
        LoginWhiteLabelingParams result = JacksonUtil.treeToValue(whiteLabeling.getSettings(), LoginWhiteLabelingParams.class);
        Assert.assertEquals(savedDomain.getId(), result.getDomainId());

        resetWlAndVerify("/api/whiteLabel/currentLoginWhiteLabelParams");
    }

    private void resetWlAndVerify(String urlTemplate) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete(urlTemplate);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WhiteLabelingProto);
        WhiteLabelingProto login = (WhiteLabelingProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, login.getMsgType());
    }

    @Test
    public void testCustomTranslation() throws Exception {
        testCustomTranslation_sysAdmin();
        testCustomTranslation_tenant();
        testCustomTranslation_customer();
        resetSysAdminWhiteLabelingSettings();
    }

    private Domain constructDomain(String domainName) {
        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setOauth2Enabled(true);
        domain.setPropagateToEdge(true);
        return domain;
    }

    private void testCustomTranslation_sysAdmin() throws Exception {
        loginSysAdmin();
        updateAndVerifyCustomTranslationUpdate("sys_admin_value_updated");
    }

    private void testCustomTranslation_tenant() throws Exception {
        loginTenantAdmin();
        updateAndVerifyCustomTranslationUpdate("tenant_value_updated");
    }

    private void testCustomTranslation_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyCustomTranslationUpdate("customer_value_updated");

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyCustomTranslationUpdate("sub_customer_value_updated");
    }

    private void createCustomerUserAndLogin(Customer customer, String email) throws Exception {
        edgeImitator.expectMessageAmount(2);
        User customerAUser = new User();
        customerAUser.setAuthority(Authority.CUSTOMER_USER);
        customerAUser.setTenantId(TenantId.SYS_TENANT_ID);
        customerAUser.setCustomerId(customer.getId());
        customerAUser.setEmail(email);
        EntityGroupInfo customerAdminsGroup = findCustomerAdminsGroup(customer);
        User savedCustomerUser = createUser(customerAUser, "customer", customerAdminsGroup.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());  // user update msg and user credentials update msg

        loginUser(savedCustomerUser.getEmail(), "customer");
    }

    private void updateAndVerifyCustomTranslationUpdate(String updatedHomeValue) throws Exception {
        // create custom translation for en_US
        edgeImitator.expectMessageAmount(1);
        JsonNode jsonNode = JacksonUtil.toJsonNode("{\"home\":\"myHome\", \"update\":\"system\" ," +
                " \"remove\":\"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/en_US", jsonNode);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomTranslationUpdateMsg);
        CustomTranslationUpdateMsg customTranslationUpdateMsg = (CustomTranslationUpdateMsg) latestMessage;
        Assert.assertEquals(customTranslationUpdateMsg.getMsgType(), UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);
        CustomTranslation ct = JacksonUtil.fromString(customTranslationUpdateMsg.getEntity(), CustomTranslation.class, true);
        Assert.assertNotNull(ct);
        Assert.assertEquals(jsonNode, ct.getValue());

        // update custom translation for en_US
        edgeImitator.expectMessageAmount(1);
        JsonNode updatedJsonNode = JacksonUtil.toJsonNode("{\"home\":\"" + updatedHomeValue + "\", \"update\":\"system\" ," +
                " \"remove\":\"system\", \"search\":\"system\"}");
        doPost("/api/translation/custom/en_US", updatedJsonNode);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomTranslationUpdateMsg);
        customTranslationUpdateMsg = (CustomTranslationUpdateMsg) latestMessage;
        Assert.assertEquals(customTranslationUpdateMsg.getMsgType(), UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);
        ct = JacksonUtil.fromString(customTranslationUpdateMsg.getEntity(), CustomTranslation.class, true);
        Assert.assertNotNull(ct);
        Assert.assertEquals(updatedJsonNode, ct.getValue());

        // delete custom translation for en_US
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/translation/custom/en_US");
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomTranslationUpdateMsg);
        customTranslationUpdateMsg = (CustomTranslationUpdateMsg) latestMessage;
        Assert.assertEquals(customTranslationUpdateMsg.getMsgType(), UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        ct = JacksonUtil.fromString(customTranslationUpdateMsg.getEntity(), CustomTranslation.class, true);
        Assert.assertNotNull(ct);
        Assert.assertEquals(updatedJsonNode, ct.getValue());
    }

    @Test
    public void testCustomMenu() throws Exception {
        testCustomMenu_tenant();
        testCustomMenu_customer();
        resetSysAdminWhiteLabelingSettings();
    }

    private void testCustomMenu_tenant() throws Exception {
        loginTenantAdmin();
        updateAndVerifyCustomMenuUpdate("Tenant custom menu", CMScope.TENANT);
    }

    private void testCustomMenu_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyCustomMenuUpdate("customer_value_updated", CMScope.CUSTOMER);

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyCustomMenuUpdate("sub_customer_value_updated", CMScope.CUSTOMER);
    }

    private void updateAndVerifyCustomMenuUpdate(String customMenuName, CMScope scope) throws Exception {
        edgeImitator.expectMessageAmount(1);

        CustomMenu menu = new CustomMenu();
        menu.setName(customMenuName);
        menu.setScope(scope);
        menu.setAssigneeType(CMAssigneeType.ALL);
        menu = doPost("/api/customMenu", menu, CustomMenu.class);
        idsToRemove.add(menu.getUuidId());

        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomMenuProto);
        CustomMenuProto customMenuProto = (CustomMenuProto) latestMessage;
        CustomMenu cm = JacksonUtil.fromString(customMenuProto.getEntity(), CustomMenu.class, true);
        Assert.assertNotNull(cm);
        Assert.assertEquals(menu.getName(), cm.getName());
        Assert.assertEquals(menu.getScope(), cm.getScope());
        Assert.assertEquals(menu.getAssigneeType(), cm.getAssigneeType());
    }

}
