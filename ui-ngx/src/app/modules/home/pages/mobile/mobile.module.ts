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
import { MobileRoutingModule } from '@home/pages/mobile/mobile-routing.module';
import { MobileApplicationModule } from '@home/pages/mobile/applications/applications.module';
import { MobileBundlesModule } from '@home/pages/mobile/bundes/bundles.module';
import {
  MobileQrCodeWidgetSettingsModule
} from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    MobileApplicationModule,
    MobileBundlesModule,
    MobileQrCodeWidgetSettingsModule,
    MobileRoutingModule,
  ]
})
export class MobileModule { }
