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
import { SharedModule } from '@app/shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ScadaSymbolComponent } from '@home/pages/scada-symbol/scada-symbol.component';
import { ScadaSymbolEditorComponent } from '@home/pages/scada-symbol/scada-symbol-editor.component';
import { ScadaSymbolTooltipComponentsModule } from '@home/pages/scada-symbol/scada-symbol-tooltip.components';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import {
  ScadaSymbolMetadataComponentsModule
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-components.module';

@NgModule({
  declarations:
    [
      ScadaSymbolEditorComponent,
      ScadaSymbolComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    ScadaSymbolMetadataComponentsModule,
    ScadaSymbolTooltipComponentsModule,
    WidgetSettingsCommonModule
  ]
})
export class ScadaSymbolModule { }
