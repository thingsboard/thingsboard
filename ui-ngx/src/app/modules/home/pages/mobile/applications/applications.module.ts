// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { MobileAppComponent } from '@home/pages/mobile/applications/mobile-app.component';
import { MobileAppTableHeaderComponent } from '@home/pages/mobile/applications/mobile-app-table-header.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { ApplicationsRoutingModule } from '@home/pages/mobile/applications/applications-routing.module';
import { MobileAppDialogComponent } from '@home/pages/mobile/applications/mobile-app-dialog.component';
import { RemoveAppDialogComponent } from '@home/pages/mobile/applications/remove-app-dialog.component';
import { CommonMobileModule } from '@home/pages/mobile/common/common-mobile.module';

@NgModule({
  declarations: [
    MobileAppComponent,
    MobileAppTableHeaderComponent,
    MobileAppDialogComponent,
    RemoveAppDialogComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CommonMobileModule,
    ApplicationsRoutingModule,
  ],
  exports: [
    MobileAppDialogComponent,
  ]
})
export class MobileApplicationModule { }
