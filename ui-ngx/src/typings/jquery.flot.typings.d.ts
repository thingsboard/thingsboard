// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
  touch?: boolean;
}

interface JQueryPlotSelectionRanges {
  [axis: string]: {
    from: number;
    to: number;
  };
}
