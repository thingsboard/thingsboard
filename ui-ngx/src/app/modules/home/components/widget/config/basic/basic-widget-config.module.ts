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
import { WidgetService } from '@core/http/widget.service';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import {
  SimpleCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/simple-card-basic-config.component';
import {
  WidgetActionsPanelComponent
} from '@home/components/widget/config/basic/common/widget-actions-panel.component';
import {
  EntitiesTableBasicConfigComponent
} from '@home/components/widget/config/basic/entity/entities-table-basic-config.component';
import { DataKeysPanelComponent } from '@home/components/widget/config/basic/common/data-keys-panel.component';
import { DataKeyRowComponent } from '@home/components/widget/config/basic/common/data-key-row.component';
import {
  TimeseriesTableBasicConfigComponent
} from '@home/components/widget/config/basic/cards/timeseries-table-basic-config.component';
import { FlotBasicConfigComponent } from '@home/components/widget/config/basic/chart/flot-basic-config.component';
import {
  AlarmsTableBasicConfigComponent
} from '@home/components/widget/config/basic/alarm/alarms-table-basic-config.component';
import {
  ValueCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/value-card-basic-config.component';
import {
  AggregatedValueCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/aggregated-value-card-basic-config.component';
import {
  AggregatedDataKeyRowComponent
} from '@home/components/widget/config/basic/cards/aggregated-data-key-row.component';
import {
  AggregatedDataKeysPanelComponent
} from '@home/components/widget/config/basic/cards/aggregated-data-keys-panel.component';
import {
  AlarmCountBasicConfigComponent
} from '@home/components/widget/config/basic/alarm/alarm-count-basic-config.component';
import {
  EntityCountBasicConfigComponent
} from '@home/components/widget/config/basic/entity/entity-count-basic-config.component';
import {
  BatteryLevelBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/battery-level-basic-config.component';
import {
  WindSpeedDirectionBasicConfigComponent
} from '@home/components/widget/config/basic/weather/wind-speed-direction-basic-config.component';
import {
  SignalStrengthBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/signal-strength-basic-config.component';
import {
  ValueChartCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/value-chart-card-basic-config.component';
import {
  ProgressBarBasicConfigComponent
} from '@home/components/widget/config/basic/cards/progress-bar-basic-config.component';
import {
  RadialGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/radial-gauge-basic-config.component';
import {
  ThermometerScaleGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/thermometer-scale-gauge-basic-config.component';
import {
  CompassGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/compass-gauge-basic-config.component';
import {
  LiquidLevelCardBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/liquid-level-card-basic-config.component';
import {
  DoughnutBasicConfigComponent
} from '@home/components/widget/config/basic/chart/doughnut-basic-config.component';
import {
  RangeChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/range-chart-basic-config.component';
import {
  BarChartWithLabelsBasicConfigComponent
} from '@home/components/widget/config/basic/chart/bar-chart-with-labels-basic-config.component';
import {
  SingleSwitchBasicConfigComponent
} from '@home/components/widget/config/basic/rpc/single-switch-basic-config.component';
import {
  ActionButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/action-button-basic-config.component';
import {
  CommandButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/command-button-basic-config.component';
import {
  PowerButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/power-button-basic-config.component';
import { SliderBasicConfigComponent } from '@home/components/widget/config/basic/rpc/slider-basic-config.component';
import {
  ToggleButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/toggle-button-basic-config.component';
import {
  TimeSeriesChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/time-series-chart-basic-config.component';
import { ComparisonKeyRowComponent } from '@home/components/widget/config/basic/chart/comparison-key-row.component';
import {
  ComparisonKeysTableComponent
} from '@home/components/widget/config/basic/chart/comparison-keys-table.component';
import {
  StatusWidgetBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/status-widget-basic-config.component';
import {
  PieChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/pie-chart-basic-config.component';
import {
  BarChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/bar-chart-basic-config.component';
import {
  PolarAreaChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/polar-area-chart-basic-config.component';
import {
  RadarChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/radar-chart-basic-config.component';
import {
  DigitalSimpleGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/digital-simple-gauge-basic-config.component';
import { MobileAppQrCodeBasicConfigComponent } from '@home/components/widget/config/basic/cards/mobile-app-qr-code-basic-config.component';
import {
  LabelCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/label-card-basic-config.component';
import {
  LabelValueCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/label-value-card-basic-config.component';
import {
  UnreadNotificationBasicConfigComponent
} from '@home/components/widget/config/basic/cards/unread-notification-basic-config.component';
import { ScadaSymbolBasicConfigComponent } from '@home/components/widget/config/basic/scada/scada-symbol-basic-config.component';
import {
  SegmentedButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/segmented-button-basic-config.component';
import {
  ValueStepperBasicConfigComponent
} from '@home/components/widget/config/basic/rpc/value-stepper-basic-config.component';
import { MapBasicConfigComponent } from '@home/components/widget/config/basic/map/map-basic-config.component';

@NgModule({
  declarations: [
    WidgetActionsPanelComponent,
    SimpleCardBasicConfigComponent,
    EntitiesTableBasicConfigComponent,
    TimeseriesTableBasicConfigComponent,
    FlotBasicConfigComponent,
    AlarmsTableBasicConfigComponent,
    ValueCardBasicConfigComponent,
    AggregatedValueCardBasicConfigComponent,
    AggregatedDataKeyRowComponent,
    AggregatedDataKeysPanelComponent,
    DataKeyRowComponent,
    DataKeysPanelComponent,
    AlarmCountBasicConfigComponent,
    EntityCountBasicConfigComponent,
    BatteryLevelBasicConfigComponent,
    WindSpeedDirectionBasicConfigComponent,
    SignalStrengthBasicConfigComponent,
    ValueChartCardBasicConfigComponent,
    ProgressBarBasicConfigComponent,
    RadialGaugeBasicConfigComponent,
    ThermometerScaleGaugeBasicConfigComponent,
    CompassGaugeBasicConfigComponent,
    LiquidLevelCardBasicConfigComponent,
    DoughnutBasicConfigComponent,
    RangeChartBasicConfigComponent,
    BarChartWithLabelsBasicConfigComponent,
    SingleSwitchBasicConfigComponent,
    ActionButtonBasicConfigComponent,
    SegmentedButtonBasicConfigComponent,
    CommandButtonBasicConfigComponent,
    PowerButtonBasicConfigComponent,
    SliderBasicConfigComponent,
    ToggleButtonBasicConfigComponent,
    ValueStepperBasicConfigComponent,
    TimeSeriesChartBasicConfigComponent,
    ComparisonKeyRowComponent,
    ComparisonKeysTableComponent,
    StatusWidgetBasicConfigComponent,
    PieChartBasicConfigComponent,
    BarChartBasicConfigComponent,
    PolarAreaChartBasicConfigComponent,
    RadarChartBasicConfigComponent,
    DigitalSimpleGaugeBasicConfigComponent,
    MobileAppQrCodeBasicConfigComponent,
    LabelCardBasicConfigComponent,
    LabelValueCardBasicConfigComponent,
    UnreadNotificationBasicConfigComponent,
    ScadaSymbolBasicConfigComponent,
    MapBasicConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetConfigComponentsModule
  ],
  exports: [
    WidgetActionsPanelComponent,
    SimpleCardBasicConfigComponent,
    EntitiesTableBasicConfigComponent,
    TimeseriesTableBasicConfigComponent,
    FlotBasicConfigComponent,
    AlarmsTableBasicConfigComponent,
    ValueCardBasicConfigComponent,
    AggregatedValueCardBasicConfigComponent,
    AggregatedDataKeyRowComponent,
    AggregatedDataKeysPanelComponent,
    DataKeyRowComponent,
    DataKeysPanelComponent,
    AlarmCountBasicConfigComponent,
    EntityCountBasicConfigComponent,
    BatteryLevelBasicConfigComponent,
    WindSpeedDirectionBasicConfigComponent,
    SignalStrengthBasicConfigComponent,
    ValueChartCardBasicConfigComponent,
    ProgressBarBasicConfigComponent,
    RadialGaugeBasicConfigComponent,
    ThermometerScaleGaugeBasicConfigComponent,
    CompassGaugeBasicConfigComponent,
    LiquidLevelCardBasicConfigComponent,
    DoughnutBasicConfigComponent,
    RangeChartBasicConfigComponent,
    BarChartWithLabelsBasicConfigComponent,
    SingleSwitchBasicConfigComponent,
    ActionButtonBasicConfigComponent,
    SegmentedButtonBasicConfigComponent,
    CommandButtonBasicConfigComponent,
    PowerButtonBasicConfigComponent,
    SliderBasicConfigComponent,
    ToggleButtonBasicConfigComponent,
    ValueStepperBasicConfigComponent,
    TimeSeriesChartBasicConfigComponent,
    StatusWidgetBasicConfigComponent,
    PieChartBasicConfigComponent,
    BarChartBasicConfigComponent,
    PolarAreaChartBasicConfigComponent,
    RadarChartBasicConfigComponent,
    ScadaSymbolBasicConfigComponent,
    DigitalSimpleGaugeBasicConfigComponent,
    MobileAppQrCodeBasicConfigComponent,
    LabelCardBasicConfigComponent,
    LabelValueCardBasicConfigComponent,
    UnreadNotificationBasicConfigComponent,
    MapBasicConfigComponent
  ]
})
export class BasicWidgetConfigModule {
  constructor(private widgetService: WidgetService) {
    this.widgetService.registerBasicWidgetConfigComponents(this.constructor)
  }
}
