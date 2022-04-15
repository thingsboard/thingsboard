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
import { QrCodeWidgetSettingsComponent } from '@home/components/widget/lib/settings/cards/qrcode-widget-settings.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { IWidgetSettingsComponent } from '@shared/models/widget.models';
import {
  TimeseriesTableWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/timeseries-table-widget-settings.component';
import {
  TimeseriesTableKeySettingsComponent
} from '@home/components/widget/lib/settings/cards/timeseries-table-key-settings.component';
import {
  TimeseriesTableLatestKeySettingsComponent
} from '@home/components/widget/lib/settings/cards/timeseries-table-latest-key-settings.component';
import {
  MarkdownWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/markdown-widget-settings.component';
import { WidgetFontComponent } from '@home/components/widget/lib/settings/common/widget-font.component';
import { LabelWidgetLabelComponent } from '@home/components/widget/lib/settings/cards/label-widget-label.component';
import { LabelWidgetSettingsComponent } from '@home/components/widget/lib/settings/cards/label-widget-settings.component';
import {
  SimpleCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/simple-card-widget-settings.component';
import {
  DashboardStateWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/dashboard-state-widget-settings.component';
import {
  EntitiesHierarchyWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/entities-hierarchy-widget-settings.component';
import {
  HtmlCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/html-card-widget-settings.component';
import {
  EntitiesTableWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/entities-table-widget-settings.component';
import {
  EntitiesTableKeySettingsComponent
} from '@home/components/widget/lib/settings/cards/entities-table-key-settings.component';
import {
  AlarmsTableWidgetSettingsComponent
} from '@home/components/widget/lib/settings/alarm/alarms-table-widget-settings.component';
import {
  AlarmsTableKeySettingsComponent
} from '@home/components/widget/lib/settings/alarm/alarms-table-key-settings.component';
import { GaugeHighlightComponent } from '@home/components/widget/lib/settings/gauge/gauge-highlight.component';
import {
  AnalogueRadialGaugeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/analogue-radial-gauge-widget-settings.component';
import {
  AnalogueLinearGaugeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/analogue-linear-gauge-widget-settings.component';
import {
  AnalogueCompassWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/analogue-compass-widget-settings.component';
import {
  DigitalGaugeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/digital-gauge-widget-settings.component';
import { ValueSourceComponent } from '@home/components/widget/lib/settings/common/value-source.component';
import { FixedColorLevelComponent } from '@home/components/widget/lib/settings/gauge/fixed-color-level.component';
import { TickValueComponent } from '@home/components/widget/lib/settings/gauge/tick-value.component';
import { FlotWidgetSettingsComponent } from '@home/components/widget/lib/settings/chart/flot-widget-settings.component';
import {
  FlotLineWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-line-widget-settings.component';
import { LabelDataKeyComponent } from '@home/components/widget/lib/settings/chart/label-data-key.component';
import {
  FlotBarWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-bar-widget-settings.component';
import { FlotThresholdComponent } from '@home/components/widget/lib/settings/chart/flot-threshold.component';
import { FlotKeySettingsComponent } from '@home/components/widget/lib/settings/chart/flot-key-settings.component';
import {
  FlotLineKeySettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-line-key-settings.component';
import {
  FlotBarKeySettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-bar-key-settings.component';
import {
  FlotLatestKeySettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-latest-key-settings.component';

@NgModule({
  declarations: [
    QrCodeWidgetSettingsComponent,
    TimeseriesTableWidgetSettingsComponent,
    TimeseriesTableKeySettingsComponent,
    TimeseriesTableLatestKeySettingsComponent,
    MarkdownWidgetSettingsComponent,
    WidgetFontComponent,
    LabelWidgetLabelComponent,
    LabelWidgetSettingsComponent,
    SimpleCardWidgetSettingsComponent,
    DashboardStateWidgetSettingsComponent,
    EntitiesHierarchyWidgetSettingsComponent,
    HtmlCardWidgetSettingsComponent,
    EntitiesTableWidgetSettingsComponent,
    EntitiesTableKeySettingsComponent,
    AlarmsTableWidgetSettingsComponent,
    AlarmsTableKeySettingsComponent,
    GaugeHighlightComponent,
    AnalogueRadialGaugeWidgetSettingsComponent,
    AnalogueLinearGaugeWidgetSettingsComponent,
    AnalogueCompassWidgetSettingsComponent,
    DigitalGaugeWidgetSettingsComponent,
    ValueSourceComponent,
    FixedColorLevelComponent,
    TickValueComponent,
    FlotWidgetSettingsComponent,
    LabelDataKeyComponent,
    FlotLineWidgetSettingsComponent,
    FlotBarWidgetSettingsComponent,
    FlotThresholdComponent,
    FlotKeySettingsComponent,
    FlotLineKeySettingsComponent,
    FlotBarKeySettingsComponent,
    FlotLatestKeySettingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule
  ],
  exports: [
    QrCodeWidgetSettingsComponent,
    TimeseriesTableWidgetSettingsComponent,
    TimeseriesTableKeySettingsComponent,
    TimeseriesTableLatestKeySettingsComponent,
    MarkdownWidgetSettingsComponent,
    WidgetFontComponent,
    LabelWidgetLabelComponent,
    LabelWidgetSettingsComponent,
    SimpleCardWidgetSettingsComponent,
    DashboardStateWidgetSettingsComponent,
    EntitiesHierarchyWidgetSettingsComponent,
    HtmlCardWidgetSettingsComponent,
    EntitiesTableWidgetSettingsComponent,
    EntitiesTableKeySettingsComponent,
    AlarmsTableWidgetSettingsComponent,
    AlarmsTableKeySettingsComponent,
    GaugeHighlightComponent,
    AnalogueRadialGaugeWidgetSettingsComponent,
    AnalogueLinearGaugeWidgetSettingsComponent,
    AnalogueCompassWidgetSettingsComponent,
    DigitalGaugeWidgetSettingsComponent,
    ValueSourceComponent,
    FixedColorLevelComponent,
    TickValueComponent,
    FlotWidgetSettingsComponent,
    LabelDataKeyComponent,
    FlotLineWidgetSettingsComponent,
    FlotBarWidgetSettingsComponent,
    FlotThresholdComponent,
    FlotKeySettingsComponent,
    FlotLineKeySettingsComponent,
    FlotBarKeySettingsComponent,
    FlotLatestKeySettingsComponent
  ]
})
export class WidgetSettingsModule {
}

export const widgetSettingsComponentsMap: {[key: string]: Type<IWidgetSettingsComponent>} = {
  'tb-qrcode-widget-settings': QrCodeWidgetSettingsComponent,
  'tb-timeseries-table-widget-settings': TimeseriesTableWidgetSettingsComponent,
  'tb-timeseries-table-key-settings': TimeseriesTableKeySettingsComponent,
  'tb-timeseries-table-latest-key-settings': TimeseriesTableLatestKeySettingsComponent,
  'tb-markdown-widget-settings': MarkdownWidgetSettingsComponent,
  'tb-label-widget-settings': LabelWidgetSettingsComponent,
  'tb-simple-card-widget-settings': SimpleCardWidgetSettingsComponent,
  'tb-dashboard-state-widget-settings': DashboardStateWidgetSettingsComponent,
  'tb-entities-hierarchy-widget-settings': EntitiesHierarchyWidgetSettingsComponent,
  'tb-html-card-widget-settings': HtmlCardWidgetSettingsComponent,
  'tb-entities-table-widget-settings': EntitiesTableWidgetSettingsComponent,
  'tb-entities-table-key-settings': EntitiesTableKeySettingsComponent,
  'tb-alarms-table-widget-settings': AlarmsTableWidgetSettingsComponent,
  'tb-alarms-table-key-settings': AlarmsTableKeySettingsComponent,
  'tb-analogue-radial-gauge-widget-settings': AnalogueRadialGaugeWidgetSettingsComponent,
  'tb-analogue-linear-gauge-widget-settings': AnalogueLinearGaugeWidgetSettingsComponent,
  'tb-analogue-compass-widget-settings': AnalogueCompassWidgetSettingsComponent,
  'tb-digital-gauge-widget-settings': DigitalGaugeWidgetSettingsComponent,
  'tb-flot-line-widget-settings': FlotLineWidgetSettingsComponent,
  'tb-flot-bar-widget-settings': FlotBarWidgetSettingsComponent,
  'tb-flot-line-key-settings': FlotLineKeySettingsComponent,
  'tb-flot-bar-key-settings': FlotBarKeySettingsComponent,
  'tb-flot-latest-key-settings': FlotLatestKeySettingsComponent
};
