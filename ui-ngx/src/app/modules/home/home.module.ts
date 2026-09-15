// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

@NgModule({
  declarations:
    [
      HomeComponent,
      MenuLinkComponent,
      MenuToggleComponent,
      SideMenuComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    NotificationBellModule,
    GithubBadgeModule,
    HomeRoutingModule
  ]
})
export class HomeModule { }
