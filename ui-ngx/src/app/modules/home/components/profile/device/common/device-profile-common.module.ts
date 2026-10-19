// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { PowerModeSettingComponent } from '@home/components/profile/device/common/power-mode-setting.component';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TimeUnitSelectComponent } from '@home/components/profile/device/common/time-unit-select.component';

@NgModule({
  declarations: [
    PowerModeSettingComponent,
    TimeUnitSelectComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    PowerModeSettingComponent
  ]
})
export class DeviceProfileCommonModule { }
