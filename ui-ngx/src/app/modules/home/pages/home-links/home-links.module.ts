// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HomeLinksRoutingModule } from './home-links-routing.module';
import { HomeLinksComponent } from './home-links.component';
import { SharedModule } from '@app/shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';

import { HomePageComponent } from '@home/pages/home-links/home/pages/home-page.component';
import { SysAdminHomeComponent } from '@home/pages/home-links/home/pages/sys-admin-home.component';
import { TenantAdminHomeComponent } from '@home/pages/home-links/home/pages/tenant-admin-home.component';
import { CustomerUserHomeComponent } from '@home/pages/home-links/home/pages/customer-user-home.component';
import { HomeCardComponent } from '@home/pages/home-links/home/components/home-card.component';
import { WelcomeHomeComponent } from '@home/pages/home-links/home/components/welcome-home.component';
import { StatusLineHomeComponent } from '@home/pages/home-links/home/components/status-line-home.component';
import { AiAssistantHomeComponent } from '@home/pages/home-links/home/components/ai-assistant-home.component';
import { WaitingAIChatsHomeComponent } from '@home/pages/home-links/home/components/waiting-ai-chats-home.component';
import { IoTHubHomeComponent } from '@home/pages/home-links/home/components/iot-hub-home.component';
import { AlarmsHomeComponent } from '@home/pages/home-links/home/components/alarms-home.component';
import { DashboardsHomeComponent } from '@home/pages/home-links/home/components/dashboards-home.component';
import { DevicesHomeComponent } from '@home/pages/home-links/home/components/devices-home.component';
import { DevicesActivityHomeComponent } from '@home/pages/home-links/home/components/devices-activity-home.component';
import { GetStartedHomeComponent } from '@home/pages/home-links/home/components/get-started-home.component';
import { NotificationsHomeComponent } from '@home/pages/home-links/home/components/notifications-home.component';
import { QuickLinksHomeComponent } from '@home/pages/home-links/home/components/quick-links-home.component';
import { ApiUsageHomeComponent } from '@home/pages/home-links/home/components/api-usage-home.component';
import { MobileAppHomeComponent } from '@home/pages/home-links/home/components/mobile-app-home.component';
import { HomeCardsFooterComponent } from '@home/pages/home-links/home/components/home-cards-footer.component';
import { CustomizeHomeDialogComponent } from '@home/pages/home-links/home/components/customize-home-dialog.component';
import { QuickLinkDialogComponent } from '@home/pages/home-links/home/components/quick-link-dialog.component';
import { HomeHscrollComponent } from '@home/pages/home-links/home/components/home-hscroll.component';
import { HomeToggleComponent } from '@home/pages/home-links/home/components/home-toggle.component';
import { HomeListFooterComponent } from '@home/pages/home-links/home/components/home-list-footer.component';
import { AiStartHomeComponent } from '@home/pages/home-links/home/components/ai-start-home.component';
import { AiModule } from '@home/components/ai/ai.module';

@NgModule({
  declarations:
    [
      HomeLinksComponent,
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
      AiStartHomeComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AiModule,
    HomeLinksRoutingModule
  ]
})
export class HomeLinksModule { }
