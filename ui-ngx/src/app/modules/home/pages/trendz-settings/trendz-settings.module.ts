// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrendzSettingsComponent } from '@home/pages/trendz-settings/trendz-settings.component';
import { SharedModule } from '@shared/shared.module';
import { TrendzSettingsRoutingModule } from '@home/pages/trendz-settings/trendz-settings-routing.module';



@NgModule({
  declarations: [
    TrendzSettingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    TrendzSettingsRoutingModule
  ]
})
export class TrendzSettingsModule { }
