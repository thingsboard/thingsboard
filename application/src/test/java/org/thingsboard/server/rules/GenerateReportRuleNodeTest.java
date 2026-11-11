// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.rules;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.rule.engine.report.TbGenerateReportV2Node;
import org.thingsboard.rule.engine.report.TbGenerateReportV2NodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedUserFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
public class GenerateReportRuleNodeTest extends AbstractRuleEngineControllerTest {

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testGenerateReportRuleNode_configFromRuleNode() throws Exception {
        // creating report template and data for the report
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        EntityAlias entityAlias = new EntityAlias(devicesAliasId, "devices", filter); ;

        EntityTableComponent tableComponent = new EntityTableComponent();
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("createdTime", "entityField", "CREATED TIME"),
                        new DataKey("name", "entityField", "NAME"),
                        new DataKey("type", "entityField", "TYPE")
                ))
                .build()));

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(tableComponent));
        configuration.setNamePattern("test.csv");

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Devices report");
        reportTemplate.setFormat(TbReportFormat.CSV);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        List<Device> devices = new ArrayList<>();
        List<String> expectedReportLines = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);
            expectedReportLines.add(device.getCreatedTime() + "," +
                                    device.getName() + "," +
                                    device.getType());
        }

        NotificationTarget recipient = createNotificationTarget(new AffectedUserFilter());
        NotificationTemplate notificationTemplate = saveNotificationTemplate(DefaultNotifications.reportGenerated.toTemplate());

        // creating rule chain with generator and generate report nodes
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Generate report rule chain");
        ruleChain.setTenantId(tenantId);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);
        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode generatorNode = new RuleNode();
        generatorNode.setName("Generator");
        generatorNode.setType(TbMsgGeneratorNode.class.getName());
        generatorNode.setConfigurationVersion(TbMsgGeneratorNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        generatorNode.setDebugSettings(DebugSettings.all());
        TbMsgGeneratorNodeConfiguration generatorNodeConfiguration = new TbMsgGeneratorNodeConfiguration();
        generatorNodeConfiguration.setMsgCount(1);
        generatorNodeConfiguration.setPeriodInSeconds(1);
        generatorNodeConfiguration.setOriginatorId(tenantId.getId().toString());
        generatorNodeConfiguration.setOriginatorType(EntityType.TENANT);
        generatorNodeConfiguration.setScriptLang(ScriptLanguage.TBEL);
        generatorNodeConfiguration.setTbelScript("""
                var msg = { humidity: 77 };
                var metadata = { data: 40 };
                var msgType = "POST_TELEMETRY_REQUEST";
                return { msg: msg, metadata: metadata, msgType: msgType };
                """);
        generatorNode.setConfiguration(JacksonUtil.valueToTree(generatorNodeConfiguration));

        RuleNode generateReportNode = new RuleNode();
        generateReportNode.setName("Generate report");
        generateReportNode.setType(TbGenerateReportV2Node.class.getName());
        generateReportNode.setConfigurationVersion(TbGenerateReportV2Node.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        generateReportNode.setDebugSettings(DebugSettings.all());
        TbGenerateReportV2NodeConfiguration generateReportNodeConfiguration = new TbGenerateReportV2NodeConfiguration();
        ReportConfig reportConfig = new ReportConfig();
        reportConfig.setReportTemplateId(reportTemplate.getId());
        reportConfig.setUserId(tenantAdminUserId);
        reportConfig.setTargets(List.of(recipient.getId().getId()));
        reportConfig.setNotificationTemplateId(notificationTemplate.getId());
        generateReportNodeConfiguration.setConfig(reportConfig);
        generateReportNode.setConfiguration(JacksonUtil.valueToTree(generateReportNodeConfiguration));

        metaData.setNodes(Arrays.asList(generatorNode, generateReportNode));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        RuleChainMetaData savedMetaData = saveRuleChainMetaData(metaData);
        ruleChain = getRuleChain(ruleChain.getId());

        // verifying result
        EventInfo input = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> getDebugEvents(tenantId, savedMetaData.getNodes().get(1).getId(), 10).getData()
                .stream().filter(event -> event.getBody().get("type").asText().equals(DataConstants.IN))
                .findFirst().orElse(null), Objects::nonNull);
        assertThat(input.getBody().get("msgType").asText()).isEqualTo(TbMsgType.POST_TELEMETRY_REQUEST.name());
        assertThat(input.getBody().get("data").asText()).isEqualTo("{\"humidity\":77}");

        EventInfo output = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> getDebugEvents(tenantId, savedMetaData.getNodes().get(1).getId(), 10).getData()
                .stream().filter(event -> event.getBody().get("type").asText().equals(DataConstants.OUT))
                .findFirst().orElse(null), Objects::nonNull);
        assertThat(output.getBody().get("msgType").asText()).isEqualTo(TbMsgType.POST_TELEMETRY_REQUEST.name());
        assertThat(output.getBody().get("data").asText()).isEqualTo("{\"humidity\":77}");
        String reportId = Optional.ofNullable(JacksonUtil.toJsonNode(output.getBody().get("metadata").asText()).get("reports"))
                .map(JsonNode::asText).orElse(null);
        assertThat(reportId).isNotBlank();

        String csvReport = doGet("/api/v2/report/" + reportId + "/download", String.class);
        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).contains("CREATED TIME,NAME,TYPE");
        for (int i = 0; i < devices.size(); i++) {
            assertThat(lines[i + 1]).contains(expectedReportLines.get(i));
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification reportNotification = getMyNotifications(true, 10).stream()
                    .filter(notification -> notification.getType() == NotificationType.REPORT_GENERATED)
                    .findFirst().orElse(null);
            assertThat(reportNotification).isNotNull();
            assertThat(reportNotification.getSubject()).isEqualTo("Report generated");
            assertThat(reportNotification.getText()).isEqualTo("CSV report 'test.csv' is ready");
        });
    }

}
