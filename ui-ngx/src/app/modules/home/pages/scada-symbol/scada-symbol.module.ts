// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
