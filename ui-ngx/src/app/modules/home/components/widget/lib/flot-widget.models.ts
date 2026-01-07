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

// eslint-disable-next-line @typescript-eslint/triple-slash-reference
/// <reference path="../../../../../../../src/typings/jquery.flot.typings.d.ts" />

import {
  DataKey, DataKeySettingsWithComparison,
  Datasource,
  DatasourceData,
  FormattedData,
  LegendConfig
} from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { ComparisonDuration } from '@shared/models/time/time.models';

export declare type ChartType = 'line' | 'pie' | 'bar' | 'state' | 'graph';

export declare type TbFlotSettings = TbFlotBaseSettings & TbFlotLegendSettings &
  TbFlotGraphSettings & TbFlotBarSettings & TbFlotPieSettings;

export declare type TooltipValueFormatFunction = (value: any, latestData: FormattedData) => string;

export declare type TbFlotTicksFormatterFunction = (t: number, a?: TbFlotPlotAxis) => string;

export interface TbFlotSeries extends DatasourceData, JQueryPlotSeriesOptions {
  dataKey: TbFlotDataKey;
  xaxisIndex?: number;
  yaxisIndex?: number;
  yaxis?: number;
}

export interface TbFlotDataKey extends DataKey {
  settings?: TbFlotKeySettings;
  tooltipValueFormatFunction?: TooltipValueFormatFunction;
}

export interface TbFlotPlotAxis extends JQueryPlotAxis, TbFlotAxisOptions {
  options: TbFlotAxisOptions;
}

export interface TbFlotAxisOptions extends JQueryPlotAxisOptions {
  tickUnits?: string;
  hidden?: boolean;
  keysInfo?: Array<{hidden: boolean}>;
  ticksFormatterFunction?: TbFlotTicksFormatterFunction;
}

export interface TbFlotPlotDataSeries extends JQueryPlotDataSeries {
  datasource?: Datasource;
  dataKey?: TbFlotDataKey;
  percent?: number;
}

export interface TbFlotPlotItem extends jquery.flot.item {
  series: TbFlotPlotDataSeries;
}

export interface TbFlotHoverInfo {
  seriesHover: Array<TbFlotSeriesHoverInfo>;
  time?: any;
}

export interface TbFlotSeriesHoverInfo {
  hoverIndex: number;
  units: string;
  decimals: number;
  label: string;
  color: string;
  index: number;
  tooltipValueFormatFunction: TooltipValueFormatFunction;
  value: any;
  time: any;
  distance: number;
}

export interface TbFlotThresholdMarking {
  lineWidth?: number;
  color?: string;
  [key: string]: any;
}

export interface TbFlotThresholdKeySettings {
  yaxis: number;
  lineWidth: number;
  color: string;
}

export interface TbFlotGridSettings {
  color: string;
  backgroundColor: string;
  tickColor: string;
  outlineWidth: number;
  verticalLines: boolean;
  horizontalLines: boolean;
  minBorderMargin?: number;
  margin?: number;
}

export interface TbFlotXAxisSettings {
  showLabels: boolean;
  title: string;
  color: boolean;
}

export interface TbFlotSecondXAxisSettings {
  axisPosition: TbFlotXAxisPosition;
  showLabels: boolean;
  title: string;
}

export interface TbFlotYAxisSettings {
  min: number;
  max: number;
  showLabels: boolean;
  title: string;
  color: string;
  ticksFormatter: string;
  tickDecimals: number;
  tickSize: number;
  tickGenerator: string;
}

export interface TbFlotBaseSettings {
  stack: boolean;
  enableSelection: boolean;
  shadowSize: number;
  fontColor: string;
  fontSize: number;
  tooltipIndividual: boolean;
  tooltipCumulative: boolean;
  tooltipValueFormatter: string;
  hideZeros: boolean;
  grid: TbFlotGridSettings;
  xaxis: TbFlotXAxisSettings;
  yaxis: TbFlotYAxisSettings;
}

export interface TbFlotLegendSettings {
  showLegend?: boolean;
  legendConfig?: LegendConfig;
}

export interface TbFlotComparisonSettings {
  comparisonEnabled: boolean;
  timeForComparison: ComparisonDuration;
  xaxisSecond: TbFlotSecondXAxisSettings;
  comparisonCustomIntervalValue?: number;
}

export interface TbFlotThresholdsSettings {
  thresholdsLineWidth: number;
}

export interface TbFlotCustomLegendSettings {
  customLegendEnabled: boolean;
  dataKeysListForLabels: Array<TbFlotLabelPatternSettings>;
}

export interface TbFlotLabelPatternSettings {
  name: string;
  type: DataKeyType;
  settings?: any;
}

export interface TbFlotGraphSettings extends TbFlotBaseSettings,
                                             TbFlotThresholdsSettings, TbFlotComparisonSettings, TbFlotCustomLegendSettings {
  smoothLines: boolean;
}

export declare type BarAlignment = 'left' | 'right' | 'center';

export interface TbFlotBarSettings extends TbFlotBaseSettings,
                                           TbFlotThresholdsSettings, TbFlotComparisonSettings, TbFlotCustomLegendSettings {
  defaultBarWidth: number;
  barAlignment: BarAlignment;
}

export interface TbFlotPieSettings {
  radius: number;
  innerRadius: number;
  tilt: number;
  animatedPie: boolean;
  stroke: {
    color: string;
    width: number;
  };
  showTooltip: boolean;
  showLabels: boolean;
  fontColor: string;
  fontSize: number;
}

export declare type TbFlotYAxisPosition = 'left' | 'right';
export declare type TbFlotXAxisPosition = 'top' | 'bottom';

export declare type TbFlotThresholdValueSource = 'predefinedValue' | 'entityAttribute';

export interface TbFlotKeyThreshold {
  thresholdValueSource: TbFlotThresholdValueSource;
  thresholdEntityAlias: string;
  thresholdAttribute: string;
  thresholdValue: number;
  lineWidth: number;
  color: string;
}

export interface TbFlotKeySettings extends DataKeySettingsWithComparison {
  excludeFromStacking: boolean;
  hideDataByDefault: boolean;
  disableDataHiding: boolean;
  removeFromLegend: boolean;
  showLines: boolean;
  fillLines: boolean;
  fillLinesOpacity: number;
  showPoints: boolean;
  showPointShape: string;
  pointShapeFormatter: string;
  showPointsLineWidth: number;
  showPointsRadius: number;
  lineWidth: number;
  tooltipValueFormatter: string;
  showSeparateAxis: boolean;
  axisMin: number;
  axisMax: number;
  axisTitle: string;
  axisTickDecimals: number;
  axisTickSize: number;
  axisPosition: TbFlotYAxisPosition;
  axisTicksFormatter: string;
  thresholds: TbFlotKeyThreshold[];
}

export interface TbFlotLatestKeySettings {
  useAsThreshold: boolean;
  thresholdLineWidth: number;
  thresholdColor: string;
}
