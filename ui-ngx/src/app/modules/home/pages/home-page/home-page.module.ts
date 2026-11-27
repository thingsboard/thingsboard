// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HomePageRoutingModule } from './home-page-routing.module';
import { HomePageComponent } from './home-page.component';
import { SharedModule } from '@app/shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';

import { SysAdminHomeComponent } from '@home/pages/home-page/pages/sys-admin-home.component';
import { TenantAdminHomeComponent } from '@home/pages/home-page/pages/tenant-admin-home.component';
import { CustomerUserHomeComponent } from '@home/pages/home-page/pages/customer-user-home.component';
import { HomeCardComponent } from '@home/pages/home-page/components/home-card.component';
import { WelcomeHomeComponent } from '@home/pages/home-page/components/welcome-home.component';
import { StatusLineHomeComponent } from '@home/pages/home-page/components/status-line-home.component';
import { AiAssistantHomeComponent } from '@home/pages/home-page/components/ai-assistant-home.component';
import { WaitingAIChatsHomeComponent } from '@home/pages/home-page/components/waiting-ai-chats-home.component';
import { IoTHubHomeComponent } from '@home/pages/home-page/components/iot-hub-home.component';
import { AlarmsHomeComponent } from '@home/pages/home-page/components/alarms-home.component';
import { DashboardsHomeComponent } from '@home/pages/home-page/components/dashboards-home.component';
import { DevicesHomeComponent } from '@home/pages/home-page/components/devices-home.component';
import { DevicesActivityHomeComponent } from '@home/pages/home-page/components/devices-activity-home.component';
import { GetStartedHomeComponent } from '@home/pages/home-page/components/get-started-home.component';
import { NotificationsHomeComponent } from '@home/pages/home-page/components/notifications-home.component';
import { QuickLinksHomeComponent } from '@home/pages/home-page/components/quick-links-home.component';
import { ApiUsageHomeComponent } from '@home/pages/home-page/components/api-usage-home.component';
import { MobileAppHomeComponent } from '@home/pages/home-page/components/mobile-app-home.component';
import { HomeCardsFooterComponent } from '@home/pages/home-page/components/home-cards-footer.component';
import { CustomizeHomeDialogComponent } from '@home/pages/home-page/components/customize-home-dialog.component';
import { QuickLinkDialogComponent } from '@home/pages/home-page/components/quick-link-dialog.component';
import { HomeHscrollComponent } from '@home/pages/home-page/components/home-hscroll.component';
import { HomeToggleComponent } from '@home/pages/home-page/components/home-toggle.component';
import { HomeListFooterComponent } from '@home/pages/home-page/components/home-list-footer.component';
import { AiStartHomeComponent } from '@home/pages/home-page/components/ai-start-home.component';
import { ProductReleaseHomeComponent } from '@home/pages/home-page/components/product-release-home.component';
import { GetStartedSysAdminHomeComponent } from '@home/pages/home-page/components/get-started-sys-admin-home.component';
import { SystemStatusHomeComponent } from '@home/pages/home-page/components/system-status-home.component';
import { LicenseHomeComponent } from '@home/pages/home-page/components/license-home.component';
import { InstallItemsHomeComponent } from '@home/pages/home-page/components/install-items-home.component';
import { AiModule } from '@home/components/ai/ai.module';

@NgModule({
  declarations:
    [
      HomePageComponent,
      SysAdminHomeComponent,
      TenantAdminHomeComponent,
      CustomerUserHomeComponent,
      HomeCardComponent,
      WelcomeHomeComponent,
      StatusLineHomeComponent,
      AiAssistantHomeComponent,
      WaitingAIChatsHomeComponent,
      IoTHubHomeComponent,
      AlarmsHomeComponent,
      DashboardsHomeComponent,
      DevicesHomeComponent,
      DevicesActivityHomeComponent,
      GetStartedHomeComponent,
      NotificationsHomeComponent,
      QuickLinksHomeComponent,
      ApiUsageHomeComponent,
      MobileAppHomeComponent,
      HomeCardsFooterComponent,
      CustomizeHomeDialogComponent,
      QuickLinkDialogComponent,
      HomeHscrollComponent,
      HomeToggleComponent,
      HomeListFooterComponent,
      AiStartHomeComponent,
      ProductReleaseHomeComponent,
      GetStartedSysAdminHomeComponent,
      SystemStatusHomeComponent,
      LicenseHomeComponent,
      InstallItemsHomeComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AiModule,
    HomePageRoutingModule
  ]
})
export class HomePageModule { }
