///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { WidgetFontComponent } from '@home/components/widget/lib/settings/common/widget-font.component';
import { ValueSourceComponent } from '@home/components/widget/lib/settings/common/value-source.component';
import { LegendConfigComponent } from '@home/components/widget/lib/settings/common/legend-config.component';
import {
  ImageCardsSelectComponent,
  ImageCardsSelectOptionDirective
} from '@home/components/widget/lib/settings/common/image-cards-select.component';
import { FontSettingsComponent } from '@home/components/widget/lib/settings/common/font-settings.component';
import { FontSettingsPanelComponent } from '@home/components/widget/lib/settings/common/font-settings-panel.component';
import {
  ColorSettingsComponent,
  ColorSettingsComponentService
} from '@home/components/widget/lib/settings/common/color-settings.component';
import {
  ColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/color-settings-panel.component';
import { CssUnitSelectComponent } from '@home/components/widget/lib/settings/common/css-unit-select.component';
import { DateFormatSelectComponent } from '@home/components/widget/lib/settings/common/date-format-select.component';
import {
  DateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/date-format-settings-panel.component';
import { BackgroundSettingsComponent } from '@home/components/widget/lib/settings/common/background-settings.component';
import {
  BackgroundSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/background-settings-panel.component';
import {
  CountWidgetSettingsComponent
} from '@home/components/widget/lib/settings/common/count-widget-settings.component';
import { ColorRangeListComponent } from '@home/components/widget/lib/settings/common/color-range-list.component';
import { ColorRangePanelComponent } from '@home/components/widget/lib/settings/common/color-range-panel.component';
import {
  ColorRangeSettingsComponent, ColorRangeSettingsComponentService
} from '@home/components/widget/lib/settings/common/color-range-settings.component';
import {
  RpcInitialStateSettingsComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-initial-state-settings.component';
import {
  RpcInitialStateSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-initial-state-settings-panel.component';
import {
  DeviceKeyAutocompleteComponent
} from '@home/components/widget/lib/settings/control/device-key-autocomplete.component';
import {
  RpcUpdateStateSettingsComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-update-state-settings.component';
import {
  RpcUpdateStateSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-update-state-settings-panel.component';
import { CssSizeInputComponent } from '@home/components/widget/lib/settings/common/css-size-input.component';

@NgModule({
  declarations: [
    ImageCardsSelectOptionDirective,
    ImageCardsSelectComponent,
    FontSettingsComponent,
    FontSettingsPanelComponent,
    ColorSettingsComponent,
    ColorSettingsPanelComponent,
    CssUnitSelectComponent,
    CssSizeInputComponent,
    DateFormatSelectComponent,
    DateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent,
    ColorRangeListComponent,
    ColorRangePanelComponent,
    ColorRangeSettingsComponent,
    RpcInitialStateSettingsComponent,
    RpcInitialStateSettingsPanelComponent,
    DeviceKeyAutocompleteComponent,
    RpcUpdateStateSettingsComponent,
    RpcUpdateStateSettingsPanelComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule
  ],
  exports: [
    ImageCardsSelectOptionDirective,
    ImageCardsSelectComponent,
    FontSettingsComponent,
    FontSettingsPanelComponent,
    ColorSettingsComponent,
    ColorSettingsPanelComponent,
    CssUnitSelectComponent,
    CssSizeInputComponent,
    DateFormatSelectComponent,
    DateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent,
    ColorRangeListComponent,
    ColorRangePanelComponent,
    ColorRangeSettingsComponent,
    RpcInitialStateSettingsComponent,
    RpcInitialStateSettingsPanelComponent,
    DeviceKeyAutocompleteComponent,
    RpcUpdateStateSettingsComponent,
    RpcUpdateStateSettingsPanelComponent
  ],
  providers: [
    ColorSettingsComponentService,
    ColorRangeSettingsComponentService
  ]
})
export class WidgetSettingsCommonModule {
}
