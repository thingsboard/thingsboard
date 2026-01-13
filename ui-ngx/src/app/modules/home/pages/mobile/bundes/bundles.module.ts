///
/// Copyright © 2016-2026 The Thingsboard Authors
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
