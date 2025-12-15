///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  ColorRangeSettingsComponent,
  ColorRangeSettingsComponentService
} from '@home/components/widget/lib/settings/common/color-range-settings.component';
import {
  GetValueActionSettingsComponent
} from '@home/components/widget/lib/settings/common/action/get-value-action-settings.component';
import {
  GetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/get-value-action-settings-panel.component';
import {
  DeviceKeyAutocompleteComponent
} from '@home/components/widget/lib/settings/control/device-key-autocomplete.component';
import {
  SetValueActionSettingsComponent
} from '@home/components/widget/lib/settings/common/action/set-value-action-settings.component';
import {
  SetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/set-value-action-settings-panel.component';
import { CssSizeInputComponent } from '@home/components/widget/lib/settings/common/css-size-input.component';
import { WidgetActionComponent } from '@home/components/widget/lib/settings/common/action/widget-action.component';
import {
  MapItemTooltipsComponent
} from '@home/components/widget/lib/settings/common/action/map-item-tooltips.component';
import {
  CustomActionPrettyResourcesTabsComponent
} from '@home/components/widget/lib/settings/common/action/custom-action-pretty-resources-tabs.component';
import {
  CustomActionPrettyEditorComponent
} from '@home/components/widget/lib/settings/common/action/custom-action-pretty-editor.component';
import {
  MobileActionEditorComponent
} from '@home/components/widget/lib/settings/common/action/mobile-action-editor.component';
import {
  WidgetActionSettingsComponent
} from '@home/components/widget/lib/settings/common/action/widget-action-settings.component';
import {
  WidgetActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/widget-action-settings-panel.component';
import {
  WidgetButtonAppearanceComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-appearance.component';
import {
  WidgetButtonCustomStyleComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-custom-style.component';
import {
  WidgetButtonCustomStylePanelComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-custom-style-panel.component';
import {
  TimeSeriesChartAxisSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings.component';
import {
  TimeSeriesChartThresholdsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-thresholds-panel.component';
import {
  TimeSeriesChartThresholdRowComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-row.component';
import { DataKeyInputComponent } from '@home/components/widget/lib/settings/common/key/data-key-input.component';
import { EntityAliasInputComponent } from '@home/components/widget/lib/settings/common/entity-alias-input.component';
import {
  TimeSeriesChartThresholdSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings-panel.component';
import {
  TimeSeriesNoAggregationBarWidthSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-no-aggregation-bar-width-settings.component';
import {
  TimeSeriesChartYAxesPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-y-axes-panel.component';
import {
  TimeSeriesChartYAxisRowComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-y-axis-row.component';
import {
  TimeSeriesChartAxisSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings-panel.component';
import {
  ChartAnimationSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/chart-animation-settings.component';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';
import {
  AutoDateFormatSettingsComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings.component';
import {
  ChartFillSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/chart-fill-settings.component';
import {
  TimeSeriesChartThresholdSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings.component';
import {
  TimeSeriesChartStateRowComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-state-row.component';
import {
  TimeSeriesChartStatesPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-states-panel.component';
import {
  TimeSeriesChartAxisSettingsButtonComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings-button.component';
import {
  TimeSeriesChartGridSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-grid-settings.component';
import {
  StatusWidgetStateSettingsComponent
} from '@home/components/widget/lib/settings/common/indicator/status-widget-state-settings.component';
import { ChartBarSettingsComponent } from '@home/components/widget/lib/settings/common/chart/chart-bar-settings.component';
import { AdvancedRangeComponent } from '@home/components/widget/lib/settings/common/advanced-range.component';
import { GradientComponent } from '@home/components/widget/lib/settings/common/gradient.component';
import {
  ValueSourceDataKeyComponent
} from '@home/components/widget/lib/settings/common/value-source-data-key.component';
import {
  ScadaSymbolObjectSettingsComponent
} from '@home/components/widget/lib/settings/common/scada/scada-symbol-object-settings.component';
import {
  WidgetButtonToggleCustomStyleComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-toggle-custom-style.component';
import {
  WidgetButtonToggleCustomStylePanelComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-toggle-custom-style-panel.component';
import {
  DynamicFormPropertiesComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-properties.component';
import {
  DynamicFormPropertyRowComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-property-row.component';
import {
  DynamicFormPropertyPanelComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-property-panel.component';
import { DynamicFormComponent } from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form.component';
import {
  DynamicFormSelectItemsComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-select-items.component';
import {
  DynamicFormSelectItemRowComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-select-item-row.component';
import {
  DynamicFormArrayComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-array.component';
import { MapSettingsComponent } from '@home/components/widget/lib/settings/common/map/map-settings.component';
import { MapLayersComponent } from '@home/components/widget/lib/settings/common/map/map-layers.component';
import { MapLayerRowComponent } from '@home/components/widget/lib/settings/common/map/map-layer-row.component';
import {
  MapLayerSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/map-layer-settings-panel.component';
import { MapDataLayersComponent } from '@home/components/widget/lib/settings/common/map/map-data-layers.component';
import { MapDataLayerRowComponent } from '@home/components/widget/lib/settings/common/map/map-data-layer-row.component';
import {
  EntityAliasSelectComponent
} from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component';
import {
  MapDataLayerDialogComponent
} from '@home/components/widget/lib/settings/common/map/map-data-layer-dialog.component';
import { FilterSelectComponent } from '@home/components/widget/lib/settings/common/filter/filter-select.component';
import { DataKeysComponent } from '@home/components/widget/lib/settings/common/key/data-keys.component';
import {
  DataKeyConfigDialogComponent
} from '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component';
import { DataKeyConfigComponent } from '@home/components/widget/lib/settings/common/key/data-key-config.component';
import { WidgetSettingsComponent } from '@home/components/widget/lib/settings/common/widget/widget-settings.component';
import {
  DataLayerColorSettingsComponent
} from '@home/components/widget/lib/settings/common/map/data-layer-color-settings.component';
import {
  DataLayerColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/data-layer-color-settings-panel.component';
import {
  MarkerImageSettingsComponent
} from '@home/components/widget/lib/settings/common/map/marker-image-settings.component';
import {
  MarkerImageSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/marker-image-settings-panel.component';
import {
  DataLayerPatternSettingsComponent
} from '@home/components/widget/lib/settings/common/map/data-layer-pattern-settings.component';
import {
  MarkerShapeSettingsComponent
} from '@home/components/widget/lib/settings/common/map/marker-shape-settings.component';
import { MarkerShapesComponent } from '@home/components/widget/lib/settings/common/map/marker-shapes.component';
import {
  MarkerClusteringSettingsComponent
} from '@home/components/widget/lib/settings/common/map/marker-clustering-settings.component';
import { AdditionalMapDataSourcesComponent } from '@home/components/widget/lib/settings/common/map/additional-map-data-sources.component';
import {
  AdditionalMapDataSourceRowComponent
} from '@home/components/widget/lib/settings/common/map/additional-map-data-source-row.component';
import {
  ImageMapSourceSettingsComponent
} from '@home/components/widget/lib/settings/common/map/image-map-source-settings.component';
import {
  MapTooltipTagActionsComponent
} from '@home/components/widget/lib/settings/common/map/map-tooltip-tag-actions.component';
import {
  MapActionButtonsSettingsComponent
} from '@home/components/widget/lib/settings/common/map/map-action-buttons-settings.component';
import {
  MapActionButtonRowComponent
} from '@home/components/widget/lib/settings/common/map/map-action-button-row.component';
import {
  TripTimelineSettingsComponent
} from '@home/components/widget/lib/settings/common/map/trip-timeline-settings.component';
import {
  MarkerIconShapesComponent
} from '@home/components/widget/lib/settings/common/map/marker-icon-shapes.component';
import { MapDataSourcesComponent } from '@home/components/widget/lib/settings/common/map/map-data-sources.component';
import {
  MapDataSourceRowComponent
} from '@home/components/widget/lib/settings/common/map/map-data-source-row.component';
import {
  ShapeFillImageSettingsComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-image-settings.component';
import {
  ShapeFillImageSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-image-settings-panel.component';
import {
  ShapeFillStripeSettingsComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-stripe-settings.component';
import {
  ShapeFillStripeSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-stripe-settings-panel.component';
import { AxisScaleRowComponent } from './axis-scale-row.component';

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
    AutoDateFormatSettingsComponent,
    AutoDateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    ValueSourceDataKeyComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent,
    ColorRangeListComponent,
    ColorRangePanelComponent,
    ColorRangeSettingsComponent,
    GetValueActionSettingsComponent,
    GetValueActionSettingsPanelComponent,
    DeviceKeyAutocompleteComponent,
    SetValueActionSettingsComponent,
    SetValueActionSettingsPanelComponent,
    WidgetActionComponent,
    MapItemTooltipsComponent,
    CustomActionPrettyResourcesTabsComponent,
    CustomActionPrettyEditorComponent,
    MobileActionEditorComponent,
    WidgetActionSettingsComponent,
    WidgetActionSettingsPanelComponent,
    WidgetButtonAppearanceComponent,
    WidgetButtonCustomStyleComponent,
    WidgetButtonToggleCustomStyleComponent,
    WidgetButtonCustomStylePanelComponent,
    WidgetButtonToggleCustomStylePanelComponent,
    TimeSeriesChartAxisSettingsComponent,
    TimeSeriesChartThresholdsPanelComponent,
    TimeSeriesChartThresholdRowComponent,
    TimeSeriesChartThresholdSettingsPanelComponent,
    TimeSeriesNoAggregationBarWidthSettingsComponent,
    TimeSeriesChartYAxesPanelComponent,
    TimeSeriesChartYAxisRowComponent,
    TimeSeriesChartAxisSettingsPanelComponent,
    TimeSeriesChartAxisSettingsButtonComponent,
    ChartAnimationSettingsComponent,
    ChartFillSettingsComponent,
    ChartBarSettingsComponent,
    TimeSeriesChartThresholdSettingsComponent,
    TimeSeriesChartStatesPanelComponent,
    TimeSeriesChartStateRowComponent,
    TimeSeriesChartGridSettingsComponent,
    StatusWidgetStateSettingsComponent,
    ScadaSymbolObjectSettingsComponent,
    DataKeyInputComponent,
    EntityAliasInputComponent,
    AdvancedRangeComponent,
    GradientComponent,
    DynamicFormPropertiesComponent,
    DynamicFormPropertyRowComponent,
    DynamicFormPropertyPanelComponent,
    DynamicFormSelectItemsComponent,
    DynamicFormSelectItemRowComponent,
    DynamicFormComponent,
    DynamicFormArrayComponent,
    MapLayerSettingsPanelComponent,
    MapLayerRowComponent,
    MapLayersComponent,
    ImageMapSourceSettingsComponent,
    DataLayerColorSettingsComponent,
    DataLayerColorSettingsPanelComponent,
    MapTooltipTagActionsComponent,
    MapActionButtonsSettingsComponent,
    MapActionButtonRowComponent,
    DataLayerPatternSettingsComponent,
    MarkerShapeSettingsComponent,
    MarkerShapesComponent,
    MarkerIconShapesComponent,
    MarkerImageSettingsComponent,
    MarkerImageSettingsPanelComponent,
    MarkerClusteringSettingsComponent,
    ShapeFillStripeSettingsComponent,
    ShapeFillStripeSettingsPanelComponent,
    ShapeFillImageSettingsComponent,
    ShapeFillImageSettingsPanelComponent,
    MapDataLayerDialogComponent,
    MapDataLayerRowComponent,
    MapDataLayersComponent,
    MapDataSourceRowComponent,
    MapDataSourcesComponent,
    AdditionalMapDataSourceRowComponent,
    AdditionalMapDataSourcesComponent,
    TripTimelineSettingsComponent,
    MapSettingsComponent,
    EntityAliasSelectComponent,
    FilterSelectComponent,
    DataKeysComponent,
    DataKeyConfigDialogComponent,
    DataKeyConfigComponent,
    WidgetSettingsComponent,
    AxisScaleRowComponent
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
    AutoDateFormatSettingsComponent,
    AutoDateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    ValueSourceDataKeyComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent,
    ColorRangeListComponent,
    ColorRangePanelComponent,
    ColorRangeSettingsComponent,
    GetValueActionSettingsComponent,
    GetValueActionSettingsPanelComponent,
    DeviceKeyAutocompleteComponent,
    SetValueActionSettingsComponent,
    SetValueActionSettingsPanelComponent,
    WidgetActionComponent,
    CustomActionPrettyResourcesTabsComponent,
    CustomActionPrettyEditorComponent,
    MobileActionEditorComponent,
    WidgetActionSettingsComponent,
    WidgetActionSettingsPanelComponent,
    WidgetButtonAppearanceComponent,
    WidgetButtonCustomStyleComponent,
    WidgetButtonToggleCustomStyleComponent,
    WidgetButtonCustomStylePanelComponent,
    WidgetButtonToggleCustomStylePanelComponent,
    TimeSeriesChartAxisSettingsComponent,
    TimeSeriesChartThresholdsPanelComponent,
    TimeSeriesChartThresholdRowComponent,
    TimeSeriesChartThresholdSettingsPanelComponent,
    TimeSeriesNoAggregationBarWidthSettingsComponent,
    TimeSeriesChartYAxesPanelComponent,
    TimeSeriesChartYAxisRowComponent,
    TimeSeriesChartAxisSettingsPanelComponent,
    TimeSeriesChartAxisSettingsButtonComponent,
    ChartAnimationSettingsComponent,
    ChartFillSettingsComponent,
    ChartBarSettingsComponent,
    TimeSeriesChartThresholdSettingsComponent,
    TimeSeriesChartStatesPanelComponent,
    TimeSeriesChartStateRowComponent,
    TimeSeriesChartGridSettingsComponent,
    StatusWidgetStateSettingsComponent,
    ScadaSymbolObjectSettingsComponent,
    DataKeyInputComponent,
    EntityAliasInputComponent,
    AdvancedRangeComponent,
    GradientComponent,
    DynamicFormPropertiesComponent,
    DynamicFormPropertyRowComponent,
    DynamicFormPropertyPanelComponent,
    DynamicFormSelectItemsComponent,
    DynamicFormSelectItemRowComponent,
    DynamicFormComponent,
    DynamicFormArrayComponent,
    MapSettingsComponent,
    EntityAliasSelectComponent,
    FilterSelectComponent,
    DataKeysComponent,
    DataKeyConfigDialogComponent,
    DataKeyConfigComponent,
    WidgetSettingsComponent,
    AxisScaleRowComponent
  ],
  providers: [
    ColorSettingsComponentService,
    ColorRangeSettingsComponentService
  ]
})
export class WidgetSettingsCommonModule {
}
