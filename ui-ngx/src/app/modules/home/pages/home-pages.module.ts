// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';

import { AdminModule } from './admin/admin.module';
import { HomeLinksModule } from './home-links/home-links.module';
import { ProfileModule } from './profile/profile.module';
import { SecurityModule } from '@home/pages/security/security.module';
import { TenantModule } from '@modules/home/pages/tenant/tenant.module';
import { CustomerModule } from '@modules/home/pages/customer/customer.module';
import { AuditLogModule } from '@modules/home/pages/audit-log/audit-log.module';
import { UserModule } from '@modules/home/pages/user/user.module';
import { DeviceModule } from '@modules/home/pages/device/device.module';
import { AssetModule } from '@modules/home/pages/asset/asset.module';
import { EntityViewModule } from '@modules/home/pages/entity-view/entity-view.module';
import { CalculatedFieldPageModule } from '@home/pages/calculated-fields/calculated-field-page.module';
import { RuleChainModule } from '@modules/home/pages/rulechain/rulechain.module';
import { WidgetLibraryModule } from '@modules/home/pages/widget/widget-library.module';
import { DashboardModule } from '@modules/home/pages/dashboard/dashboard.module';
import { IFrameViewModule } from '@home/pages/iframe/iframe-view.module';
import { ConverterModule } from '@home/pages/converter/converter.module';
import { IntegrationModule } from '@home/pages/integration/integration.module';
import { RoleModule } from '@home/pages/role/role.module';
import { SchedulerModule } from '@home/pages/scheduler/scheduler.module';
import { EntityGroupModule } from '@home/pages/group/entity-group.module';
import { TenantProfileModule } from './tenant-profile/tenant-profile.module';
import { DeviceProfileModule } from './device-profile/device-profile.module';
import { ApiUsageModule } from '@home/pages/api-usage/api-usage.module';
import { EdgeModule } from '@home/pages/edge/edge.module';
import { OtaUpdateModule } from '@home/pages/ota-update/ota-update.module';
import { SolutionCreatorModule } from '@home/pages/ai-solution-creator/solution-creator.module';
import { VcModule } from '@home/pages/vc/vc.module';
import { TaskManagerModule } from '@home/pages/task-manager/task-manager.module';
import { AssetProfileModule } from '@home/pages/asset-profile/asset-profile.module';
import { ProfilesModule } from '@home/pages/profiles/profiles.module';
import { AlarmModule } from '@home/pages/alarm/alarm.module';
import { EntitiesModule } from '@home/pages/entities/entities.module';
import { FeaturesModule } from '@home/pages/features/features.module';
import { NotificationModule } from '@home/pages/notification/notification.module';
import { AccountModule } from '@home/pages/account/account.module';
import { IntegrationsCenterModule } from '@home/pages/integration/integrations-center.module';
import { CustomTranslationModule } from '@home/pages/custom-translation/custom-translation.module';
import { ScadaSymbolModule } from '@home/pages/scada-symbol/scada-symbol.module';
import { GatewaysModule } from '@home/pages/gateways/gateways.module';
import { MobileModule } from '@home/pages/mobile/mobile.module';
import { CustomMenuModule } from '@home/pages/custom-menu/custom-menu.module';
import { SecretStorageModule } from '@home/pages/secret-storage/secret-storage.module';
import { AiModelModule } from '@home/pages/ai-model/ai-model.module';
import { ReportingModule } from '@home/pages/reporting/reporting.module';
import { TrendzAnalyticsModule } from '@home/pages/trendz-analytics/trendz-analytics.module';
import { TrendzSettingsModule } from '@home/pages/trendz-settings/trendz-settings.module';
import { AgentModule } from '@home/pages/agent/agent.module';
import { IotHubModule } from '@home/pages/iot-hub/iot-hub.module';

@NgModule({
  exports: [
    AdminModule,
    HomeLinksModule,
    ProfileModule,
    SecurityModule,
    TenantProfileModule,
    TenantModule,
    DeviceProfileModule,
    AssetProfileModule,
    ProfilesModule,
    EntitiesModule,
    FeaturesModule,
    MobileModule,
    NotificationModule,
    DeviceModule,
    AssetModule,
    AlarmModule,
    EdgeModule,
    AgentModule,
    EntityViewModule,
    CustomerModule,
    CalculatedFieldPageModule,
    RuleChainModule,
    WidgetLibraryModule,
    DashboardModule,
    AuditLogModule,
    ApiUsageModule,
    GatewaysModule,
    OtaUpdateModule,
    UserModule,
    AccountModule,
    RoleModule,
    SecretStorageModule,
    IntegrationsCenterModule,
    ConverterModule,
    IntegrationModule,
    EntityGroupModule,
    IFrameViewModule,
    SchedulerModule,
    OtaUpdateModule,
    SolutionCreatorModule,
    VcModule,
    TaskManagerModule,
    AccountModule,
    ScadaSymbolModule,
    CustomTranslationModule,
    CustomMenuModule,
    AiModelModule,
    ReportingModule,
    TrendzAnalyticsModule,
    TrendzSettingsModule,
    IotHubModule
  ]
})
export class HomePagesModule { }
