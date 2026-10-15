// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Type } from '@angular/core';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityService } from '@core/http/entity.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { EntityViewService } from '@core/http/entity-view.service';
import { CustomerService } from '@core/http/customer.service';
import { DashboardService } from '@core/http/dashboard.service';
import { UserService } from '@core/http/user.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { RoleService } from '@core/http/role.service';
import { AlarmService } from '@core/http/alarm.service';
import { Router } from '@angular/router';
import { BroadcastService } from '@core/services/broadcast.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EdgeService } from '@core/http/edge.service';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { OtaPackageService } from '@core/http/ota-package.service';
import { RuleEngineService } from '@core/http/rule-engine.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthService } from '@core/auth/auth.service';
import { ResourceService } from '@core/http/resource.service';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NotificationService } from '@core/http/notification.service';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { UserSettingsService } from '@core/http/user-settings.service';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { ImageService } from '@core/http/image.service';
import { AlarmCommentService } from '@core/http/alarm-comment.service';
import { TenantService } from '@core/http/tenant.service';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { UiSettingsService } from '@core/http/ui-settings.service';
import { UsageInfoService } from '@core/http/usage-info.service';
import { EventService } from '@core/http/event.service';
import { UnitService } from '@core/services/unit.service';
import { AuditLogService } from '@core/http/audit-log.service';
import { BlobEntityService } from '@core/http/blob-entity.service';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { AiModelService } from '@core/http/ai-model.service';
import { DashboardReportService } from '@core/http/dashboard-report.service';
import { ReportService } from '@core/http/report.service';
import { ReportTemplateService } from '@core/http/report-template.service';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { AgentAutoProvisionDialogService } from '@home/components/agent/dialog/agent-auto-provision-dialog.service';
import { AgentDeployDialogService } from '@home/pages/agent/agent-deploy-dialog.service';

export const ServicesMap = new Map<string, Type<any>>(
  [
   ['broadcastService', BroadcastService],
   ['deviceService', DeviceService],
   ['alarmService', AlarmService],
   ['alarmCommentService', AlarmCommentService],
   ['assetService', AssetService],
   ['blobEntityService', BlobEntityService],
   ['entityViewService', EntityViewService],
   ['edgeService', EdgeService],
   ['customTranslationService', CustomTranslationService],
   ['customerService', CustomerService],
   ['dashboardService', DashboardService],
   ['dashboardReportService', DashboardReportService],
   ['userService', UserService],
   ['attributeService', AttributeService],
   ['entityRelationService', EntityRelationService],
   ['entityService', EntityService],
   ['entityGroupService', EntityGroupService],
   ['roleService', RoleService],
   ['dialogs', DialogService],
   ['customDialog', CustomDialogService],
   ['date', DatePipe],
   ['milliSecondsToTimeString', MillisecondsToTimeStringPipe],
   ['utils', UtilsService],
   ['translate', TranslateService],
   ['http', HttpClient],
   ['router', Router],
   ['imageService', ImageService],
   ['importExport', ImportExportService],
   ['schedulerEventService', SchedulerEventService],
   ['deviceProfileService', DeviceProfileService],
   ['assetProfileService', AssetProfileService],
   ['otaPackageService', OtaPackageService],
   ['ruleEngineService', RuleEngineService],
   ['userPermissionsService', UserPermissionsService],
   ['authService', AuthService],
   ['reportService', ReportService],
   ['resourceService', ResourceService],
   ['reportTemplateService', ReportTemplateService],
   ['twoFactorAuthenticationService', TwoFactorAuthenticationService],
   ['telemetryWsService', TelemetryWebsocketService],
   ['tenantService', TenantService],
   ['tenantProfileService', TenantProfileService],
   ['userSettingsService', UserSettingsService],
   ['uiSettingsService', UiSettingsService],
   ['usageInfoService', UsageInfoService],
   ['notificationService', NotificationService],
   ['eventService', EventService],
   ['unitService', UnitService],
   ['auditLogService', AuditLogService],
   ['actionNotificationShow', ActionNotificationShow],
   ['actionNotificationHide', ActionNotificationHide],
   ['store', Store],
   ['secretStorageService', SecretStorageService],
   ['aiModelService', AiModelService],
   ['agentAutoProvisionDialogService', AgentAutoProvisionDialogService],
   ['agentDeployDialogService', AgentDeployDialogService]
  ]
);
