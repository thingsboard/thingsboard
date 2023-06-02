///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { IBasicWidgetConfigComponent } from '@home/components/widget/widget-config.component.models';
import { WidgetConfigComponentsModule } from '@home/components/widget/widget-config-components.module';
import {
  SimpleCardBasicConfigComponent
} from '@home/components/widget/basic-config/cards/simple-card-basic-config.component';
import {
  WidgetActionsPanelComponent
} from '@home/components/widget/basic-config/common/widget-actions-panel.component';

@NgModule({
  declarations: [
    WidgetActionsPanelComponent,
    SimpleCardBasicConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetConfigComponentsModule
  ],
  exports: [
    WidgetActionsPanelComponent,
    SimpleCardBasicConfigComponent
  ]
})
export class BasicWidgetConfigModule {
}

export const basicWidgetConfigComponentsMap: {[key: string]: Type<IBasicWidgetConfigComponent>} = {
  'tb-simple-card-basic-config': SimpleCardBasicConfigComponent
};
