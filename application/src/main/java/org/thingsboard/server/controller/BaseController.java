// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.mail.MessagingException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.thingsboard.ai.common.data.usage.ApiUsageInfo;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.TbBiFunction;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.dao.mobile.MobileAppBundleService;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageStateService;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.EntitiesLimitExceededException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.edge.EdgeLicenseService;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.agent.bulk.AgentBulkActionProcessingService;
import org.thingsboard.server.service.entitiy.ai.TbAiModelService;
import org.thingsboard.server.service.entitiy.ota.group.TbDeviceGroupOtaPackageService;
import org.thingsboard.server.service.entitiy.user.TbUserSettingsService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static org.thingsboard.ai.common.data.usage.Resource.AI_CREDITS;
import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;
import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.UserController.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION;
import static org.thingsboard.server.dao.service.Validator.validateId;

@TbCoreComponent
public abstract class BaseController {

    protected static final String HOME_DASHBOARD_ID = "homeDashboardId";
    protected static final String HOME_DASHBOARD_HIDE_TOOLBAR = "homeDashboardHideToolbar";

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

    /*Swagger UI description*/

    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    protected AccessControlService accessControlService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected TenantProfileService tenantProfileService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected WhiteLabelingService whiteLabelingService;

    @Autowired
    protected TbUserSettingsService userSettingsService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected AssetProfileService assetProfileService;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected ConverterService converterService;

    @Autowired
    protected AlarmSubscriptionService alarmService;

    @Autowired
    protected AlarmCommentService alarmCommentService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected OAuth2ClientService oAuth2ClientService;

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected MobileAppService mobileAppService;

    @Autowired
    protected MobileAppBundleService mobileAppBundleService;

    @Autowired
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    protected TbAiService aiService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected OwnersCacheService ownersCacheService;

    @Autowired
    protected SchedulerEventService schedulerEventService;

    @Autowired
    protected BlobEntityService blobEntityService;

    @Autowired
    protected ReportTemplateService reportTemplateService;

    @Autowired
    protected ReportService reportService;

    @Autowired
    protected AuditLogService auditLogService;

    @Autowired
    protected DeviceStateService deviceStateService;

    @Autowired
    protected SchedulerService schedulerService;

    @Autowired
    protected SubscriptionService subscriptionService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    protected GroupPermissionService groupPermissionService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected UserPermissionsService userPermissionsService;

    @Autowired
    protected ClaimDevicesService claimDevicesService;

    @Autowired
    protected PartitionService partitionService;

    @Autowired
    protected TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    protected ResourceService resourceService;

    @Autowired
    protected OtaPackageService otaPackageService;

    @Autowired
    protected OtaPackageStateService otaPackageStateService;

    @Autowired
    protected DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Autowired
    protected TbDeviceGroupOtaPackageService tbDeviceGroupOtaPackageService;

    @Autowired
    protected RpcService rpcService;

    @Autowired
    protected TbQueueProducerProvider producerProvider;

    @Autowired
    protected EntityQueryService entityQueryService;

    @Autowired
    protected EntityService entityService;

    @Autowired
    protected TbTenantProfileCache tenantProfileCache;

    @Autowired
    protected TbDeviceProfileCache deviceProfileCache;

    @Autowired
    protected TbAssetProfileCache assetProfileCache;

    @Autowired(required = false)
    protected EdgeService edgeService;

    @Autowired(required = false)
    protected EdgeLicenseService edgeLicenseService;

    @Autowired
    protected TbLogEntityActionService logEntityActionService;

    @Autowired
    protected EntityActionService entityActionService;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected EntitiesVersionControlService vcService;

    @Autowired
    protected ExportableEntitiesService entitiesService;

    @Autowired
    protected CustomMenuService customMenuService;

    @Autowired
    protected NotificationTargetService notificationTargetService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected CalculatedFieldService calculatedFieldService;

    @Autowired
    protected SecretService secretService;

    @Autowired
    protected AiModelService aiModelService;

    @Autowired
    protected TbAiModelService tbAiModelService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected AgentService agentService;

    @Autowired
    protected AgentApplicationService agentAppService;

    @Autowired
    protected AgentAppProfileService agentAppProfileService;

    @Autowired
    protected AgentProfileService agentProfileService;

    @Autowired
    protected AgentBulkActionProcessingService agentBulkActionProcessingService;

    @Autowired
    protected AgentAppUnitService agentAppUnitService;

    @Autowired
    protected AgentAppEventService agentAppEventService;

    @Autowired
    protected AgentBulkActionService agentBulkActionService;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    @Value("${edges.enabled}")
    @Getter
    protected boolean edgesEnabled;

    @ExceptionHandler(Exception.class)
    public void handleControllerException(Exception e, HttpServletResponse response) {
        ThingsboardException thingsboardException = handleException(e);
        if (thingsboardException.getErrorCode() == ThingsboardErrorCode.GENERAL && thingsboardException.getCause() instanceof Exception
            && StringUtils.equals(thingsboardException.getCause().getMessage(), thingsboardException.getMessage())) {
            e = (Exception) thingsboardException.getCause();
        } else {
            e = thingsboardException;
        }
        errorResponseHandler.handle(e, response);
    }

    @ExceptionHandler(ThingsboardException.class)
    public void handleThingsboardException(ThingsboardException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    /**
     * @deprecated Exceptions that are not of {@link ThingsboardException} type
     * are now caught and mapped to {@link ThingsboardException} by
     * {@link ExceptionHandler} {@link BaseController#handleControllerException(Exception, HttpServletResponse)}
     * which basically acts like the following boilerplate:
     * {@code
     *  try {
     *      someExceptionThrowingMethod();
     *  } catch (Exception e) {
     *      throw handleException(e);
     *  }
     * }
     * */
    @Deprecated
    ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    private ThingsboardException handleException(Throwable exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            try {
                SecurityUser user = getCurrentUser();
                if (exception instanceof SubscriptionException) {
                    log.error("[{}][{}] Error [{}]", user.getTenantId(), user.getId(), exception.getMessage());
                } else {
                    log.error("[{}][{}] Error", user.getTenantId(), user.getId(), exception);
                }
            } catch (Exception e) {
                log.error("Error", exception);
            }
        }

        Throwable cause = exception.getCause();
        if (exception instanceof ExecutionException) {
            exception = cause;
        }
        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof EntitiesLimitExceededException) {
            return new ThingsboardException(exception, ThingsboardErrorCode.ENTITIES_LIMIT_EXCEEDED);
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                   || exception instanceof DataValidationException || cause instanceof IncorrectParameterException) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail", ThingsboardErrorCode.GENERAL);
        } else if (exception instanceof SubscriptionException) {
            return new ThingsboardException(exception, ThingsboardErrorCode.SUBSCRIPTION_VIOLATION);
        } else if (exception instanceof AsyncRequestTimeoutException) {
            return new ThingsboardException("Request timeout", ThingsboardErrorCode.GENERAL);
        } else if (exception instanceof DataAccessException) {
            return new ThingsboardException(exception, ThingsboardErrorCode.DATABASE);
        } else if (exception instanceof EntityVersionMismatchException) {
            return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.VERSION_CONFLICT);
        } else if (exception instanceof MethodArgumentTypeMismatchException) {
            return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.GENERAL);
    }

    /**
     * Handles validation error for controller method arguments annotated with @{@link jakarta.validation.Valid}
     * */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationError(MethodArgumentNotValidException validationError, HttpServletResponse response) {
        List<ConstraintViolation<Object>> constraintsViolations = validationError.getFieldErrors().stream()
                .map(fieldError -> {
                    try {
                        return (ConstraintViolation<Object>) fieldError.unwrap(ConstraintViolation.class);
                    } catch (Exception e) {
                        log.warn("FieldError source is not of type ConstraintViolation");
                        return null; // should not happen
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        String errorMessage = "Validation error: " + ConstraintValidator.getErrorMessage(constraintsViolations);
        ThingsboardException thingsboardException = new ThingsboardException(errorMessage, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        handleControllerException(thingsboardException, response);
    }

    <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    static void checkParameter(String name, String param) throws ThingsboardException {
        if (StringUtils.isBlank(param)) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    void checkArrayParameter(String name, String[] params) throws ThingsboardException {
        if (params == null || params.length == 0) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    protected <T> T checkEnumParameter(String name, String param, Function<String, T> valueOf) throws ThingsboardException {
        try {
            return valueOf.apply(param.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException(name + " \"" + param + "\" is not supported!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    RoleType checkStrRoleType(String name, String strGroupType) throws ThingsboardException {
        checkParameter(name, strGroupType);
        RoleType groupType;
        try {
            groupType = RoleType.valueOf(strGroupType);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported role type '" + strGroupType + "'! Only 'GENERIC' or 'GROUP' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    EntityType checkStrEntityGroupType(String name, String strGroupType) throws ThingsboardException {
        checkParameter(name, strGroupType);
        EntityType groupType;
        try {
            groupType = EntityType.valueOf(strGroupType);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported entityGroup type '" + strGroupType + "'! Only 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE' or 'AGENT' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return checkEntityGroupType(groupType);
    }

    void checkEntityGroupType(EntityType expected, EntityType actual) throws ThingsboardException {
        if (expected == null) {
            throw new RuntimeException("Expected Entitytype is not specified!");
        }
        if (actual == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!expected.equals(actual)) {
            throw new ThingsboardException("Expected entity group with type '" + expected + "' but received '" + actual + "'!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    EntityType checkEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (groupType != EntityType.CUSTOMER && groupType != EntityType.ASSET
            && groupType != EntityType.DEVICE && groupType != EntityType.USER
            && groupType != EntityType.ENTITY_VIEW && groupType != EntityType.EDGE
            && groupType != EntityType.DASHBOARD
            && groupType != EntityType.AGENT) {
            throw new ThingsboardException("Unsupported entityGroup type '" + groupType + "'! Only 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE' or 'AGENT' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    EntityType checkSharableEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!Arrays.stream(EntityGroup.sharableGroupTypes).anyMatch(type -> type.equals(groupType))) {
            throw new ThingsboardException("Invalid entityGroup type '" + groupType + "'! Only entity groups of types 'CUSTOMER', 'ASSET', 'DEVICE', 'ENTITY_VIEW' or 'DASHBOARD' can be shared.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    UUID toUUID(String id) throws ThingsboardException {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw handleException(e, false);
        }
    }

    PageLink createPageLink(int pageSize, int page, String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        if (StringUtils.isNotEmpty(sortProperty)) {
            if (!Validator.isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            SortOrder.Direction direction = SortOrder.Direction.ASC;
            if (StringUtils.isNotEmpty(sortOrder)) {
                try {
                    direction = SortOrder.Direction.valueOf(sortOrder.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ThingsboardException("Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            SortOrder sort = new SortOrder(sortProperty, direction);
            return new PageLink(pageSize, page, textSearch, sort);
        } else {
            return new PageLink(pageSize, page, textSearch);
        }
    }

    TimePageLink createTimePageLink(int pageSize, int page, String textSearch,
                                    String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        PageLink pageLink = this.createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return new TimePageLink(pageLink, startTime, endTime);
    }

    protected SecurityUser getCurrentUser() throws ThingsboardException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

    /**
     * Fills in the AI credits this deployment has spent, which only the AI service can answer. The grant they
     * are spent against comes off the licence's own plan data, so it is not read here.
     */
    protected void enrichSubscriptionInfo(SubscriptionInfo subscriptionInfo) throws ThingsboardException {
        ApiUsageInfo aiApiUsage = aiService.getApiUsageInfo(getCurrentUser());
        ApiUsageInfo.ResourceUsage aiCreditsUsage = aiApiUsage.usage().get(AI_CREDITS);
        subscriptionInfo.setUsedAiCredits(aiCreditsUsage != null ? aiCreditsUsage.used() : 0);
    }

    Tenant checkTenantId(TenantId tenantId, Operation operation) throws ThingsboardException {
        return checkEntityId(tenantId, (t, i) -> tenantService.findTenantById(tenantId), operation);
    }

    TenantInfo checkTenantInfoId(TenantId tenantId, Operation operation) throws ThingsboardException {
        return checkEntityId(tenantId, (t, i) -> tenantService.findTenantInfoById(tenantId), operation);
    }

    TenantProfile checkTenantProfileId(TenantProfileId tenantProfileId, Operation operation) throws ThingsboardException {
        try {
            validateId(tenantProfileId, id -> "Incorrect tenantProfileId " + id);
            TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(getTenantId(), tenantProfileId);
            checkNotNull(tenantProfile, "Tenant profile with id [" + tenantProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, operation);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected TenantId getTenantId() throws ThingsboardException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerId(CustomerId customerId, Operation operation) throws ThingsboardException {
        return checkEntityId(customerId, customerService::findCustomerById, operation);
    }

    CustomerInfo checkCustomerInfoId(CustomerId customerId, Operation operation) throws ThingsboardException {
        return checkEntityId(customerId, customerService::findCustomerInfoById, operation);
    }

    CustomMenu checkCustomMenuId(CustomMenuId customMenuId, Operation operation) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        CustomMenu customMenu = customMenuService.findCustomMenuById(currentUser.getTenantId(), customMenuId);
        checkNotNull(customMenu, "Custom menu not found");
        accessControlService.checkCustomMenuPermission(currentUser, operation, customMenu);
        return customMenu;
    }

    CustomMenuInfo checkCustomMenuInfoId(CustomMenuId customMenuId, Operation operation) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        CustomMenuInfo customMenuInfo = customMenuService.findCustomMenuInfoById(currentUser.getTenantId(), customMenuId);
        checkNotNull(customMenuInfo, "Custom menu not found");
        accessControlService.checkCustomMenuPermission(currentUser, operation, customMenuInfo);
        return customMenuInfo;
    }

    User checkUserId(UserId userId, Operation operation) throws ThingsboardException {
        try {
            validateId(userId, id -> "Incorrect userId " + id);
            User user = userService.findUserById(getCurrentUser().getTenantId(), userId);
            checkNotNull(user, "User with id [" + userId + "] is not found");
            if (operation != Operation.READ || !getCurrentUser().getId().equals(userId)) {
                accessControlService.checkPermission(getCurrentUser(), Resource.USER, operation, userId, user);
            }
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    UserInfo checkUserInfoId(UserId userId, Operation operation) throws ThingsboardException {
        try {
            validateId(userId, id -> "Incorrect userId " + id);
            UserInfo user = userService.findUserInfoById(getCurrentUser().getTenantId(), userId);
            checkNotNull(user, "User with id [" + userId + "] is not found");
            if (operation != Operation.READ || !getCurrentUser().getId().equals(userId)) {
                accessControlService.checkPermission(getCurrentUser(), Resource.USER, operation, userId, user);
            }
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected UserId checkAssigneeId(String assigneeId) throws ThingsboardException {
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
            checkUserId(assigneeUserId, Operation.READ);
        }
        return assigneeUserId;
    }

    protected <I extends EntityId, T extends GroupEntity<I>> T saveGroupEntity(T entity, String strEntityGroupId,
                                                                               TbBiFunction<T, EntityGroup, T> saveEntityFunction) throws ThingsboardException {
        return saveGroupEntity(entity, strEntityGroupId, null,
                (t, entityGroups) -> saveEntityFunction.apply(t, entityGroups != null && !entityGroups.isEmpty() ? entityGroups.get(0) : null));
    }

    protected <I extends EntityId, T extends GroupEntity<I>> T saveGroupEntity(T entity, String strEntityGroupId, String[] strEntityGroupIds,
                                                                               TbBiFunction<T, List<EntityGroup>, T> saveEntityFunction) throws ThingsboardException {
        try {
            entity.setTenantId(getCurrentUser().getTenantId());

            List<EntityGroupId> entityGroupIds = new ArrayList<>();
            List<EntityGroup> entityGroups = new ArrayList<>();
            String[] groupIds = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                groupIds = new String[]{strEntityGroupId};
            } else if (strEntityGroupIds != null && strEntityGroupIds.length > 0) {
                groupIds = strEntityGroupIds;
            }
            if (groupIds != null) {
                for (String id : groupIds) {
                    EntityGroupId entityGroupId = new EntityGroupId(toUUID(id));
                    EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
                    entityGroupIds.add(entityGroupId);
                    entityGroups.add(entityGroup);
                }
            }
            if (entity.getId() == null && (entity.getCustomerId() == null || entity.getCustomerId().isNullUid())) {
                if (!entityGroups.isEmpty() && entityGroups.get(0).getOwnerId().getEntityType() == EntityType.CUSTOMER) {
                    entity.setOwnerId(new CustomerId(entityGroups.get(0).getOwnerId().getId()));
                } else if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                    entity.setOwnerId(getCurrentUser().getCustomerId());
                }
            }

            checkEntityWithGroupIds(entity.getId(), entity, Resource.resourceFromEntityType(entity.getEntityType()), entityGroupIds);

            if (entity.getId() != null) {
                EntityId oldOwner = ownersCacheService.getOwner(entity.getTenantId(), entity.getId());
                if (!entity.getOwnerId().equals(oldOwner)) {
                    throw new DataValidationException("Entity owner can`t be changed. Please use owner api to change owner");
                }
            }

            return saveEntityFunction.apply(entity, entityGroups);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(entity.getEntityType()), entity,
                    entity.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser(), e);
            throw handleException(e);
        }
    }

    /** Refuses the request unless the current license entitles this instance to the given feature. */
    protected void checkFeatureAllowed(PlatformFeature feature) throws ThingsboardException {
        subscriptionService.checkFeatureAllowed(getCurrentUser().getTenantId(), feature);
    }

    protected void checkUserOwnerPermission(SecurityUser currentUser, UserId userId) throws ThingsboardException {
        if (!ownersCacheService.getOwners(currentUser.getTenantId(), userId, null).contains(currentUser.getOwnerId())) {
            throw new ThingsboardException("You aren't authorized to perform this operation for the chosen user!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    protected <I extends EntityId, T extends TenantEntity> void checkEntityWithGroupIds(I entityId, T entity, Resource resource, List<EntityGroupId> entityGroupIds) throws ThingsboardException {
        if (entityGroupIds != null && !entityGroupIds.isEmpty()) {
            for (EntityGroupId entityGroupId : entityGroupIds) {
                checkEntity(entityId, entity, resource, entityGroupId);
            }
        } else {
            checkEntity(entityId, entity, resource);
        }
    }

    protected <I extends EntityId, T extends TenantEntity> void checkEntity(I entityId, T entity, Resource resource, EntityGroupId entityGroupId) throws ThingsboardException {
        if (entityId == null) {
            if (entityGroupId == null) {
                accessControlService
                        .checkPermission(getCurrentUser(), resource, Operation.CREATE, null, entity);
            } else {
                accessControlService
                        .checkPermission(getCurrentUser(), resource, Operation.CREATE, null, entity, entityGroupId);
            }
        } else {
            checkEntityId(entityId, Operation.WRITE);
        }
    }

    protected <I extends EntityId, T extends TenantEntity> void checkEntity(I entityId, T entity, Resource resource) throws ThingsboardException {
        checkEntity(entityId, entity, resource, null);
    }

    protected HasId<? extends EntityId> checkEntityId(EntityId entityId, Operation operation) throws ThingsboardException {
        try {
            if (entityId == null) {
                throw new ThingsboardException("Parameter entityId can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            validateId(entityId.getId(), id -> "Incorrect entityId " + id);
            return switch (entityId.getEntityType()) {
                case ALARM -> checkAlarmId(new AlarmId(entityId.getId()), operation);
                case DEVICE -> checkDeviceId(new DeviceId(entityId.getId()), operation);
                case DEVICE_PROFILE -> checkDeviceProfileId(new DeviceProfileId(entityId.getId()), operation);
                case CUSTOMER -> checkCustomerId(new CustomerId(entityId.getId()), operation);
                case TENANT -> checkTenantId(TenantId.fromUUID(entityId.getId()), operation);
                case TENANT_PROFILE -> checkTenantProfileId(new TenantProfileId(entityId.getId()), operation);
                case RULE_CHAIN -> checkRuleChain(new RuleChainId(entityId.getId()), operation);
                case RULE_NODE -> checkRuleNode(new RuleNodeId(entityId.getId()), operation);
                case ASSET -> checkAssetId(new AssetId(entityId.getId()), operation);
                case ASSET_PROFILE -> checkAssetProfileId(new AssetProfileId(entityId.getId()), operation);
                case INTEGRATION -> checkIntegrationId(new IntegrationId(entityId.getId()), operation);
                case CONVERTER -> checkConverterId(new ConverterId(entityId.getId()), operation);
                case DASHBOARD -> checkDashboardId(new DashboardId(entityId.getId()), operation);
                case USER -> checkUserId(new UserId(entityId.getId()), operation);
                case ENTITY_GROUP -> checkEntityGroupId(new EntityGroupId(entityId.getId()), operation);
                case SCHEDULER_EVENT -> checkSchedulerEventInfoId(new SchedulerEventId(entityId.getId()), operation);
                case BLOB_ENTITY -> checkBlobEntityInfoId(new BlobEntityId(entityId.getId()), operation);
                case ENTITY_VIEW -> checkEntityViewId(new EntityViewId(entityId.getId()), operation);
                case EDGE -> checkEdgeId(new EdgeId(entityId.getId()), operation);
                case ROLE -> checkRoleId(new RoleId(entityId.getId()), operation);
                case WIDGETS_BUNDLE -> checkWidgetsBundleId(new WidgetsBundleId(entityId.getId()), operation);
                case WIDGET_TYPE -> checkWidgetTypeId(new WidgetTypeId(entityId.getId()), operation);
                case GROUP_PERMISSION -> checkGroupPermissionId(new GroupPermissionId(entityId.getId()), operation);
                case TB_RESOURCE -> checkResourceInfoId(new TbResourceId(entityId.getId()), operation);
                case OTA_PACKAGE -> checkOtaPackageId(new OtaPackageId(entityId.getId()), operation);
                case QUEUE -> checkQueueId(new QueueId(entityId.getId()), operation);
                case OAUTH2_CLIENT -> checkOauth2ClientId(new OAuth2ClientId(entityId.getId()), operation);
                case DOMAIN -> checkDomainId(new DomainId(entityId.getId()), operation);
                case MOBILE_APP -> checkMobileAppId(new MobileAppId(entityId.getId()), operation);
                case MOBILE_APP_BUNDLE -> checkMobileAppBundleId(new MobileAppBundleId(entityId.getId()), operation);
                case CALCULATED_FIELD -> checkCalculatedFieldId(new CalculatedFieldId(entityId.getId()), operation);
                case SECRET -> checkSecretId(new SecretId(entityId.getId()), operation);
                case REPORT_TEMPLATE -> checkReportTemplateInfoId(new ReportTemplateId(entityId.getId()), operation);
                case AI_MODEL -> checkAiModelId(new AiModelId(entityId.getId()), operation);
                case API_KEY -> checkApiKeyId(new ApiKeyId(entityId.getId()), operation);
                case AGENT -> checkAgentId(new AgentId(entityId.getId()), operation);
                case AGENT_APPLICATION -> checkAgentAppId(new AgentApplicationId(entityId.getId()), operation);
                case AGENT_APP_UNIT -> checkAgentAppUnitId(new AgentAppUnitId(entityId.getId()), operation);
                case AGENT_APP_EVENT -> checkAgentAppEventId(new AgentAppEventId(entityId.getId()), operation);
                case AGENT_BULK_ACTION -> checkAgentBulkActionId(new AgentBulkActionId(entityId.getId()), operation);
                case AGENT_APP_PROFILE -> checkAgentAppProfileId(new AgentAppProfileId(entityId.getId()), operation);
                case AGENT_PROFILE -> checkAgentProfileId(new AgentProfileId(entityId.getId()), operation);
                default -> (HasId<? extends EntityId>) checkEntityId(entityId, entitiesService::findEntityByTenantIdAndId, operation);
            };
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <E extends HasId<I> & TenantEntity, I extends EntityId> E checkEntityId(I entityId, ThrowingBiFunction<TenantId, I, E> findingFunction, Operation operation) throws ThingsboardException {
        try {
            validateId((UUIDBased) entityId, id -> "Invalid entity id");
            SecurityUser user = getCurrentUser();
            E entity = findingFunction.apply(user.getTenantId(), entityId);
            checkNotNull(entity, entityId.getEntityType().getNormalName() + " with id [" + entityId + "] is not found");
            return checkEntity(user, entity, operation);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <E extends HasId<I> & TenantEntity, I extends EntityId> E checkEntityId(I entityId, ThrowingBiFunction<TenantId, I, E> findingFunction, Operation first, Operation... rest) throws ThingsboardException {
        E entity = checkEntityId(entityId, findingFunction, first);
        if (rest.length > 0) {
            SecurityUser user = getCurrentUser();
            for (Operation operation : rest) {
                checkEntity(user, entity, operation);
            }
        }
        return entity;
    }

    protected <E extends HasId<I> & TenantEntity, I extends EntityId> E checkEntity(SecurityUser user, E entity, Operation operation) throws ThingsboardException {
        checkNotNull(entity, "Entity not found");
        accessControlService.checkPermission(user, Resource.resourceFromEntityType(entity.getId().getEntityType()), operation, entity.getId(), entity);
        return entity;
    }

    protected void checkReferencedEntities(CalculatedFieldConfiguration calculatedFieldConfig) throws ThingsboardException {
        for (EntityId referencedEntityId : calculatedFieldConfig.getReferencedEntities()) {
            EntityType refEntityType = referencedEntityId.getEntityType();
            switch (refEntityType) {
                case TENANT -> {}
                case CUSTOMER, ASSET, DEVICE -> checkEntityId(referencedEntityId, Operation.READ);
                default -> throw new IllegalArgumentException("Unsupported referenced entity type: '" + refEntityType + "'.");
            }
        }
    }

    Device checkDeviceId(DeviceId deviceId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceId, deviceService::findDeviceById, operation);
    }

    Device checkDeviceId(DeviceId deviceId, Operation first, Operation... rest) throws ThingsboardException {
        return checkEntityId(deviceId, deviceService::findDeviceById, first, rest);
    }

    DeviceInfo checkDeviceInfoId(DeviceId deviceId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceId, deviceService::findDeviceInfoById, operation);
    }

    DeviceProfile checkDeviceProfileId(DeviceProfileId deviceProfileId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceProfileId, deviceProfileService::findDeviceProfileById, operation);
    }

    protected EntityView checkEntityViewId(EntityViewId entityViewId, Operation operation) throws ThingsboardException {
        return checkEntityId(entityViewId, entityViewService::findEntityViewById, operation);
    }

    protected EntityViewInfo checkEntityViewInfoId(EntityViewId entityViewId, Operation operation) throws ThingsboardException {
        return checkEntityId(entityViewId, entityViewService::findEntityViewInfoById, operation);
    }

    protected Role checkRoleId(RoleId roleId, Operation operation) throws ThingsboardException {
        return checkEntityId(roleId, roleService::findRoleById, operation);
    }

    GroupPermission checkGroupPermissionId(GroupPermissionId groupPermissionId, Operation operation) throws ThingsboardException {
        return checkEntityId(groupPermissionId, groupPermissionService::findGroupPermissionById, operation);
    }

    GroupPermissionInfo checkGroupPermissionInfoId(GroupPermissionId groupPermissionId, Operation operation, boolean isUserGroup) throws ThingsboardException {
        return checkEntityId(groupPermissionId, (tenantId, id) -> {
            return groupPermissionService.findGroupPermissionInfoByIdAsync(getTenantId(), groupPermissionId, isUserGroup).get();
        }, operation);
    }

    Asset checkAssetId(AssetId assetId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetId, assetService::findAssetById, operation);
    }

    AssetInfo checkAssetInfoId(AssetId assetId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetId, assetService::findAssetInfoById, operation);
    }

    Integration checkIntegrationId(IntegrationId integrationId, Operation operation) throws ThingsboardException {
        return checkEntityId(integrationId, integrationService::findIntegrationById, operation);
    }

    Converter checkConverterId(ConverterId converterId, Operation operation) throws ThingsboardException {
        return checkEntityId(converterId, converterService::findConverterById, operation);
    }

    AssetProfile checkAssetProfileId(AssetProfileId assetProfileId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetProfileId, assetProfileService::findAssetProfileById, operation);
    }

    Alarm checkAlarmId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        return checkEntityId(alarmId, alarmService::findAlarmById, operation);
    }

    AlarmComment checkAlarmCommentId(AlarmCommentId alarmCommentId, AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmCommentId, id -> "Incorrect alarmCommentId " + id);
            AlarmComment alarmComment = alarmCommentService.findAlarmCommentByIdAsync(getCurrentUser().getTenantId(), alarmCommentId).get();
            checkNotNull(alarmComment, "Alarm comment with id [" + alarmCommentId + "] is not found");
            if (!alarmId.equals(alarmComment.getAlarmId())) {
                throw new ThingsboardException("Alarm id does not match with comment alarm id", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            return alarmComment;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        return checkEntityId(alarmId, alarmService::findAlarmInfoById, operation);
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetsBundleId, widgetsBundleService::findWidgetsBundleById, operation);
    }

    WidgetTypeDetails checkWidgetTypeId(WidgetTypeId widgetTypeId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetTypeId, widgetTypeService::findWidgetTypeDetailsById, operation);
    }

    WidgetTypeInfo checkWidgetTypeInfoId(WidgetTypeId widgetTypeId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetTypeId, widgetTypeService::findWidgetTypeInfoById, operation);
    }

    Dashboard checkDashboardId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        return checkEntityId(dashboardId, dashboardService::findDashboardById, operation);
    }

    Edge checkEdgeId(EdgeId edgeId, Operation operation) throws ThingsboardException {
        return checkEntityId(edgeId, edgeService::findEdgeById, operation);
    }

    EdgeInfo checkEdgeInfoId(EdgeId edgeId, Operation operation) throws ThingsboardException {
        return checkEntityId(edgeId, edgeService::findEdgeInfoById, operation);
    }

    DashboardInfo checkDashboardInfoId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        return checkEntityId(dashboardId, dashboardService::findDashboardInfoById, operation);
    }

    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            return checkNotNull(componentDescriptorService.getComponent(clazz));
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type, RuleChainType ruleChainType) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByTypes(Set<ComponentType> types, RuleChainType ruleChainType) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", types);
            return componentDescriptorService.getComponents(types, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected RuleChain checkRuleChain(RuleChainId ruleChainId, Operation operation) throws ThingsboardException {
        return checkEntityId(ruleChainId, ruleChainService::findRuleChainById, operation);
    }

    protected EntityGroupInfo checkEntityGroupId(EntityGroupId entityGroupId, Operation operation) throws ThingsboardException {
        try {
            validateId(entityGroupId, id -> "Incorrect entityGroupId " + id);
            EntityGroupInfo entityGroup = entityGroupService.findEntityGroupInfoById(getTenantId(), entityGroupId);
            checkNotNull(entityGroup, "Entity group with id [" + entityGroupId + "] is not found");
            accessControlService.checkEntityGroupInfoPermission(getCurrentUser(), operation, entityGroup);
            return entityGroup;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    SchedulerEvent checkSchedulerEventId(SchedulerEventId schedulerEventId, Operation operation) throws ThingsboardException {
        return checkEntityId(schedulerEventId, schedulerEventService::findSchedulerEventById, operation);
    }

    SchedulerEventWithCustomerInfo checkSchedulerEventInfoId(SchedulerEventId schedulerEventId, Operation operation) throws ThingsboardException {
        return checkEntityId(schedulerEventId, schedulerEventService::findSchedulerEventWithCustomerInfoById, operation);
    }

    BlobEntity checkBlobEntityId(BlobEntityId blobEntityId, Operation operation) throws ThingsboardException {
        return checkEntityId(blobEntityId, blobEntityService::findBlobEntityById, operation);
    }

    BlobEntityWithCustomerInfo checkBlobEntityInfoId(BlobEntityId blobEntityId, Operation operation) throws ThingsboardException {
        return checkEntityId(blobEntityId, blobEntityService::findBlobEntityWithCustomerInfoById, operation);
    }

    ReportTemplate checkReportTemplateId(ReportTemplateId reportTemplateId, Operation operation) throws ThingsboardException {
        return checkEntityId(reportTemplateId, reportTemplateService::findReportTemplateById, operation);
    }

    ReportTemplateInfo checkReportTemplateInfoId(ReportTemplateId reportTemplateId, Operation operation) throws ThingsboardException {
        return checkEntityId(reportTemplateId, reportTemplateService::findReportTemplateInfoById, operation);
    }

    Report checkReportId(ReportId reportId, Operation operation) throws ThingsboardException {
        return checkEntityId(reportId, reportService::findReportById, operation);
    }

    protected RuleNode checkRuleNode(RuleNodeId ruleNodeId, Operation operation) throws ThingsboardException {
        validateId(ruleNodeId, id -> "Incorrect ruleNodeId " + id);
        RuleNode ruleNode = ruleChainService.findRuleNodeById(getTenantId(), ruleNodeId);
        checkNotNull(ruleNode, "Rule node with id [" + ruleNodeId + "] is not found");
        checkRuleChain(ruleNode.getRuleChainId(), operation);
        return ruleNode;
    }

    TbResource checkResourceId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        return checkEntityId(resourceId, resourceService::findResourceById, operation);
    }

    TbResourceInfo checkResourceInfoId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        return checkEntityId(resourceId, resourceService::findResourceInfoById, operation);
    }

    protected ThingsboardException permissionDenied() {
        return new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

    OtaPackage checkOtaPackageId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        return checkEntityId(otaPackageId, otaPackageService::findOtaPackageById, operation);
    }

    OtaPackageInfo checkOtaPackageInfoId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        return checkEntityId(otaPackageId, otaPackageService::findOtaPackageInfoById, operation);
    }

    Rpc checkRpcId(RpcId rpcId) throws ThingsboardException {
        try {
            validateId(rpcId, id -> "Incorrect rpcId " + id);
            Rpc rpc = rpcService.findById(getCurrentUser().getTenantId(), rpcId);
            checkNotNull(rpc, "RPC with id [" + rpcId + "] is not found");
            checkDeviceId(rpc.getDeviceId(), Operation.RPC_CALL);
            return rpc;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected Queue checkQueueId(QueueId queueId, Operation operation) throws ThingsboardException {
        Queue queue = checkEntityId(queueId, queueService::findQueueById, operation);
        TenantId tenantId = getTenantId();
        if (queue.getTenantId().isNullUid() && !tenantId.isNullUid()) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            if (tenantProfile.isIsolatedTbRuleEngine()) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
        return queue;
    }

    OAuth2Client checkOauth2ClientId(OAuth2ClientId oAuth2ClientId, Operation operation) throws ThingsboardException {
        return checkEntityId(oAuth2ClientId, oAuth2ClientService::findOAuth2ClientById, operation);
    }

    Domain checkDomainId(DomainId domainId, Operation operation) throws ThingsboardException {
        return checkEntityId(domainId, domainService::findDomainById, operation);
    }

    MobileApp checkMobileAppId(MobileAppId mobileAppId, Operation operation) throws ThingsboardException {
        return checkEntityId(mobileAppId, mobileAppService::findMobileAppById, operation);
    }

    MobileAppBundle checkMobileAppBundleId(MobileAppBundleId mobileAppBundleId, Operation operation) throws ThingsboardException {
        return checkEntityId(mobileAppBundleId, mobileAppBundleService::findMobileAppBundleById, operation);
    }

    NotificationTarget checkNotificationTargetId(NotificationTargetId notificationTargetId, Operation operation) throws ThingsboardException {
        return checkEntityId(notificationTargetId, notificationTargetService::findNotificationTargetById, operation);
    }

    Job checkJobId(JobId jobId, Operation operation) throws ThingsboardException {
        return checkEntityId(jobId, jobService::findJobById, operation);
    }

    SecretInfo checkSecretId(SecretId secretId, Operation operation) throws ThingsboardException {
        return checkEntityId(secretId, secretService::findSecretInfoById, operation);
    }

    AiModel checkAiModelId(AiModelId modelId, Operation operation) throws ThingsboardException {
        return checkEntityId(modelId, (tenantId, id) -> aiModelService.findAiModelByTenantIdAndId(tenantId, id).orElse(null), operation);
    }

    ApiKey checkApiKeyId(ApiKeyId apiKeyId, Operation operation) throws ThingsboardException {
        return checkEntityId(apiKeyId, apiKeyService::findApiKeyById, operation);
    }

    Agent checkAgentId(AgentId agentId, Operation operation) throws ThingsboardException {
        return checkEntityId(agentId, agentService::findAgentById, operation);
    }


    AgentApplication checkAgentAppId(AgentApplicationId agentApplicationId, Operation operation) throws ThingsboardException {
        validateId(agentApplicationId, id -> "Incorrect agentApplicationId " + id);
        AgentApplication app = agentAppService.findById(getTenantId(), agentApplicationId);
        checkNotNull(app, "Agent application with id [" + agentApplicationId + "] is not found");
        checkAgentId(app.getAgentId(), operation);
        return app;
    }

    AgentAppUnit checkAgentAppUnitId(AgentAppUnitId agentAppUnitId, Operation operation) throws ThingsboardException {
        validateId(agentAppUnitId, id -> "Incorrect agentAppUnitId " + id);
        AgentAppUnit unit = agentAppUnitService.findAgentAppUnitById(getTenantId(), agentAppUnitId);
        checkNotNull(unit, "Agent application unit with id [" + agentAppUnitId + "] is not found");
        checkAgentAppId(unit.getAgentApplicationId(), operation);
        return unit;
    }

    AgentAppEvent checkAgentAppEventId(AgentAppEventId agentAppEventId, Operation operation) throws ThingsboardException {
        validateId(agentAppEventId, id -> "Incorrect agentAppEventId " + id);
        AgentAppEvent event = agentAppEventService.findById(getTenantId(), agentAppEventId);
        checkNotNull(event, "Agent application event with id [" + agentAppEventId + "] is not found");
        checkAgentId(event.getAgentId(), operation);
        return event;
    }

    AgentBulkAction checkAgentBulkActionId(AgentBulkActionId agentBulkActionId, Operation operation) throws ThingsboardException {
        validateId(agentBulkActionId, id -> "Incorrect agentBulkActionId " + id);
        AgentBulkAction bulkAction = agentBulkActionService.findById(getTenantId(), agentBulkActionId);
        checkNotNull(bulkAction, "Agent bulk action with id [" + agentBulkActionId + "] is not found");
        checkAgentProfileId(new AgentProfileId(bulkAction.getAgentProfileId()), operation);
        checkAgentAppProfileId(new AgentAppProfileId(bulkAction.getApplicationProfileId()), operation);
        return bulkAction;
    }

    AgentAppProfile checkAgentAppProfileId(AgentAppProfileId profileId, Operation operation) throws ThingsboardException {
        return checkEntityId(profileId, agentAppProfileService::findProfileById, operation);
    }

    AgentProfile checkAgentProfileId(AgentProfileId profileId, Operation operation) throws ThingsboardException {
        return checkEntityId(profileId, agentProfileService::findProfileById, operation);
    }

    /**
     * Tenant-scoped read of an agent application profile Info, deliberately without an AGENT_APP_PROFILE
     * permission check so agent/application screens can resolve the linked profile name.
     */
    AgentAppProfileInfo checkAgentAppProfileInfoId(AgentAppProfileId profileId) throws ThingsboardException {
        validateId(profileId, id -> "Incorrect agentAppProfileId " + id);
        AgentAppProfileInfo profileInfo = checkNotNull(agentAppProfileService.findProfileInfoById(getTenantId(), profileId));
        if (!getTenantId().equals(profileInfo.getTenantId())) {
            throw permissionDenied();
        }
        return profileInfo;
    }

    /**
     * Tenant-scoped read of an agent profile Info, deliberately without an AGENT_PROFILE permission check
     * so agent screens can resolve the linked profile name.
     */
    AgentProfileInfo checkAgentProfileInfoId(AgentProfileId agentProfileId) throws ThingsboardException {
        validateId(agentProfileId, id -> "Incorrect agentProfileId " + id);
        AgentProfileInfo profileInfo = checkNotNull(agentProfileService.findAgentProfileInfoById(getTenantId(), agentProfileId));
        if (!getTenantId().equals(profileInfo.getTenantId())) {
            throw permissionDenied();
        }
        return profileInfo;
    }

    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    public static Exception toException(Throwable error) {
        return error != null ? (error instanceof Exception ? (Exception) error : new Exception(error)) : null;
    }

    protected <E extends HasName & HasId<? extends EntityId>> void logEntityAction(SecurityUser user, EntityType entityType, E savedEntity, ActionType actionType) {
        logEntityAction(user, entityType, null, savedEntity, actionType, null);
    }

    protected <E extends HasName & HasId<? extends EntityId>> void logEntityAction(SecurityUser user, EntityType entityType, E entity, E savedEntity, ActionType actionType, Exception e) {
        EntityId entityId = savedEntity != null ? savedEntity.getId() : emptyId(entityType);
        entityActionService.logEntityAction(user, entityId, savedEntity != null ? savedEntity : entity,
                user.getCustomerId(), actionType, e);
    }

    protected MergedUserPermissions getMergedUserPermissions(User user, boolean isPublic) {
        try {
            return userPermissionsService.getMergedPermissions(user, isPublic);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }
    }

    protected <E> PageData<E> toPageData(List<E> entities, PageLink pageLink) {
        int totalElements = entities.size();
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        boolean hasNext = false;
        if (pageLink.getPageSize() > 0) {
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            int endIndex = startIndex + pageLink.getPageSize();
            if (entities.size() <= startIndex) {
                entities = Collections.emptyList();
            } else {
                if (endIndex > entities.size()) {
                    endIndex = entities.size();
                }
                entities = new ArrayList<>(entities.subList(startIndex, endIndex));
            }
            hasNext = totalElements > startIndex + entities.size();
        }
        return new PageData<>(entities, totalPages, totalElements, hasNext);
    }

    protected Comparator<ContactBased<? extends UUIDBased>> entityComparator = (e1, e2) -> {
        int result = e1.getName().compareToIgnoreCase(e2.getName());
        if (result == 0) {
            result = (int) (e2.getCreatedTime() - e1.getCreatedTime());
        }
        return result;
    };

    protected class EntityPageLinkFilter implements Predicate<ContactBased<? extends UUIDBased>> {

        private final String textSearch;

        EntityPageLinkFilter(PageLink pageLink) {
            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                this.textSearch = pageLink.getTextSearch().toLowerCase();
            } else {
                this.textSearch = "";
            }
        }

        @Override
        public boolean test(ContactBased<? extends UUIDBased> searchTextBased) {
            if (textSearch.length() > 0) {
                return searchTextBased.getName().toLowerCase().startsWith(textSearch);
            } else {
                return true;
            }
        }

    }

    protected <E extends HasName & HasId<? extends EntityId>> E doSaveAndLog(EntityType entityType, E entity, BiFunction<TenantId, E, E> savingFunction) throws Exception {
        ActionType actionType = entity.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        SecurityUser user = getCurrentUser();
        try {
            E savedEntity = savingFunction.apply(user.getTenantId(), entity);
            logEntityAction(user, entityType, savedEntity, actionType);
            return savedEntity;
        } catch (Exception e) {
            logEntityAction(user, entityType, entity, null, actionType, e);
            throw e;
        }
    }

    protected <E extends HasName & HasId<I>, I extends EntityId> void doDeleteAndLog(EntityType entityType, E entity, BiConsumer<TenantId, I> deleteFunction) throws Exception {
        SecurityUser user = getCurrentUser();
        try {
            deleteFunction.accept(user.getTenantId(), entity.getId());
            logEntityAction(user, entityType, entity, ActionType.DELETED);
        } catch (Exception e) {
            logEntityAction(user, entityType, entity, entity, ActionType.DELETED, e);
            throw e;
        }
    }

    protected void checkUserInfo(User user) throws ThingsboardException {
        ObjectNode info;
        if (user.getAdditionalInfo() instanceof ObjectNode additionalInfo) {
            info = additionalInfo;
            checkDashboardInfo(info);
        } else {
            info = JacksonUtil.newObjectNode();
            user.setAdditionalInfo(info);
        }

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
        info.put("userCredentialsEnabled", userCredentials.isEnabled());
        info.put("userActivated", userCredentials.getActivateToken() == null);
        info.put("lastLoginTs", userCredentials.getLastLoginTs());
    }

    protected void checkDashboardInfo(JsonNode additionalInfo) throws ThingsboardException {
        checkDashboardInfo(additionalInfo, DEFAULT_DASHBOARD);
        checkDashboardInfo(additionalInfo, HOME_DASHBOARD);
    }

    protected void checkDashboardInfo(JsonNode node, String dashboardField) throws ThingsboardException {
        if (node instanceof ObjectNode additionalInfo) {
            DashboardId dashboardId = Optional.ofNullable(additionalInfo.get(dashboardField))
                    .filter(JsonNode::isTextual).map(JsonNode::asText)
                    .map(id -> {
                        try {
                            return new DashboardId(UUID.fromString(id));
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }).orElse(null);

            if (dashboardId != null && !dashboardService.existsById(getTenantId(), dashboardId)) {
                additionalInfo.remove(dashboardField);
            }
        }
    }

    private CalculatedField checkCalculatedFieldId(CalculatedFieldId calculatedFieldId, Operation operation) throws ThingsboardException {
        validateId(calculatedFieldId, id -> "Invalid entity id");
        SecurityUser user = getCurrentUser();
        CalculatedField cf = calculatedFieldService.findById(user.getTenantId(), calculatedFieldId);
        checkNotNull(cf, calculatedFieldId.getEntityType().getNormalName() + " with id [" + calculatedFieldId + "] is not found");
        checkEntityId(cf.getEntityId(), operation);
        return cf;
    }

    protected HomeDashboardInfo getHomeDashboardInfo(SecurityUser securityUser, JsonNode additionalInfo) {
        HomeDashboardInfo homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
        if (homeDashboardInfo == null) {
            if (securityUser.isCustomerUser()) {
                Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(customer.getAdditionalInfo());
            }
            if (homeDashboardInfo == null) {
                Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(tenant.getAdditionalInfo());
            }
        }
        return homeDashboardInfo;
    }

    private HomeDashboardInfo extractHomeDashboardInfoFromAdditionalInfo(JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    protected MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    protected void throwRealCause(ExecutionException e) throws Exception {
        if (e.getCause() != null && e.getCause() instanceof Exception) {
            throw (Exception) e.getCause();
        } else {
            throw e;
        }
    }

    protected <T> DeferredResult<T> wrapFuture(ListenableFuture<T> future) {
        DeferredResult<T> deferredResult = new DeferredResult<>(); // Timeout of spring.mvc.async.request-timeout is used
        DonAsynchron.withCallback(future, deferredResult::setResult, deferredResult::setErrorResult);
        return deferredResult;
    }

    protected <T> DeferredResult<T> wrapFuture(ListenableFuture<T> future, long timeoutMs) {
        DeferredResult<T> deferredResult = new DeferredResult<>(timeoutMs);
        DonAsynchron.withCallback(future, deferredResult::setResult, deferredResult::setErrorResult);
        return deferredResult;
    }

    protected EntityDataSortOrder createEntityDataSortOrder(String sortProperty, String sortOrder) {
        if (isNotEmpty(sortProperty)) {
            EntityDataSortOrder entityDataSortOrder = new EntityDataSortOrder();
            entityDataSortOrder.setKey(new EntityKey(ENTITY_FIELD, sortProperty));
            if (isNotEmpty(sortOrder)) {
                entityDataSortOrder.setDirection(EntityDataSortOrder.Direction.valueOf(sortOrder));
            }
            return entityDataSortOrder;
        } else {
            return null;
        }
    }

    protected void compressResponseWithGzipIFAccepted(String acceptEncodingHeader, HttpServletResponse response, byte[] content) throws IOException {
        if (StringUtils.isNotEmpty(acceptEncodingHeader) && acceptEncodingHeader.contains("gzip")) {
            response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(response.getOutputStream())) {
                gzipOutputStream.write(content);
                gzipOutputStream.finish();
            }
        } else {
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                outputStream.write(content);
                outputStream.flush();
            }
        }
    }

    protected <T> ResponseEntity<T> response(HttpStatus status) {
        return ResponseEntity.status(status).build();
    }

    protected <T> ResponseEntity<T> redirectTo(String location) {
        URI uri;
        try {
            uri = URI.create(location);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create URI from '{}'", location, e);
            throw e;
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(uri)
                .build();
    }

    protected List<OAuth2ClientId> getOAuth2ClientIds(UUID[] ids) throws ThingsboardException {
        if (ids == null) {
            return Collections.emptyList();
        }
        List<OAuth2ClientId> oAuth2ClientIds = new ArrayList<>();
        for (UUID id : ids) {
            OAuth2ClientId oauth2ClientId = new OAuth2ClientId(id);
            checkOauth2ClientId(oauth2ClientId, Operation.READ);
            oAuth2ClientIds.add(oauth2ClientId);
        }
        return oAuth2ClientIds;
    }

}
