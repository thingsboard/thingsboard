// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { MobileBundleRoutingModule } from '@home/pages/mobile/bundes/bundles-routing.module';
import { MobileBundleTableHeaderComponent } from '@home/pages/mobile/bundes/mobile-bundle-table-header.component';
import { MobileBundleDialogComponent } from '@home/pages/mobile/bundes/mobile-bundle-dialog.component';
import { MobileLayoutComponent } from '@home/pages/mobile/bundes/layout/mobile-layout.component';
import { MobilePageItemRowComponent } from '@home/pages/mobile/bundes/layout/mobile-page-item-row.component';
import { AddMobilePageDialogComponent } from '@home/pages/mobile/bundes/layout/add-mobile-page-dialog.component';
import { CustomMobilePageComponent } from '@home/pages/mobile/bundes/layout/custom-mobile-page.component';
import { CustomMobilePagePanelComponent } from '@home/pages/mobile/bundes/layout/custom-mobile-page-panel.component';
import { DefaultMobilePagePanelComponent } from '@home/pages/mobile/bundes/layout/default-mobile-page-panel.component';
import {
  MobileAppConfigurationDialogComponent
} from '@home/pages/mobile/bundes/mobile-app-configuration-dialog.component';


@NgModule({
  declarations: [
    MobileBundleTableHeaderComponent,
    MobileBundleDialogComponent,
    MobileLayoutComponent,
    MobilePageItemRowComponent,
    AddMobilePageDialogComponent,
    CustomMobilePageComponent,
    CustomMobilePagePanelComponent,
    DefaultMobilePagePanelComponent,
    MobileAppConfigurationDialogComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    MobileBundleRoutingModule,
  ]
})
export class MobileBundlesModule { }
