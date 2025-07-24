///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
import { AlarmService } from '@core/http/alarm.service';
import { Router } from '@angular/router';
import { BroadcastService } from '@core/services/broadcast.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { OtaPackageService } from '@core/http/ota-package.service';
import { AuthService } from '@core/auth/auth.service';
import { ResourceService } from '@core/http/resource.service';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NotificationService } from '@core/http/notification.service';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { UserSettingsService } from '@core/http/user-settings.service';
import { ImageService } from '@core/http/image.service';
import { AlarmCommentService } from '@core/http/alarm-comment.service';
import { TenantService } from '@core/http/tenant.service';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { UiSettingsService } from '@core/http/ui-settings.service';
import { UsageInfoService } from '@core/http/usage-info.service';
import { EventService } from '@core/http/event.service';
import { UnitService } from '@core/services/unit.service';
import { AuditLogService } from '@core/http/audit-log.service';
import { TrendzSettingsService } from '@core/http/trendz-settings.service';
import { AiModelService } from '@core/http/ai-model.service';

export const ServicesMap = new Map<string, Type<any>>(
  [
   ['broadcastService', BroadcastService],
   ['deviceService', DeviceService],
   ['alarmService', AlarmService],
   ['alarmCommentService', AlarmCommentService],
   ['assetService', AssetService],
   ['entityViewService', EntityViewService],
   ['customerService', CustomerService],
   ['dashboardService', DashboardService],
   ['userService', UserService],
   ['attributeService', AttributeService],
   ['entityRelationService', EntityRelationService],
   ['entityService', EntityService],
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
   ['deviceProfileService', DeviceProfileService],
   ['assetProfileService', AssetProfileService],
   ['otaPackageService', OtaPackageService],
   ['authService', AuthService],
   ['resourceService', ResourceService],
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
   ['trendzSettingsService', TrendzSettingsService],
   ['aiModelService', AiModelService]
  ]
);
