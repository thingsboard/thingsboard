///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { NgModule, Type } from '@angular/core';
import { QrCodeWidgetSettingsComponent } from '@home/components/widget/lib/settings/qrcode-widget-settings.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { IWidgetSettingsComponent } from '@shared/models/widget.models';

@NgModule({
  declarations: [
    QrCodeWidgetSettingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule
  ],
  exports: [
    QrCodeWidgetSettingsComponent
  ]
})
export class WidgetSettingsModule {
}

export const widgetSettingsComponentsMap: {[key: string]: Type<IWidgetSettingsComponent>} = {
  'tb-qrcode-widget-settings': QrCodeWidgetSettingsComponent
};
