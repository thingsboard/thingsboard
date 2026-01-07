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
import {
  ScadaSymbolMetadataTagComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag.component';
import {
  ScadaSymbolMetadataComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata.component';
import {
  ScadaSymbolMetadataTagsComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tags.component';
import {
  ScadaSymbolBehaviorsComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behaviors.component';
import {
  ScadaSymbolBehaviorRowComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-row.component';
import {
  ScadaSymbolBehaviorPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-panel.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import {
  ScadaSymbolMetadataTagFunctionPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag-function-panel.component';

@NgModule({
  declarations:
    [
      ScadaSymbolMetadataComponent,
      ScadaSymbolMetadataTagComponent,
      ScadaSymbolMetadataTagsComponent,
      ScadaSymbolMetadataTagFunctionPanelComponent,
      ScadaSymbolBehaviorsComponent,
      ScadaSymbolBehaviorRowComponent,
      ScadaSymbolBehaviorPanelComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetSettingsCommonModule
  ],
  exports: [
    ScadaSymbolMetadataComponent
  ]
})
export class ScadaSymbolMetadataComponentsModule { }
