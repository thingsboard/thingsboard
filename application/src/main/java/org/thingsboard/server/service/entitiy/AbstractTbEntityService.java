/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.edge.EdgeNotificationService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractTbEntityService {

    protected static final int DEFAULT_PAGE_SIZE = 1000;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;
    @Value("${edges.enabled}")
    @Getter
    protected boolean edgesEnabled;

    @Autowired
    protected DbCallbackExecutorService dbExecutor;
    @Autowired(required = false)
    protected TbNotificationEntityService notificationEntityService;
    @Autowired(required = false)
    protected EdgeService edgeService;
    @Autowired
    protected AlarmService alarmService;
    @Autowired(required = false)
    protected EntityActionService entityActionService;
    @Autowired
    protected DeviceService deviceService;
    @Autowired
    protected AssetService assetService;
    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;
    @Autowired
    protected TenantService tenantService;
    @Autowired
    protected CustomerService customerService;
    @Lazy
    @Autowired(required = false)
    protected ClaimDevicesService claimDevicesService;
    @Autowired
    protected TbTenantProfileCache tenantProfileCache;
    @Autowired
    protected RuleChainService ruleChainService;
    @Autowired(required = false)
    protected TbRuleChainService tbRuleChainService;
    @Autowired(required = false)
    protected EdgeNotificationService edgeNotificationService;
    @Autowired
    protected QueueService queueService;
    @Autowired
    protected DashboardService dashboardService;

    @Autowired(required = false)
    private EntitiesVersionControlService vcService;
    @Autowired
    protected EntityViewService entityViewService;
    @Lazy
    @Autowired
    protected TelemetrySubscriptionService tsSubService;
    @Autowired
    protected AttributesService attributesService;
    @Autowired
    protected AccessControlService accessControlService;
    @Autowired
    protected DeviceProfileService deviceProfileService;
    @Autowired
    protected TbClusterService tbClusterService;
    @Autowired
    protected OtaPackageStateService otaPackageStateService;
    @Autowired
    protected RelationService relationService;
    @Autowired
    protected OtaPackageService otaPackageService;
    @Autowired
    protected InstallScripts installScripts;
    @Autowired
    protected UserService userService;
    @Autowired(required = false)
    protected TbResourceService resourceService;
    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    protected ListenableFuture<Void> removeAlarmsByEntityId(TenantId tenantId, EntityId entityId) {
        ListenableFuture<PageData<AlarmInfo>> alarmsFuture =
                alarmService.findAlarms(tenantId, new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE), null, null, false));

        ListenableFuture<List<AlarmId>> alarmIdsFuture = Futures.transform(alarmsFuture, page ->
                page.getData().stream().map(AlarmInfo::getId).collect(Collectors.toList()), dbExecutor);

        return Futures.transform(alarmIdsFuture, ids -> {
            ids.stream().map(alarmId -> alarmService.deleteAlarm(tenantId, alarmId)).collect(Collectors.toList());
            return null;
        }, dbExecutor);
    }

    protected <E extends HasName, I extends EntityId> void logEntityAction(User user, TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                           ActionType actionType, Exception e, Object... additionalInfo) throws ThingsboardException {
        if (user != null) {
            entityActionService.logEntityAction(user, entityId, entity, customerId, actionType, e, additionalInfo);
        } else if (e == null) {
            entityActionService.pushEntityActionToRuleEngine(entityId, entity, tenantId, customerId, actionType, null, additionalInfo);
        }
    }

    protected <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    protected <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    protected <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    protected <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    protected ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    protected ThingsboardException handleException(Exception exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
        } else {
            return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.GENERAL);
        }
    }

    @SuppressWarnings("unchecked")
    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected List<EdgeId> findRelatedEdgeIds(TenantId tenantId, EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    protected ListenableFuture<UUID> autoCommit(SecurityUser user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }
}
