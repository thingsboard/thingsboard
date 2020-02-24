///
/// Copyright © 2016-2020 The Thingsboard Authors
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


interface JQueryPlot extends jquery.flot.plot {
  destroy(): void;
  highlight(series: jquery.flot.dataSeries | number, datapoint: jquery.flot.item | number): void;
  clearSelection(): void;
}

interface JQueryPlotPoint extends jquery.flot.point {
  pageX: number;
  pageY: number;
  x2: number;
}

interface JQueryPlotDataSeries extends jquery.flot.dataSeries, JQueryPlotSeriesOptions {
  datapoints?: jquery.flot.datapoints;
}

interface JQueryPlotOptions extends jquery.flot.plotOptions {
  title?: string;
  subtitile?: string;
  shadowSize?: number;
  HtmlText?: boolean;
  selection?: JQueryPlotSelection;
  xaxis?: JQueryPlotAxisOptions;
  series?: JQueryPlotSeriesOptions;
  crosshair?: JQueryPlotCrosshairOptions;
}

interface JQueryPlotAxisOptions extends jquery.flot.axisOptions {
  label?: string;
  labelFont?: any;
}

interface JQueryPlotAxis extends jquery.flot.axis, JQueryPlotAxisOptions {
  options: JQueryPlotAxisOptions;
}

interface JQueryPlotSeriesOptions extends jquery.flot.seriesOptions {
  stack?: boolean;
  curvedLines?: JQueryPlotCurvedLinesOptions;
  pie?: JQueryPlotPieOptions;
}

declare type JQueryPlotCrosshairMode = 'x' | 'y' | 'xy' | null;

interface JQueryPlotCrosshairOptions {
  mode?: JQueryPlotCrosshairMode;
  color?: string;
  lineWidth?: number;
}

interface JQueryPlotCurvedLinesOptions {
  active?: boolean;
  apply?: boolean;
  monotonicFit?: boolean;
  tension?: number;
  nrSplinePoints?: number;
  legacyOverride?: any;
}

interface JQueryPlotPieOptions {
  show?: boolean;
  radius?: any;
  innerRadius?: any;
  startAngle?: number;
  tilt?: number;
  offset?: {
    top?: number;
    left?: number;
  };
  stroke?: {
    color?: string;
    width?: number;
  };
  shadow?: {
    top?: number;
    left?: number;
    alpha?: number;
  };
  label?: {
    show?: boolean;
    formatter?: (label: string, slice?: any) => string;
    radius?: any;
    background?: {
      color?: string;
      opacity?: number;
    };
    threshold?: number;
  };
  combine?: {
    threshold?: number;
    color?: string;
    label?: string;
  };
  highlight?: number;
}

declare type JQueryPlotSelectionMode = 'x' | 'y' | 'xy' | null;
declare type JQueryPlotSelectionShape = 'round' | 'mitter' | 'bevel';

interface JQueryPlotSelection {
  mode?: JQueryPlotSelectionMode;
  color?: string;
  shape?: JQueryPlotSelectionShape;
  minSize?: number;
}

interface JQueryPlotSelectionRanges {
  [axis: string]: {
    from: number;
    to: number;
  };
}
