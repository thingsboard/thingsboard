// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineApiUsageStateService;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.dao.mobile.MobileAppBundleService;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantIdLoaderTest {

    @Mock
    private TbContext ctx;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private RuleEngineAlarmService alarmService;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private EntityViewService entityViewService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private EdgeService edgeService;
    @Mock
    private OtaPackageService otaPackageService;
    @Mock
    private RuleEngineAssetProfileCache assetProfileCache;
    @Mock
    private RuleEngineDeviceProfileCache deviceProfileCache;
    @Mock
    private WidgetTypeService widgetTypeService;
    @Mock
    private WidgetsBundleService widgetsBundleService;
    @Mock
    private QueueService queueService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private RuleEngineRpcService rpcService;
    @Mock
    private RuleEngineApiUsageStateService ruleEngineApiUsageStateService;
    @Mock
    private NotificationTargetService notificationTargetService;
    @Mock
    private NotificationTemplateService notificationTemplateService;
    @Mock
    private NotificationRequestService notificationRequestService;
    @Mock
    private NotificationRuleService notificationRuleService;
    @Mock
    private EntityGroupService entityGroupService;
    @Mock
    private ConverterService converterService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private SchedulerEventService schedulerEventService;
    @Mock
    private BlobEntityService blobEntityService;
    @Mock
    private ReportTemplateService reportTemplateService;
    @Mock
    private ReportService reportService;
    @Mock
    private RoleService roleService;
    @Mock
    private GroupPermissionService groupPermissionService;
    @Mock
    private TbPeContext tbPeContext;
    @Mock
    private QueueStatsService queueStatsService;
    @Mock
    private OAuth2ClientService oAuth2ClientService;
    @Mock
    private DomainService domainService;
    @Mock
    private MobileAppService mobileAppService;
    @Mock
    private MobileAppBundleService mobileAppBundleService;
    @Mock
    private CalculatedFieldService calculatedFieldService;
    @Mock
    private JobService jobService;
    @Mock
    private AiModelService aiModelService;
    @Mock
    private SecretService secretService;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private AgentService agentService;
    @Mock
    private AgentApplicationService agentApplicationService;
    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private AgentAppUnitService agentAppUnitService;
    @Mock
    private AgentAppProfileService agentAppProfileService;
    @Mock
    private AgentProfileService agentProfileService;
    @Mock
    private AgentBulkActionService agentBulkActionService;

    private TenantId tenantId;
    private TenantProfileId tenantProfileId;
    private AbstractListeningExecutor dbExecutor;

    @BeforeEach
    public void before() {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();
        this.tenantId = TenantId.fromUUID(UUID.randomUUID());
        this.tenantProfileId = new TenantProfileId(UUID.randomUUID());

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getPeContext()).thenReturn(tbPeContext);

        for (EntityType entityType : EntityType.values()) {
            initMocks(entityType, tenantId);
        }
    }

    @AfterEach
    public void after() {
        dbExecutor.destroy();
    }

    private void initMocks(EntityType entityType, TenantId tenantId) {
        switch (entityType) {
            case TENANT:
            case NOTIFICATION:
            case ADMIN_SETTINGS:
                break;
            case CUSTOMER:
                Customer customer = new Customer();
                customer.setTenantId(tenantId);
                when(ctx.getCustomerService()).thenReturn(customerService);
                doReturn(customer).when(customerService).findCustomerById(eq(tenantId), any());
                break;
            case USER:
                User user = new User();
                user.setTenantId(tenantId);
                when(ctx.getUserService()).thenReturn(userService);
                doReturn(user).when(userService).findUserById(eq(tenantId), any());
                break;
            case ASSET:
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                when(ctx.getAssetService()).thenReturn(assetService);
                doReturn(asset).when(assetService).findAssetById(eq(tenantId), any());
                break;
            case DEVICE:
                Device device = new Device();
                device.setTenantId(tenantId);
                when(ctx.getDeviceService()).thenReturn(deviceService);
                doReturn(device).when(deviceService).findDeviceById(eq(tenantId), any());
                break;
            case ALARM:
                Alarm alarm = new Alarm();
                alarm.setTenantId(tenantId);
                when(ctx.getAlarmService()).thenReturn(alarmService);
                doReturn(alarm).when(alarmService).findAlarmById(eq(tenantId), any());
                break;
            case RULE_CHAIN:
                RuleChain ruleChain = new RuleChain();
                ruleChain.setTenantId(tenantId);
                when(ctx.getRuleChainService()).thenReturn(ruleChainService);
                doReturn(ruleChain).when(ruleChainService).findRuleChainById(eq(tenantId), any());
                break;
            case ENTITY_VIEW:
                EntityView entityView = new EntityView();
                entityView.setTenantId(tenantId);
                when(ctx.getEntityViewService()).thenReturn(entityViewService);
                doReturn(entityView).when(entityViewService).findEntityViewById(eq(tenantId), any());
                break;
            case DASHBOARD:
                Dashboard dashboard = new Dashboard();
                dashboard.setTenantId(tenantId);
                when(ctx.getDashboardService()).thenReturn(dashboardService);
                doReturn(dashboard).when(dashboardService).findDashboardById(eq(tenantId), any());
                break;
            case EDGE:
                Edge edge = new Edge();
                edge.setTenantId(tenantId);
                when(ctx.getEdgeService()).thenReturn(edgeService);
                doReturn(edge).when(edgeService).findEdgeById(eq(tenantId), any());
                break;
            case OTA_PACKAGE:
                OtaPackage otaPackage = new OtaPackage();
                otaPackage.setTenantId(tenantId);
                when(ctx.getOtaPackageService()).thenReturn(otaPackageService);
                doReturn(otaPackage).when(otaPackageService).findOtaPackageInfoById(eq(tenantId), any());
                break;
            case ASSET_PROFILE:
                AssetProfile assetProfile = new AssetProfile();
                assetProfile.setTenantId(tenantId);
                when(ctx.getAssetProfileCache()).thenReturn(assetProfileCache);
                doReturn(assetProfile).when(assetProfileCache).get(eq(tenantId), any(AssetProfileId.class));
                break;
            case DEVICE_PROFILE:
                DeviceProfile deviceProfile = new DeviceProfile();
                deviceProfile.setTenantId(tenantId);
                when(ctx.getDeviceProfileCache()).thenReturn(deviceProfileCache);
                doReturn(deviceProfile).when(deviceProfileCache).get(eq(tenantId), any(DeviceProfileId.class));
                break;
            case WIDGET_TYPE:
                WidgetType widgetType = new WidgetType();
                widgetType.setTenantId(tenantId);
                when(ctx.getWidgetTypeService()).thenReturn(widgetTypeService);
                doReturn(widgetType).when(widgetTypeService).findWidgetTypeById(eq(tenantId), any());
                break;
            case WIDGETS_BUNDLE:
                WidgetsBundle widgetsBundle = new WidgetsBundle();
                widgetsBundle.setTenantId(tenantId);
                when(ctx.getWidgetBundleService()).thenReturn(widgetsBundleService);
                doReturn(widgetsBundle).when(widgetsBundleService).findWidgetsBundleById(eq(tenantId), any());
                break;
            case RPC:
                Rpc rpc = new Rpc();
                rpc.setTenantId(tenantId);
                when(ctx.getRpcService()).thenReturn(rpcService);
                doReturn(rpc).when(rpcService).findRpcById(eq(tenantId), any());
                break;
            case QUEUE:
                Queue queue = new Queue();
                queue.setTenantId(tenantId);
                when(ctx.getQueueService()).thenReturn(queueService);
                doReturn(queue).when(queueService).findQueueById(eq(tenantId), any());
                break;
            case API_USAGE_STATE:
                ApiUsageState apiUsageState = new ApiUsageState();
                apiUsageState.setTenantId(tenantId);
                when(ctx.getRuleEngineApiUsageStateService()).thenReturn(ruleEngineApiUsageStateService);
                doReturn(apiUsageState).when(ruleEngineApiUsageStateService).findApiUsageStateById(eq(tenantId), any());
                break;
            case TB_RESOURCE:
                TbResource tbResource = new TbResource();
                tbResource.setTenantId(tenantId);
                when(ctx.getResourceService()).thenReturn(resourceService);
                doReturn(tbResource).when(resourceService).findResourceInfoById(eq(tenantId), any());
                break;
            case RULE_NODE:
                RuleNode ruleNode = new RuleNode();
                when(ctx.getRuleChainService()).thenReturn(ruleChainService);
                doReturn(ruleNode).when(ruleChainService).findRuleNodeById(eq(tenantId), any());
                break;
            case TENANT_PROFILE:
                TenantProfile tenantProfile = new TenantProfile(tenantProfileId);
                when(ctx.getTenantProfile()).thenReturn(tenantProfile);
                break;
            case NOTIFICATION_TARGET:
                NotificationTarget notificationTarget = new NotificationTarget();
                notificationTarget.setTenantId(tenantId);
                when(ctx.getNotificationTargetService()).thenReturn(notificationTargetService);
                doReturn(notificationTarget).when(notificationTargetService).findNotificationTargetById(eq(tenantId), any());
                break;
            case NOTIFICATION_TEMPLATE:
                NotificationTemplate notificationTemplate = new NotificationTemplate();
                notificationTemplate.setTenantId(tenantId);
                when(ctx.getNotificationTemplateService()).thenReturn(notificationTemplateService);
                doReturn(notificationTemplate).when(notificationTemplateService).findNotificationTemplateById(eq(tenantId), any());
                break;
            case NOTIFICATION_REQUEST:
                NotificationRequest notificationRequest = new NotificationRequest();
                notificationRequest.setTenantId(tenantId);
                when(ctx.getNotificationRequestService()).thenReturn(notificationRequestService);
                doReturn(notificationRequest).when(notificationRequestService).findNotificationRequestById(eq(tenantId), any());
                break;
            case NOTIFICATION_RULE:
                NotificationRule notificationRule = new NotificationRule();
                notificationRule.setTenantId(tenantId);
                when(ctx.getNotificationRuleService()).thenReturn(notificationRuleService);
                doReturn(notificationRule).when(notificationRuleService).findNotificationRuleById(eq(tenantId), any());
                break;
            //PE Entities
            case ENTITY_GROUP:
                EntityGroup entityGroup = new EntityGroup();
                entityGroup.setOwnerId(tenantId);
                entityGroup.setTenantId(tenantId);
                when(tbPeContext.getEntityGroupService()).thenReturn(entityGroupService);
                doReturn(entityGroup).when(entityGroupService).findEntityGroupById(eq(tenantId), any());
                break;
            case CONVERTER:
                Converter converter = new Converter();
                converter.setTenantId(tenantId);
                when(tbPeContext.getConverterService()).thenReturn(converterService);
                doReturn(converter).when(converterService).findConverterById(eq(tenantId), any());
                break;
            case INTEGRATION:
                Integration integration = new Integration();
                integration.setTenantId(tenantId);
                when(tbPeContext.getIntegrationService()).thenReturn(integrationService);
                doReturn(integration).when(integrationService).findIntegrationById(eq(tenantId), any());
                break;
            case SCHEDULER_EVENT:
                SchedulerEvent schedulerEvent = new SchedulerEvent();
                schedulerEvent.setTenantId(tenantId);
                when(tbPeContext.getSchedulerEventService()).thenReturn(schedulerEventService);
                doReturn(schedulerEvent).when(schedulerEventService).findSchedulerEventById(eq(tenantId), any());
                break;
            case BLOB_ENTITY:
                BlobEntity blobEntity = new BlobEntity();
                blobEntity.setTenantId(tenantId);
                when(tbPeContext.getBlobEntityService()).thenReturn(blobEntityService);
                doReturn(blobEntity).when(blobEntityService).findBlobEntityById(eq(tenantId), any());
                break;
            case REPORT_TEMPLATE:
                ReportTemplate reportTemplate = new ReportTemplate();
                reportTemplate.setTenantId(tenantId);
                when(tbPeContext.getReportTemplateService()).thenReturn(reportTemplateService);
                doReturn(reportTemplate).when(reportTemplateService).findReportTemplateById(eq(tenantId), any());
                break;
            case REPORT:
                Report report = new Report();
                report.setTenantId(tenantId);
                when(tbPeContext.getReportService()).thenReturn(reportService);
                doReturn(report).when(reportService).findReportById(eq(tenantId), any());
                break;
            case ROLE:
                Role role = new Role();
                role.setTenantId(tenantId);
                when(tbPeContext.getRoleService()).thenReturn(roleService);
                doReturn(role).when(roleService).findRoleById(eq(tenantId), any());
                break;
            case GROUP_PERMISSION:
                GroupPermission groupPermission = new GroupPermission();
                groupPermission.setTenantId(tenantId);
                when(tbPeContext.getGroupPermissionService()).thenReturn(groupPermissionService);
                doReturn(groupPermission).when(groupPermissionService).findGroupPermissionById(eq(tenantId), any());
                break;
            case SECRET:
                Secret secret = new Secret();
                secret.setTenantId(tenantId);
                when(ctx.getPeContext().getSecretService()).thenReturn(secretService);
                doReturn(secret).when(secretService).findSecretInfoById(eq(tenantId), any());
                break;
            // ... PE entities
            case QUEUE_STATS:
                QueueStats queueStats = new QueueStats();
                queueStats.setTenantId(tenantId);
                when(ctx.getQueueStatsService()).thenReturn(queueStatsService);
                doReturn(queueStats).when(queueStatsService).findQueueStatsById(eq(tenantId), any());
                break;
            case OAUTH2_CLIENT:
                OAuth2Client oAuth2Client = new OAuth2Client();
                oAuth2Client.setTenantId(tenantId);
                when(ctx.getOAuth2ClientService()).thenReturn(oAuth2ClientService);
                doReturn(oAuth2Client).when(oAuth2ClientService).findOAuth2ClientById(eq(tenantId), any());
                break;
            case DOMAIN:
                Domain domain = new Domain();
                domain.setTenantId(tenantId);
                when(ctx.getDomainService()).thenReturn(domainService);
                doReturn(domain).when(domainService).findDomainById(eq(tenantId), any());
                break;
            case MOBILE_APP:
                MobileApp mobileApp = new MobileApp();
                mobileApp.setTenantId(tenantId);
                when(ctx.getMobileAppService()).thenReturn(mobileAppService);
                doReturn(mobileApp).when(mobileAppService).findMobileAppById(eq(tenantId), any());
                break;
            case MOBILE_APP_BUNDLE:
                MobileAppBundle mobileAppBundle = new MobileAppBundle();
                mobileAppBundle.setTenantId(tenantId);
                when(ctx.getMobileAppBundleService()).thenReturn(mobileAppBundleService);
                doReturn(mobileAppBundle).when(mobileAppBundleService).findMobileAppBundleById(eq(tenantId), any());
                break;
            case CALCULATED_FIELD:
                CalculatedField calculatedField = new CalculatedField();
                calculatedField.setTenantId(tenantId);
                when(ctx.getCalculatedFieldService()).thenReturn(calculatedFieldService);
                doReturn(calculatedField).when(calculatedFieldService).findById(eq(tenantId), any());
                break;
            case JOB:
                Job job = new Job();
                job.setTenantId(tenantId);
                when(ctx.getJobService()).thenReturn(jobService);
                doReturn(job).when(jobService).findJobById(eq(tenantId), any());
                break;
            case AI_MODEL:
                AiModel aiModel = new AiModel();
                aiModel.setTenantId(tenantId);
                when(ctx.getAiModelService()).thenReturn(aiModelService);
                doReturn(Optional.of(aiModel)).when(aiModelService).findAiModelById(eq(tenantId), any());
                break;
            case API_KEY:
                ApiKey apiKey = new ApiKey();
                apiKey.setTenantId(tenantId);
                when(ctx.getApiKeyService()).thenReturn(apiKeyService);
                doReturn(apiKey).when(apiKeyService).findApiKeyById(eq(tenantId), any());
                break;
            case AGENT:
                Agent agent = new Agent();
                agent.setTenantId(tenantId);
                when(tbPeContext.getAgentService()).thenReturn(agentService);
                doReturn(agent).when(agentService).findAgentById(eq(tenantId), any());
                break;
            case AGENT_APPLICATION:
                AgentApplication agentApplication = new AgentApplication();
                agentApplication.setTenantId(tenantId);
                when(tbPeContext.getAgentApplicationService()).thenReturn(agentApplicationService);
                doReturn(agentApplication).when(agentApplicationService).findById(eq(tenantId), any());
                break;
            case AGENT_APP_EVENT:
                AgentAppEvent agentAppEvent = new AgentAppEvent();
                agentAppEvent.setTenantId(tenantId);
                when(tbPeContext.getAgentAppEventService()).thenReturn(agentAppEventService);
                doReturn(agentAppEvent).when(agentAppEventService).findById(eq(tenantId), any());
                break;
            case AGENT_APP_UNIT:
                AgentAppUnit agentAppUnit = new AgentAppUnit();
                agentAppUnit.setTenantId(tenantId);
                when(tbPeContext.getAgentAppUnitService()).thenReturn(agentAppUnitService);
                doReturn(agentAppUnit).when(agentAppUnitService).findAgentAppUnitById(eq(tenantId), any());
                break;
            case AGENT_APP_PROFILE:
                AgentAppProfile agentAppProfile = new AgentAppProfile();
                agentAppProfile.setTenantId(tenantId);
                when(tbPeContext.getAgentAppProfileService()).thenReturn(agentAppProfileService);
                doReturn(agentAppProfile).when(agentAppProfileService).findProfileById(eq(tenantId), any());
                break;
            case AGENT_PROFILE:
                AgentProfile agentProfile = new AgentProfile();
                agentProfile.setTenantId(tenantId);
                when(tbPeContext.getAgentProfileService()).thenReturn(agentProfileService);
                doReturn(agentProfile).when(agentProfileService).findProfileById(eq(tenantId), any());
                break;
            case AGENT_BULK_ACTION:
                AgentBulkAction agentBulkAction = new AgentBulkAction();
                agentBulkAction.setTenantId(tenantId);
                when(tbPeContext.getAgentBulkActionService()).thenReturn(agentBulkActionService);
                doReturn(agentBulkAction).when(agentBulkActionService).findById(eq(tenantId), any());
                break;
            default:
                throw new RuntimeException("Unexpected originator EntityType " + entityType);
        }
    }

    private EntityId getEntityId(EntityType entityType) {
        return EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID());
    }

    private void checkTenant(TenantId checkTenantId, boolean equals) {
        for (EntityType entityType : EntityType.values()) {
            EntityId entityId;
            if (EntityType.TENANT.equals(entityType)) {
                entityId = tenantId;
            } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                entityId = tenantProfileId;
            } else {
                entityId = getEntityId(entityType);
            }
            TenantId targetTenantId = TenantIdLoader.findTenantId(ctx, entityId);
            String msg = "Check entity type <" + entityType.name() + ">:";
            if (equals) {
                Assertions.assertEquals(targetTenantId, checkTenantId, msg);
            } else {
                Assertions.assertNotEquals(targetTenantId, checkTenantId, msg);
            }
        }
    }

    @Test
    public void test_findEntityIdAsync_current_tenant() {
        checkTenant(tenantId, true);
    }

    @Test
    public void test_findEntityIdAsync_other_tenant() {
        checkTenant(TenantId.fromUUID(UUID.randomUUID()), false);
    }

}
