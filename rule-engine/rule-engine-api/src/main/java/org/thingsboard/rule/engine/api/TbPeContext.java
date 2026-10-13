// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.secret.SecretService;

import java.util.Set;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbPeContext {

    IntegrationService getIntegrationService();

    EntityGroupService getEntityGroupService();

    DashboardReportService getDashboardReportService();

    BlobEntityService getBlobEntityService();

    ReportTemplateService getReportTemplateService();

    ReportService getReportService();

    GroupPermissionService getGroupPermissionService();

    RoleService getRoleService();

    ConverterService getConverterService();

    SchedulerEventService getSchedulerEventService();

    SecretService getSecretService();

    AgentService getAgentService();

    AgentApplicationService getAgentApplicationService();

    AgentAppEventService getAgentAppEventService();

    AgentAppUnitService getAgentAppUnitService();

    AgentAppProfileService getAgentAppProfileService();

    AgentProfileService getAgentProfileService();

    AgentBulkActionService getAgentBulkActionService();

    DeviceGroupOtaPackageService getDeviceGroupOtaPackageService();

    EntityId getOwner(TenantId tenantId, EntityId entityId);

    void clearOwners(EntityId entityId);

    Set<EntityId> getChildOwners(TenantId tenantId, EntityId parentOwnerId);

    void changeDashboardOwner(TenantId tenantId, EntityId targetOwnerId, Dashboard dashboard) throws ThingsboardException;

    void changeUserOwner(TenantId tenantId, EntityId targetOwnerId, User user) throws ThingsboardException;

    void changeCustomerOwner(TenantId tenantId, EntityId targetOwnerId, Customer customer) throws ThingsboardException;

    void changeEntityViewOwner(TenantId tenantId, EntityId targetOwnerId, EntityView entityView) throws ThingsboardException;

    void changeAssetOwner(TenantId tenantId, EntityId targetOwnerId, Asset asset) throws ThingsboardException;

    void changeDeviceOwner(TenantId tenantId, EntityId targetOwnerId, Device device) throws ThingsboardException;

    void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId) throws ThingsboardException;

    void pushToIntegration(IntegrationId integrationId, TbMsg tbMsg, FutureCallback<Void> callback);

    void ack(TbMsg msg);

    boolean isLocalEntity(EntityId entityId);

    ScriptEngine createAttributesScriptEngine(ScriptLanguage scriptLang, String script);

}
