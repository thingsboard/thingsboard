// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
