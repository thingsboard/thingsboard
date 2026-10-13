// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HomeRoutingModule } from './home-routing.module';
import { HomeComponent } from './home.component';
import { SharedModule } from '@app/shared/shared.module';
import { MenuLinkComponent } from '@modules/home/menu/menu-link.component';
import { MenuToggleComponent } from '@modules/home/menu/menu-toggle.component';
import { SideMenuComponent } from '@modules/home/menu/side-menu.component';
import { NotificationBellModule } from '@home/components/notification/notification-bell.module';
import { GithubBadgeModule } from '@home/components/github-badge/github-badge.module';
import { AiModule } from '@home/components/ai/ai.module';
import { GotoMenuComponent } from '@home/menu/goto-menu.component';

@NgModule({
  declarations:
    [
      HomeComponent,
      MenuLinkComponent,
      MenuToggleComponent,
      SideMenuComponent,
      GotoMenuComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    NotificationBellModule,
    GithubBadgeModule,
    HomeRoutingModule,
    AiModule
  ]
})
export class HomeModule { }
