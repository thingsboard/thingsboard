// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc;

import lombok.Getter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AiModelEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomMenuEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomTranslationEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DefaultProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceGroupOtaPackageEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EncryptionKeyEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EntityGroupEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.IntegrationsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationRuleEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationTargetEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationTemplateEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OAuth2EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.PublicCustomerUserGroupEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.ReportTemplateEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SchedulerEventsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SecretEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SysAdminRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemCustomMenuEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantResourcesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.WhiteLabelingEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

@Getter
public class EdgeSyncCursor {

    private final List<EdgeEventFetcher> fetchers = new LinkedList<>();

    private int currentIdx = 0;

    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
        if (fullSync) {
            fetchers.add(new TenantEdgeEventFetcher(ctx.getTenantService()));
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService(), TenantId.SYS_TENANT_ID));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService(), edge.getTenantId()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            fetchers.add(new SysAdminRolesEdgeEventFetcher(ctx.getRoleService()));
            fetchers.add(new TenantRolesEdgeEventFetcher(ctx.getRoleService()));
            fetchers.add(new OAuth2EdgeEventFetcher(ctx.getDomainService(), TenantId.SYS_TENANT_ID, new CustomerId(CustomerId.NULL_UUID)));
            fetchers.add(new OAuth2EdgeEventFetcher(ctx.getDomainService(), edge.getTenantId(), new CustomerId(CustomerId.NULL_UUID)));
            if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                addCustomerOAuth2EdgeEventFetchers(ctx, edge.getTenantId(), new CustomerId(edge.getOwnerId().getId()));
            }
            fetchers.add(new WhiteLabelingEdgeEventFetcher(ctx.getCustomerService()));
            fetchers.add(new SystemWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new TenantWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new AiModelEdgeEventFetcher(ctx.getAiModelService()));
        }
        Customer publicCustomer = ctx.getCustomerService().findPublicCustomer(edge.getTenantId(), edge.getTenantId());
        if (publicCustomer != null) {
            fetchers.add(new CustomerEdgeEventFetcher(ctx.getCustomerService(), publicCustomer.getId()));
            fetchers.add(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), publicCustomer.getId()));
            fetchers.add(new PublicCustomerUserGroupEdgeEventFetcher(ctx.getCustomerService(), edge.getTenantId()));
        }
        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
            CustomerId customerId = new CustomerId(edge.getOwnerId().getId());
            fetchers.add(new CustomerEdgeEventFetcher(ctx.getCustomerService(), customerId));
            addCustomerRolesEdgeEventFetchers(ctx, edge.getTenantId(), customerId);
        }
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DASHBOARD));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.USER));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.ASSET));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DEVICE));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.ENTITY_VIEW));
        fetchers.add(new SchedulerEventsEdgeEventFetcher(ctx.getSchedulerEventService()));
        if (fullSync) {
            fetchers.add(new NotificationTemplateEdgeEventFetcher(ctx.getNotificationTemplateService()));
            fetchers.add(new NotificationTargetEdgeEventFetcher(ctx.getNotificationTargetService()));
            fetchers.add(new NotificationRuleEdgeEventFetcher(ctx.getNotificationRuleService()));
            fetchers.add(new IntegrationsEdgeEventFetcher(ctx.getIntegrationService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
            // sync device profiles twice to update software and hardware fields
            fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
            fetchers.add(new TenantResourcesEdgeEventFetcher(ctx.getResourceService()));
            fetchers.add(new DeviceGroupOtaPackageEdgeEventFetcher(ctx.getDeviceGroupOtaPackageService(), ctx.getEntityGroupService()));
            fetchers.add(new CustomTranslationEdgeEventFetcher(ctx.getCustomerService(), ctx.getCustomTranslationService()));
            fetchers.add(new SystemCustomMenuEdgeEventFetcher(ctx.getCustomMenuService()));
            fetchers.add(new CustomMenuEdgeEventFetcher(ctx.getCustomMenuService()));
            fetchers.add(new EncryptionKeyEdgeEventFetcher(ctx.getEncryptionService()));
            fetchers.add(new SecretEdgeEventFetcher(ctx.getSecretService()));
            fetchers.add(new ReportTemplateEdgeEventFetcher(ctx.getReportTemplateService(), null));
            if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                fetchers.add(new ReportTemplateEdgeEventFetcher(ctx.getReportTemplateService(), new CustomerId(edge.getOwnerId().getId())));
            }
        }
    }

    private void addCustomerRolesEdgeEventFetchers(EdgeContextComponent ctx, TenantId tenantId, CustomerId customerId) {
        fetchers.add(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), customerId));
        Customer publicCustomer = ctx.getCustomerService().findPublicCustomer(tenantId, customerId);
        if (publicCustomer != null) {
            fetchers.add(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), publicCustomer.getId()));
            fetchers.add(new PublicCustomerUserGroupEdgeEventFetcher(ctx.getCustomerService(), customerId));
        }
        Customer customerById = ctx.getCustomerService().findCustomerById(tenantId, customerId);
        if (customerById != null && customerById.getParentCustomerId() != null && !customerById.getParentCustomerId().isNullUid()) {
            addCustomerRolesEdgeEventFetchers(ctx, tenantId, customerById.getParentCustomerId());
        }
    }

    private void addCustomerOAuth2EdgeEventFetchers(EdgeContextComponent ctx, TenantId tenantId, CustomerId customerId) {
        fetchers.add(new OAuth2EdgeEventFetcher(ctx.getDomainService(), tenantId, customerId));
        Customer customerById = ctx.getCustomerService().findCustomerById(tenantId, customerId);
        if (customerById != null && customerById.getParentCustomerId() != null && !customerById.getParentCustomerId().isNullUid()) {
            addCustomerOAuth2EdgeEventFetchers(ctx, tenantId, customerById.getParentCustomerId());
        }
    }

    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    public EdgeEventFetcher getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

}
