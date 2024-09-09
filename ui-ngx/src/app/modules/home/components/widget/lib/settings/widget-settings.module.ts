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

import { NgModule, Type } from '@angular/core';
import {
  QrCodeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/qrcode-widget-settings.component';
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
import { LabelWidgetLabelComponent } from '@home/components/widget/lib/settings/cards/label-widget-label.component';
import {
  LabelWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/label-widget-settings.component';
import {
  SimpleCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/simple-card-widget-settings.component';
import {
  DashboardStateWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/dashboard-state-widget-settings.component';
import {
  EntitiesHierarchyWidgetSettingsComponent
} from '@home/components/widget/lib/settings/entity/entities-hierarchy-widget-settings.component';
import {
  HtmlCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/html-card-widget-settings.component';
import {
  EntitiesTableWidgetSettingsComponent
} from '@home/components/widget/lib/settings/entity/entities-table-widget-settings.component';
import {
  EntitiesTableKeySettingsComponent
} from '@home/components/widget/lib/settings/entity/entities-table-key-settings.component';
import {
  AlarmsTableWidgetSettingsComponent
} from '@home/components/widget/lib/settings/alarm/alarms-table-widget-settings.component';
import {
  AlarmsTableKeySettingsComponent
} from '@home/components/widget/lib/settings/alarm/alarms-table-key-settings.component';
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
import {
  FlotPieWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-pie-widget-settings.component';
import {
  FlotPieKeySettingsComponent
} from '@home/components/widget/lib/settings/chart/flot-pie-key-settings.component';
import {
  ChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/chart-widget-settings.component';
import {
  DoughnutChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/doughnut-chart-widget-settings.component';
import { SwitchRpcSettingsComponent } from '@home/components/widget/lib/settings/control/switch-rpc-settings.component';
import {
  RoundSwitchWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/round-switch-widget-settings.component';
import {
  SwitchControlWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/switch-control-widget-settings.component';
import {
  SlideToggleWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/slide-toggle-widget-settings.component';
import {
  PersistentTableWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/persistent-table-widget-settings.component';
import { RpcButtonStyleComponent } from '@home/components/widget/lib/settings/control/rpc-button-style.component';
import {
  UpdateDeviceAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/update-device-attribute-widget-settings.component';
import {
  SendRpcWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/send-rpc-widget-settings.component';
import {
  LedIndicatorWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/led-indicator-widget-settings.component';
import {
  KnobControlWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/knob-control-widget-settings.component';
import {
  RpcTerminalWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/rpc-terminal-widget-settings.component';
import {
  RpcShellWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/rpc-shell-widget-settings.component';
import {
  DateRangeNavigatorWidgetSettingsComponent
} from '@home/components/widget/lib/settings/date/date-range-navigator-widget-settings.component';
import {
  EdgeQuickOverviewWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/edge-quick-overview-widget-settings.component';
import {
  GatewayConfigWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gateway/gateway-config-widget-settings.component';
import {
  GatewayConfigSingleDeviceWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gateway/gateway-config-single-device-widget-settings.component';
import {
  GatewayEventsWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gateway/gateway-events-widget-settings.component';
import { GpioItemComponent } from '@home/components/widget/lib/settings/gpio/gpio-item.component';
import {
  GpioControlWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gpio/gpio-control-widget-settings.component';
import {
  GpioPanelWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gpio/gpio-panel-widget-settings.component';
import {
  NavigationCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/navigation/navigation-card-widget-settings.component';
import {
  NavigationCardsWidgetSettingsComponent
} from '@home/components/widget/lib/settings/navigation/navigation-cards-widget-settings.component';
import {
  DeviceClaimingWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/device-claiming-widget-settings.component';
import {
  UpdateAttributeGeneralSettingsComponent
} from '@home/components/widget/lib/settings/input/update-attribute-general-settings.component';
import {
  UpdateIntegerAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-integer-attribute-widget-settings.component';
import {
  UpdateDoubleAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-double-attribute-widget-settings.component';
import {
  UpdateStringAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-string-attribute-widget-settings.component';
import {
  UpdateBooleanAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-boolean-attribute-widget-settings.component';
import {
  UpdateImageAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-image-attribute-widget-settings.component';
import {
  UpdateDateAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-date-attribute-widget-settings.component';
import {
  UpdateLocationAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-location-attribute-widget-settings.component';
import {
  UpdateJsonAttributeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-json-attribute-widget-settings.component';
import {
  PhotoCameraInputWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/photo-camera-input-widget-settings.component';
import {
  UpdateMultipleAttributesWidgetSettingsComponent
} from '@home/components/widget/lib/settings/input/update-multiple-attributes-widget-settings.component';
import {
  DataKeySelectOptionComponent
} from '@home/components/widget/lib/settings/input/datakey-select-option.component';
import {
  UpdateMultipleAttributesKeySettingsComponent
} from '@home/components/widget/lib/settings/input/update-multiple-attributes-key-settings.component';
import {
  OpenStreetMapProviderSettingsComponent
} from '@home/components/widget/lib/settings/map/openstreet-map-provider-settings.component';
import { MapProviderSettingsComponent } from '@home/components/widget/lib/settings/map/map-provider-settings.component';
import { MapSettingsComponent } from '@home/components/widget/lib/settings/map/map-settings.component';
import { MapWidgetSettingsComponent } from '@home/components/widget/lib/settings/map/map-widget-settings.component';
import {
  GoogleMapProviderSettingsComponent
} from '@home/components/widget/lib/settings/map/google-map-provider-settings.component';
import {
  HereMapProviderSettingsComponent
} from '@home/components/widget/lib/settings/map/here-map-provider-settings.component';
import {
  TencentMapProviderSettingsComponent
} from '@home/components/widget/lib/settings/map/tencent-map-provider-settings.component';
import {
  ImageMapProviderSettingsComponent
} from '@home/components/widget/lib/settings/map/image-map-provider-settings.component';
import {
  DatasourcesKeyAutocompleteComponent
} from '@home/components/widget/lib/settings/map/datasources-key-autocomplete.component';
import { CommonMapSettingsComponent } from '@home/components/widget/lib/settings/map/common-map-settings.component';
import { MarkersSettingsComponent } from '@home/components/widget/lib/settings/map/markers-settings.component';
import { PolygonSettingsComponent } from '@home/components/widget/lib/settings/map/polygon-settings.component';
import { CircleSettingsComponent } from '@home/components/widget/lib/settings/map/circle-settings.component';
import {
  MarkerClusteringSettingsComponent
} from '@home/components/widget/lib/settings/map/marker-clustering-settings.component';
import { MapEditorSettingsComponent } from '@home/components/widget/lib/settings/map/map-editor-settings.component';
import { RouteMapSettingsComponent } from '@home/components/widget/lib/settings/map/route-map-settings.component';
import {
  RouteMapWidgetSettingsComponent
} from '@home/components/widget/lib/settings/map/route-map-widget-settings.component';
import {
  TripAnimationWidgetSettingsComponent
} from '@home/components/widget/lib/settings/map/trip-animation-widget-settings.component';
import {
  TripAnimationCommonSettingsComponent
} from '@home/components/widget/lib/settings/map/trip-animation-common-settings.component';
import {
  TripAnimationMarkerSettingsComponent
} from '@home/components/widget/lib/settings/map/trip-animation-marker-settings.component';
import {
  TripAnimationPathSettingsComponent
} from '@home/components/widget/lib/settings/map/trip-animation-path-settings.component';
import {
  TripAnimationPointSettingsComponent
} from '@home/components/widget/lib/settings/map/trip-animation-point-settings.component';
import {
  GatewayLogsSettingsComponent
} from '@home/components/widget/lib/settings/gateway/gateway-logs-settings.component';
import {
  GatewayServiceRPCSettingsComponent
} from '@home/components/widget/lib/settings/gateway/gateway-service-rpc-settings.component';
import {
  DocLinksWidgetSettingsComponent
} from '@home/components/widget/lib/settings/home-page/doc-links-widget-settings.component';
import {
  QuickLinksWidgetSettingsComponent
} from '@home/components/widget/lib/settings/home-page/quick-links-widget-settings.component';
import {
  ValueCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/value-card-widget-settings.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import {
  AggregatedValueCardKeySettingsComponent
} from '@home/components/widget/lib/settings/cards/aggregated-value-card-key-settings.component';
import {
  AggregatedValueCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/aggregated-value-card-widget-settings.component';
import {
  AlarmCountWidgetSettingsComponent
} from '@home/components/widget/lib/settings/alarm/alarm-count-widget-settings.component';
import {
  EntityCountWidgetSettingsComponent
} from '@home/components/widget/lib/settings/entity/entity-count-widget-settings.component';
import {
  BatteryLevelWidgetSettingsComponent
} from '@home/components/widget/lib/settings/indicator/battery-level-widget-settings.component';
import {
  WindSpeedDirectionWidgetSettingsComponent
} from '@home/components/widget/lib/settings/weather/wind-speed-direction-widget-settings.component';
import {
  SignalStrengthWidgetSettingsComponent
} from '@home/components/widget/lib/settings/indicator/signal-strength-widget-settings.component';
import {
  ValueChartCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/value-chart-card-widget-settings.component';
import {
  ProgressBarWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/progress-bar-widget-settings.component';
import {
  LiquidLevelCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/indicator/liquid-level-card-widget-settings.component';
import {
  DoughnutWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/doughnut-widget-settings.component';
import {
  RangeChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/range-chart-widget-settings.component';
import {
  BarChartWithLabelsWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/bar-chart-with-labels-widget-settings.component';
import {
  SingleSwitchWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/single-switch-widget-settings.component';
import {
  ActionButtonWidgetSettingsComponent
} from '@home/components/widget/lib/settings/button/action-button-widget-settings.component';
import {
  CommandButtonWidgetSettingsComponent
} from '@home/components/widget/lib/settings/button/command-button-widget-settings.component';
import {
  PowerButtonWidgetSettingsComponent
} from '@home/components/widget/lib/settings/button/power-button-widget-settings.component';
import {
  SliderWidgetSettingsComponent
} from '@home/components/widget/lib/settings/control/slider-widget-settings.component';
import {
  ToggleButtonWidgetSettingsComponent
} from '@home/components/widget/lib/settings/button/toggle-button-widget-settings.component';
import {
  TimeSeriesChartKeySettingsComponent
} from '@home/components/widget/lib/settings/chart/time-series-chart-key-settings.component';
import {
  TimeSeriesChartLineSettingsComponent
} from '@home/components/widget/lib/settings/chart/time-series-chart-line-settings.component';
import {
  TimeSeriesChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/time-series-chart-widget-settings.component';
import {
  StatusWidgetSettingsComponent
} from '@home/components/widget/lib/settings/indicator/status-widget-settings.component';
import {
  PieChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/pie-chart-widget-settings.component';
import {
  BarChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/bar-chart-widget-settings.component';
import {
  PolarAreaChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/polar-area-chart-widget-settings.component';
import {
  RadarChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/radar-chart-widget-settings.component';
import {
  MobileAppQrCodeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/mobile-app-qr-code-widget-settings.component';
import {
  LabelCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/label-card-widget-settings.component';
import {
  LabelValueCardWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/label-value-card-widget-settings.component';
import {
  UnreadNotificationWidgetSettingsComponent
} from '@home/components/widget/lib/settings/cards/unread-notification-widget-settings.component';
import {
ScadaSymbolWidgetSettingsComponent
} from '@home/components/widget/lib/settings/scada/scada-symbol-widget-settings.component';

@NgModule({
  declarations: [
    QrCodeWidgetSettingsComponent,
    MobileAppQrCodeWidgetSettingsComponent,
    TimeseriesTableWidgetSettingsComponent,
    TimeseriesTableKeySettingsComponent,
    TimeseriesTableLatestKeySettingsComponent,
    MarkdownWidgetSettingsComponent,
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
    AnalogueRadialGaugeWidgetSettingsComponent,
    AnalogueLinearGaugeWidgetSettingsComponent,
    AnalogueCompassWidgetSettingsComponent,
    DigitalGaugeWidgetSettingsComponent,
    TickValueComponent,
    FlotWidgetSettingsComponent,
    LabelDataKeyComponent,
    FlotLineWidgetSettingsComponent,
    FlotBarWidgetSettingsComponent,
    FlotThresholdComponent,
    FlotKeySettingsComponent,
    FlotLineKeySettingsComponent,
    FlotBarKeySettingsComponent,
    FlotLatestKeySettingsComponent,
    FlotPieWidgetSettingsComponent,
    FlotPieKeySettingsComponent,
    ChartWidgetSettingsComponent,
    DoughnutChartWidgetSettingsComponent,
    SwitchRpcSettingsComponent,
    RoundSwitchWidgetSettingsComponent,
    SwitchControlWidgetSettingsComponent,
    SlideToggleWidgetSettingsComponent,
    PersistentTableWidgetSettingsComponent,
    RpcButtonStyleComponent,
    UpdateDeviceAttributeWidgetSettingsComponent,
    SendRpcWidgetSettingsComponent,
    LedIndicatorWidgetSettingsComponent,
    KnobControlWidgetSettingsComponent,
    RpcTerminalWidgetSettingsComponent,
    RpcShellWidgetSettingsComponent,
    DateRangeNavigatorWidgetSettingsComponent,
    EdgeQuickOverviewWidgetSettingsComponent,
    GatewayConfigWidgetSettingsComponent,
    GatewayConfigSingleDeviceWidgetSettingsComponent,
    GatewayEventsWidgetSettingsComponent,
    GpioItemComponent,
    GpioControlWidgetSettingsComponent,
    GpioPanelWidgetSettingsComponent,
    NavigationCardWidgetSettingsComponent,
    NavigationCardsWidgetSettingsComponent,
    DeviceClaimingWidgetSettingsComponent,
    UpdateAttributeGeneralSettingsComponent,
    UpdateIntegerAttributeWidgetSettingsComponent,
    UpdateDoubleAttributeWidgetSettingsComponent,
    UpdateStringAttributeWidgetSettingsComponent,
    UpdateBooleanAttributeWidgetSettingsComponent,
    UpdateImageAttributeWidgetSettingsComponent,
    UpdateDateAttributeWidgetSettingsComponent,
    UpdateLocationAttributeWidgetSettingsComponent,
    UpdateJsonAttributeWidgetSettingsComponent,
    PhotoCameraInputWidgetSettingsComponent,
    UpdateMultipleAttributesWidgetSettingsComponent,
    DataKeySelectOptionComponent,
    UpdateMultipleAttributesKeySettingsComponent,
    GoogleMapProviderSettingsComponent,
    OpenStreetMapProviderSettingsComponent,
    HereMapProviderSettingsComponent,
    ImageMapProviderSettingsComponent,
    TencentMapProviderSettingsComponent,
    MapProviderSettingsComponent,
    DatasourcesKeyAutocompleteComponent,
    CommonMapSettingsComponent,
    MarkersSettingsComponent,
    PolygonSettingsComponent,
    CircleSettingsComponent,
    MarkerClusteringSettingsComponent,
    MapEditorSettingsComponent,
    RouteMapSettingsComponent,
    MapSettingsComponent,
    TripAnimationCommonSettingsComponent,
    TripAnimationMarkerSettingsComponent,
    TripAnimationPathSettingsComponent,
    TripAnimationPointSettingsComponent,
    MapWidgetSettingsComponent,
    RouteMapWidgetSettingsComponent,
    GatewayLogsSettingsComponent,
    GatewayServiceRPCSettingsComponent,
    TripAnimationWidgetSettingsComponent,
    DocLinksWidgetSettingsComponent,
    QuickLinksWidgetSettingsComponent,
    ValueCardWidgetSettingsComponent,
    AggregatedValueCardKeySettingsComponent,
    AggregatedValueCardWidgetSettingsComponent,
    AlarmCountWidgetSettingsComponent,
    EntityCountWidgetSettingsComponent,
    BatteryLevelWidgetSettingsComponent,
    WindSpeedDirectionWidgetSettingsComponent,
    SignalStrengthWidgetSettingsComponent,
    ValueChartCardWidgetSettingsComponent,
    ProgressBarWidgetSettingsComponent,
    LiquidLevelCardWidgetSettingsComponent,
    DoughnutWidgetSettingsComponent,
    RangeChartWidgetSettingsComponent,
    BarChartWithLabelsWidgetSettingsComponent,
    SingleSwitchWidgetSettingsComponent,
    ActionButtonWidgetSettingsComponent,
    CommandButtonWidgetSettingsComponent,
    PowerButtonWidgetSettingsComponent,
    SliderWidgetSettingsComponent,
    ToggleButtonWidgetSettingsComponent,
    TimeSeriesChartKeySettingsComponent,
    TimeSeriesChartLineSettingsComponent,
    TimeSeriesChartWidgetSettingsComponent,
    StatusWidgetSettingsComponent,
    PieChartWidgetSettingsComponent,
    BarChartWidgetSettingsComponent,
    PolarAreaChartWidgetSettingsComponent,
    RadarChartWidgetSettingsComponent,
    LabelCardWidgetSettingsComponent,
    LabelValueCardWidgetSettingsComponent,
    UnreadNotificationWidgetSettingsComponent,
    ScadaSymbolWidgetSettingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    WidgetSettingsCommonModule
  ],
  exports: [
    QrCodeWidgetSettingsComponent,
    MobileAppQrCodeWidgetSettingsComponent,
    TimeseriesTableWidgetSettingsComponent,
    TimeseriesTableKeySettingsComponent,
    TimeseriesTableLatestKeySettingsComponent,
    MarkdownWidgetSettingsComponent,
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
    AnalogueRadialGaugeWidgetSettingsComponent,
    AnalogueLinearGaugeWidgetSettingsComponent,
    AnalogueCompassWidgetSettingsComponent,
    DigitalGaugeWidgetSettingsComponent,
    TickValueComponent,
    FlotWidgetSettingsComponent,
    LabelDataKeyComponent,
    FlotLineWidgetSettingsComponent,
    FlotBarWidgetSettingsComponent,
    FlotThresholdComponent,
    FlotKeySettingsComponent,
    FlotLineKeySettingsComponent,
    FlotBarKeySettingsComponent,
    FlotLatestKeySettingsComponent,
    FlotPieWidgetSettingsComponent,
    FlotPieKeySettingsComponent,
    ChartWidgetSettingsComponent,
    DoughnutChartWidgetSettingsComponent,
    SwitchRpcSettingsComponent,
    RoundSwitchWidgetSettingsComponent,
    SwitchControlWidgetSettingsComponent,
    SlideToggleWidgetSettingsComponent,
    PersistentTableWidgetSettingsComponent,
    RpcButtonStyleComponent,
    UpdateDeviceAttributeWidgetSettingsComponent,
    SendRpcWidgetSettingsComponent,
    LedIndicatorWidgetSettingsComponent,
    KnobControlWidgetSettingsComponent,
    RpcTerminalWidgetSettingsComponent,
    RpcShellWidgetSettingsComponent,
    DateRangeNavigatorWidgetSettingsComponent,
    EdgeQuickOverviewWidgetSettingsComponent,
    GatewayConfigWidgetSettingsComponent,
    GatewayConfigSingleDeviceWidgetSettingsComponent,
    GatewayEventsWidgetSettingsComponent,
    GpioItemComponent,
    GpioControlWidgetSettingsComponent,
    GpioPanelWidgetSettingsComponent,
    NavigationCardWidgetSettingsComponent,
    NavigationCardsWidgetSettingsComponent,
    DeviceClaimingWidgetSettingsComponent,
    UpdateAttributeGeneralSettingsComponent,
    UpdateIntegerAttributeWidgetSettingsComponent,
    UpdateDoubleAttributeWidgetSettingsComponent,
    UpdateStringAttributeWidgetSettingsComponent,
    UpdateBooleanAttributeWidgetSettingsComponent,
    UpdateImageAttributeWidgetSettingsComponent,
    UpdateDateAttributeWidgetSettingsComponent,
    UpdateLocationAttributeWidgetSettingsComponent,
    UpdateJsonAttributeWidgetSettingsComponent,
    PhotoCameraInputWidgetSettingsComponent,
    UpdateMultipleAttributesWidgetSettingsComponent,
    DataKeySelectOptionComponent,
    UpdateMultipleAttributesKeySettingsComponent,
    GoogleMapProviderSettingsComponent,
    OpenStreetMapProviderSettingsComponent,
    HereMapProviderSettingsComponent,
    ImageMapProviderSettingsComponent,
    TencentMapProviderSettingsComponent,
    MapProviderSettingsComponent,
    DatasourcesKeyAutocompleteComponent,
    CommonMapSettingsComponent,
    MarkersSettingsComponent,
    PolygonSettingsComponent,
    CircleSettingsComponent,
    MarkerClusteringSettingsComponent,
    MapEditorSettingsComponent,
    RouteMapSettingsComponent,
    MapSettingsComponent,
    TripAnimationCommonSettingsComponent,
    TripAnimationMarkerSettingsComponent,
    TripAnimationPathSettingsComponent,
    TripAnimationPointSettingsComponent,
    MapWidgetSettingsComponent,
    RouteMapWidgetSettingsComponent,
    GatewayLogsSettingsComponent,
    GatewayServiceRPCSettingsComponent,
    TripAnimationWidgetSettingsComponent,
    DocLinksWidgetSettingsComponent,
    QuickLinksWidgetSettingsComponent,
    ValueCardWidgetSettingsComponent,
    AggregatedValueCardKeySettingsComponent,
    AggregatedValueCardWidgetSettingsComponent,
    AlarmCountWidgetSettingsComponent,
    EntityCountWidgetSettingsComponent,
    BatteryLevelWidgetSettingsComponent,
    WindSpeedDirectionWidgetSettingsComponent,
    SignalStrengthWidgetSettingsComponent,
    ValueChartCardWidgetSettingsComponent,
    ProgressBarWidgetSettingsComponent,
    LiquidLevelCardWidgetSettingsComponent,
    DoughnutWidgetSettingsComponent,
    RangeChartWidgetSettingsComponent,
    BarChartWithLabelsWidgetSettingsComponent,
    SingleSwitchWidgetSettingsComponent,
    ActionButtonWidgetSettingsComponent,
    CommandButtonWidgetSettingsComponent,
    PowerButtonWidgetSettingsComponent,
    SliderWidgetSettingsComponent,
    ToggleButtonWidgetSettingsComponent,
    TimeSeriesChartKeySettingsComponent,
    TimeSeriesChartLineSettingsComponent,
    TimeSeriesChartWidgetSettingsComponent,
    StatusWidgetSettingsComponent,
    PieChartWidgetSettingsComponent,
    BarChartWidgetSettingsComponent,
    PolarAreaChartWidgetSettingsComponent,
    RadarChartWidgetSettingsComponent,
    LabelCardWidgetSettingsComponent,
    LabelValueCardWidgetSettingsComponent,
    UnreadNotificationWidgetSettingsComponent,
    ScadaSymbolWidgetSettingsComponent
  ]
})
export class WidgetSettingsModule {
}

export const widgetSettingsComponentsMap: {[key: string]: Type<IWidgetSettingsComponent>} = {
  'tb-qrcode-widget-settings': QrCodeWidgetSettingsComponent,
  'tb-mobile-app-qr-code-widget-settings': MobileAppQrCodeWidgetSettingsComponent,
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
  'tb-flot-latest-key-settings': FlotLatestKeySettingsComponent,
  'tb-flot-pie-widget-settings': FlotPieWidgetSettingsComponent,
  'tb-flot-pie-key-settings': FlotPieKeySettingsComponent,
  'tb-chart-widget-settings': ChartWidgetSettingsComponent,
  'tb-doughnut-chart-widget-settings': DoughnutChartWidgetSettingsComponent,
  'tb-round-switch-widget-settings': RoundSwitchWidgetSettingsComponent,
  'tb-switch-control-widget-settings': SwitchControlWidgetSettingsComponent,
  'tb-slide-toggle-widget-settings': SlideToggleWidgetSettingsComponent,
  'tb-persistent-table-widget-settings': PersistentTableWidgetSettingsComponent,
  'tb-update-device-attribute-widget-settings': UpdateDeviceAttributeWidgetSettingsComponent,
  'tb-send-rpc-widget-settings': SendRpcWidgetSettingsComponent,
  'tb-led-indicator-widget-settings': LedIndicatorWidgetSettingsComponent,
  'tb-knob-control-widget-settings': KnobControlWidgetSettingsComponent,
  'tb-rpc-terminal-widget-settings': RpcTerminalWidgetSettingsComponent,
  'tb-rpc-shell-widget-settings': RpcShellWidgetSettingsComponent,
  'tb-date-range-navigator-widget-settings': DateRangeNavigatorWidgetSettingsComponent,
  'tb-edge-quick-overview-widget-settings': EdgeQuickOverviewWidgetSettingsComponent,
  'tb-gateway-config-widget-settings': GatewayConfigWidgetSettingsComponent,
  'tb-gateway-config-single-device-widget-settings': GatewayConfigSingleDeviceWidgetSettingsComponent,
  'tb-gateway-events-widget-settings': GatewayEventsWidgetSettingsComponent,
  'tb-gpio-control-widget-settings': GpioControlWidgetSettingsComponent,
  'tb-gpio-panel-widget-settings': GpioPanelWidgetSettingsComponent,
  'tb-navigation-card-widget-settings': NavigationCardWidgetSettingsComponent,
  'tb-navigation-cards-widget-settings': NavigationCardsWidgetSettingsComponent,
  'tb-device-claiming-widget-settings': DeviceClaimingWidgetSettingsComponent,
  'tb-update-integer-attribute-widget-settings': UpdateIntegerAttributeWidgetSettingsComponent,
  'tb-update-double-attribute-widget-settings': UpdateDoubleAttributeWidgetSettingsComponent,
  'tb-update-string-attribute-widget-settings': UpdateStringAttributeWidgetSettingsComponent,
  'tb-update-boolean-attribute-widget-settings': UpdateBooleanAttributeWidgetSettingsComponent,
  'tb-update-image-attribute-widget-settings': UpdateImageAttributeWidgetSettingsComponent,
  'tb-update-date-attribute-widget-settings': UpdateDateAttributeWidgetSettingsComponent,
  'tb-update-location-attribute-widget-settings': UpdateLocationAttributeWidgetSettingsComponent,
  'tb-update-json-attribute-widget-settings': UpdateJsonAttributeWidgetSettingsComponent,
  'tb-photo-camera-input-widget-settings': PhotoCameraInputWidgetSettingsComponent,
  'tb-update-multiple-attributes-widget-settings': UpdateMultipleAttributesWidgetSettingsComponent,
  'tb-update-multiple-attributes-key-settings': UpdateMultipleAttributesKeySettingsComponent,
  'tb-map-widget-settings': MapWidgetSettingsComponent,
  'tb-route-map-widget-settings': RouteMapWidgetSettingsComponent,
  'tb-trip-animation-widget-settings': TripAnimationWidgetSettingsComponent,
  'tb-gateway-logs-settings': GatewayLogsSettingsComponent,
  'tb-gateway-service-rpc-settings':GatewayServiceRPCSettingsComponent,
  'tb-doc-links-widget-settings': DocLinksWidgetSettingsComponent,
  'tb-quick-links-widget-settings': QuickLinksWidgetSettingsComponent,
  'tb-value-card-widget-settings': ValueCardWidgetSettingsComponent,
  'tb-aggregated-value-card-key-settings': AggregatedValueCardKeySettingsComponent,
  'tb-aggregated-value-card-widget-settings': AggregatedValueCardWidgetSettingsComponent,
  'tb-alarm-count-widget-settings': AlarmCountWidgetSettingsComponent,
  'tb-entity-count-widget-settings': EntityCountWidgetSettingsComponent,
  'tb-battery-level-widget-settings': BatteryLevelWidgetSettingsComponent,
  'tb-wind-speed-direction-widget-settings': WindSpeedDirectionWidgetSettingsComponent,
  'tb-signal-strength-widget-settings': SignalStrengthWidgetSettingsComponent,
  'tb-value-chart-card-widget-settings': ValueChartCardWidgetSettingsComponent,
  'tb-progress-bar-widget-settings': ProgressBarWidgetSettingsComponent,
  'tb-liquid-level-card-widget-settings': LiquidLevelCardWidgetSettingsComponent,
  'tb-doughnut-widget-settings': DoughnutWidgetSettingsComponent,
  'tb-range-chart-widget-settings': RangeChartWidgetSettingsComponent,
  'tb-bar-chart-with-labels-widget-settings': BarChartWithLabelsWidgetSettingsComponent,
  'tb-single-switch-widget-settings': SingleSwitchWidgetSettingsComponent,
  'tb-action-button-widget-settings': ActionButtonWidgetSettingsComponent,
  'tb-command-button-widget-settings': CommandButtonWidgetSettingsComponent,
  'tb-power-button-widget-settings': PowerButtonWidgetSettingsComponent,
  'tb-slider-widget-settings': SliderWidgetSettingsComponent,
  'tb-toggle-button-widget-settings': ToggleButtonWidgetSettingsComponent,
  'tb-time-series-chart-key-settings': TimeSeriesChartKeySettingsComponent,
  'tb-time-series-chart-widget-settings': TimeSeriesChartWidgetSettingsComponent,
  'tb-status-widget-settings': StatusWidgetSettingsComponent,
  'tb-pie-chart-widget-settings': PieChartWidgetSettingsComponent,
  'tb-bar-chart-widget-settings': BarChartWidgetSettingsComponent,
  'tb-polar-area-chart-widget-settings': PolarAreaChartWidgetSettingsComponent,
  'tb-radar-chart-widget-settings': RadarChartWidgetSettingsComponent,
  'tb-label-card-widget-settings': LabelCardWidgetSettingsComponent,
  'tb-label-value-card-widget-settings': LabelValueCardWidgetSettingsComponent,
  'tb-unread-notification-widget-settings': UnreadNotificationWidgetSettingsComponent,
  'tb-scada-symbol-widget-settings': ScadaSymbolWidgetSettingsComponent
};
