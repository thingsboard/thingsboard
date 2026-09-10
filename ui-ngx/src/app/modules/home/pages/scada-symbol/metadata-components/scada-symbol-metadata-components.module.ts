// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
