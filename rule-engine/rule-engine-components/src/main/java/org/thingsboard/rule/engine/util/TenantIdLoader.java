/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.util;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.UUID;

public class TenantIdLoader {

    public static TenantId findTenantId(TbContext ctx, EntityId entityId) {
        UUID id = entityId.getId();
        EntityType entityType = entityId.getEntityType();
        TenantId ctxTenantId = ctx.getTenantId();

        HasTenantId tenantEntity;
        switch (entityType) {
            case TENANT:
                return TenantId.fromUUID(id);
            case CUSTOMER:
                tenantEntity = ctx.getCustomerService().findCustomerById(ctxTenantId, new CustomerId(id));
                break;
            case USER:
                tenantEntity = ctx.getUserService().findUserById(ctxTenantId, new UserId(id));
                break;
            case ASSET:
                tenantEntity = ctx.getAssetService().findAssetById(ctxTenantId, new AssetId(id));
                break;
            case DEVICE:
                tenantEntity = ctx.getDeviceService().findDeviceById(ctxTenantId, new DeviceId(id));
                break;
            case ALARM:
                tenantEntity = ctx.getAlarmService().findAlarmById(ctxTenantId, new AlarmId(id));
                break;
            case RULE_CHAIN:
                tenantEntity = ctx.getRuleChainService().findRuleChainById(ctxTenantId, new RuleChainId(id));
                break;
            case ENTITY_VIEW:
                tenantEntity = ctx.getEntityViewService().findEntityViewById(ctxTenantId, new EntityViewId(id));
                break;
            case DASHBOARD:
                tenantEntity = ctx.getDashboardService().findDashboardById(ctxTenantId, new DashboardId(id));
                break;
            case EDGE:
                tenantEntity = ctx.getEdgeService().findEdgeById(ctxTenantId, new EdgeId(id));
                break;
            case OTA_PACKAGE:
                tenantEntity = ctx.getOtaPackageService().findOtaPackageInfoById(ctxTenantId, new OtaPackageId(id));
                break;
            case ASSET_PROFILE:
                tenantEntity = ctx.getAssetProfileCache().get(ctxTenantId, new AssetProfileId(id));
                break;
            case DEVICE_PROFILE:
                tenantEntity = ctx.getDeviceProfileCache().get(ctxTenantId, new DeviceProfileId(id));
                break;
            case WIDGET_TYPE:
                tenantEntity = ctx.getWidgetTypeService().findWidgetTypeById(ctxTenantId, new WidgetTypeId(id));
                break;
            case WIDGETS_BUNDLE:
                tenantEntity = ctx.getWidgetBundleService().findWidgetsBundleById(ctxTenantId, new WidgetsBundleId(id));
                break;
            case RPC:
                tenantEntity = ctx.getRpcService().findRpcById(ctxTenantId, new RpcId(id));
                break;
            case QUEUE:
                tenantEntity = ctx.getQueueService().findQueueById(ctxTenantId, new QueueId(id));
                break;
            case API_USAGE_STATE:
                tenantEntity = ctx.getRuleEngineApiUsageStateService().findApiUsageStateById(ctxTenantId, new ApiUsageStateId(id));
                break;
            case TB_RESOURCE:
                tenantEntity = ctx.getResourceService().findResourceInfoById(ctxTenantId, new TbResourceId(id));
                break;
            case RULE_NODE:
                RuleNode ruleNode = ctx.getRuleChainService().findRuleNodeById(ctxTenantId, new RuleNodeId(id));
                if (ruleNode != null) {
                    tenantEntity = ctx.getRuleChainService().findRuleChainById(ctxTenantId, ruleNode.getRuleChainId());
                } else {
                    tenantEntity = null;
                }
                break;
            case TENANT_PROFILE:
                if (ctx.getTenantProfile().getId().equals(entityId)) {
                    return ctxTenantId;
                } else {
                    tenantEntity = null;
                }
                break;
            case NOTIFICATION_TARGET:
                tenantEntity = ctx.getNotificationTargetService().findNotificationTargetById(ctxTenantId, new NotificationTargetId(id));
                break;
            case NOTIFICATION_TEMPLATE:
                tenantEntity = ctx.getNotificationTemplateService().findNotificationTemplateById(ctxTenantId, new NotificationTemplateId(id));
                break;
            case NOTIFICATION_REQUEST:
                tenantEntity = ctx.getNotificationRequestService().findNotificationRequestById(ctxTenantId, new NotificationRequestId(id));
                break;
            case NOTIFICATION:
            case ADMIN_SETTINGS:
                return ctxTenantId;
            case NOTIFICATION_RULE:
                tenantEntity = ctx.getNotificationRuleService().findNotificationRuleById(ctxTenantId, new NotificationRuleId(id));
                break;
            case QUEUE_STATS:
                tenantEntity = ctx.getQueueStatsService().findQueueStatsById(ctxTenantId, new QueueStatsId(id));
                break;
            case OAUTH2_CLIENT:
                tenantEntity = ctx.getOAuth2ClientService().findOAuth2ClientById(ctxTenantId, new OAuth2ClientId(id));
                break;
            case DOMAIN:
                tenantEntity = ctx.getDomainService().findDomainById(ctxTenantId, new DomainId(id));
                break;
            case MOBILE_APP:
                tenantEntity = ctx.getMobileAppService().findMobileAppById(ctxTenantId, new MobileAppId(id));
                break;
            case MOBILE_APP_BUNDLE:
                tenantEntity = ctx.getMobileAppBundleService().findMobileAppBundleById(ctxTenantId, new MobileAppBundleId(id));
                break;
            case CALCULATED_FIELD:
                tenantEntity = ctx.getCalculatedFieldService().findById(ctxTenantId, new CalculatedFieldId(id));
                break;
            case CALCULATED_FIELD_LINK:
                CalculatedFieldLink calculatedFieldLink = ctx.getCalculatedFieldService().findCalculatedFieldLinkById(ctxTenantId, new CalculatedFieldLinkId(id));
                if (calculatedFieldLink != null) {
                    tenantEntity = ctx.getCalculatedFieldService().findById(ctxTenantId, calculatedFieldLink.getCalculatedFieldId());
                } else {
                    tenantEntity = null;
                }
                break;
            case JOB:
                tenantEntity = ctx.getJobService().findJobById(ctxTenantId, new JobId(id));
                break;
            case AI_MODEL:
                tenantEntity = ctx.getAiModelService().findAiModelById(ctxTenantId, new AiModelId(id)).orElse(null);
                break;
            case API_KEY:
                tenantEntity = ctx.getApiKeyService().findApiKeyById(ctxTenantId, new ApiKeyId(id));
                break;
            default:
                throw new RuntimeException("Unexpected entity type: " + entityId.getEntityType());
        }
        return tenantEntity != null ? tenantEntity.getTenantId() : null;
    }

}
