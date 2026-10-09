// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class ReportTemplateEdgeTest extends AbstractEdgeTest {

    @Test
    public void testReportTemplate_tenantLevel() throws Exception {
        ReportTemplate reportTemplate = createReportTemplate("Edge Report Template", tenantId);

        edgeImitator.expectMessageAmount(1);
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedReportTemplate, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        edgeImitator.expectMessageAmount(1);
        savedReportTemplate.setDescription("Edge Report Template Updated");
        savedReportTemplate = doPost("/api/reportTemplate", savedReportTemplate, ReportTemplate.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedReportTemplate, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        edgeImitator.expectMessageAmount(1);
        doDelete("/api/reportTemplate/" + savedReportTemplate.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareDeletedExpectedAndActual(savedReportTemplate);
    }

    @Test
    public void testReportTemplate_customerLevel() throws Exception {
        Customer savedCustomer = createCustomerAndChangeEdgeOwner();
        ReportTemplate reportTemplate = createReportTemplate("Edge Customer Report Template", savedCustomer.getId());

        edgeImitator.expectMessageAmount(1);
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedReportTemplate, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        edgeImitator.expectMessageAmount(1);
        savedReportTemplate.setName("Edge Customer Report Template Updated");
        savedReportTemplate = doPost("/api/reportTemplate", savedReportTemplate, ReportTemplate.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedReportTemplate, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        edgeImitator.expectMessageAmount(1);
        doDelete("/api/reportTemplate/" + savedReportTemplate.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareDeletedExpectedAndActual(savedReportTemplate);

        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testReportTemplate_toCloud_tenantLevel() throws Exception {
        ReportTemplate reportTemplate = createReportTemplate("Edge Report Template To Cloud", tenantId);
        reportTemplate.setId(new ReportTemplateId(UUID.randomUUID()));

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUplinkMsg(reportTemplate));
        Assert.assertTrue(edgeImitator.waitForResponses());
        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class);
        compareExpectedAndActual(reportTemplate, foundReportTemplate);

        reportTemplate.setDescription("Edge Report Template To Cloud Updated");
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUpdateUplinkMsg(reportTemplate));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        foundReportTemplate = doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class);
        compareExpectedAndActual(reportTemplate, foundReportTemplate);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createDeleteUplinkMsg(reportTemplate));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class, status().isNotFound());
        });
    }

    @Test
    public void testReportTemplate_toCloud_customerLevel() throws Exception {
        Customer savedCustomer = createCustomerAndChangeEdgeOwner();
        ReportTemplate reportTemplate = createReportTemplate("Edge Customer Report Template To Cloud", savedCustomer.getId());
        reportTemplate.setId(new ReportTemplateId(UUID.randomUUID()));

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUplinkMsg(reportTemplate));
        Assert.assertTrue(edgeImitator.waitForResponses());
        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class);
        compareExpectedAndActual(reportTemplate, foundReportTemplate);

        reportTemplate.setName("Edge Customer Report Template To Cloud Updated");
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUpdateUplinkMsg(reportTemplate));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        foundReportTemplate = doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class);
        compareExpectedAndActual(reportTemplate, foundReportTemplate);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createDeleteUplinkMsg(reportTemplate));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class, status().isNotFound());
        });

        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    private UplinkMsg createDeleteUplinkMsg(ReportTemplate reportTemplate) throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addReportTemplateUpdateMsg(
                EdgeMsgConstructorUtils.constructReportTemplateDeleteMsg(reportTemplate.getId()));
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        return uplinkMsgBuilder.build();
    }

    private ReportTemplate createReportTemplate(String name, EntityId ownerId) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setOwnerId(ownerId);
        reportTemplate.setName(name);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);
        reportTemplate.setDescription("Edge report template description");

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(new ArrayList<>());
        configuration.setFilters(new ArrayList<>());
        HeadingComponent headingComponent = new HeadingComponent();
        headingComponent.setValue("Edge Heading");
        configuration.setComponents(List.of(headingComponent));
        reportTemplate.setConfiguration(configuration);
        return reportTemplate;
    }

    private void compareExpectedAndActual(ReportTemplate expected, UpdateMsgType expectedMsgType) {
        Optional<ReportTemplateUpdateMsg> msgOpt = edgeImitator.findMessageByType(ReportTemplateUpdateMsg.class);
        Assert.assertTrue(msgOpt.isPresent());
        ReportTemplateUpdateMsg actualMsg = msgOpt.get();
        Assert.assertEquals(expectedMsgType, actualMsg.getMsgType());
        Assert.assertEquals(expected.getUuidId().getMostSignificantBits(), actualMsg.getIdMSB());
        Assert.assertEquals(expected.getUuidId().getLeastSignificantBits(), actualMsg.getIdLSB());
        ReportTemplate actual = JacksonUtil.fromString(actualMsg.getEntity(), ReportTemplate.class, true);
        compareExpectedAndActual(expected, actual);
    }

    private void compareExpectedAndActual(ReportTemplate expected, ReportTemplate actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getDescription(), actual.getDescription());
        Assert.assertEquals(expected.getType(), actual.getType());
        Assert.assertEquals(expected.getFormat(), actual.getFormat());
        Assert.assertEquals(expected.getTenantId(), actual.getTenantId());
        Assert.assertEquals(normalizeCustomerId(expected.getCustomerId()), normalizeCustomerId(actual.getCustomerId()));
        Assert.assertEquals(JacksonUtil.toString(expected.getConfiguration()), JacksonUtil.toString(actual.getConfiguration()));
    }

    private void compareDeletedExpectedAndActual(ReportTemplate expected) {
        Optional<ReportTemplateUpdateMsg> msgOpt = edgeImitator.findMessageByType(ReportTemplateUpdateMsg.class);
        Assert.assertTrue(msgOpt.isPresent());
        ReportTemplateUpdateMsg actualMsg = msgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, actualMsg.getMsgType());
        Assert.assertEquals(expected.getUuidId().getMostSignificantBits(), actualMsg.getIdMSB());
        Assert.assertEquals(expected.getUuidId().getLeastSignificantBits(), actualMsg.getIdLSB());
    }

    private UplinkMsg createUplinkMsg(ReportTemplate reportTemplate) throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addReportTemplateUpdateMsg(
                EdgeMsgConstructorUtils.constructReportTemplateUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, reportTemplate));
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        return uplinkMsgBuilder.build();
    }

    private UplinkMsg createUpdateUplinkMsg(ReportTemplate reportTemplate) throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addReportTemplateUpdateMsg(
                EdgeMsgConstructorUtils.constructReportTemplateUpdatedMsg(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, reportTemplate));
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        return uplinkMsgBuilder.build();
    }

    private Customer createCustomerAndChangeEdgeOwner() throws Exception {
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        saveCustomer("Edge Sub Customer", savedCustomer.getId());

        TimeUnit.MILLISECONDS.sleep(500);

        changeEdgeOwnerToCustomer(savedCustomer);
        return savedCustomer;
    }

    private CustomerId normalizeCustomerId(CustomerId customerId) {
        return customerId == null || customerId.isNullUid() ? null : customerId;
    }
}

