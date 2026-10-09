// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Datasource,
  defaultLegendConfig,
  LegendConfig,
  LegendPosition,
  widgetType
} from '@shared/models/widget.models';
import { alignment, alignmentTranslations, ColorRange, Font } from '@shared/models/widget-settings.models';
import { Insets } from '@shared/models/report.models';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { FormProperty, FormPropertyType } from '@shared/models/dynamic-form.models';
import { DashboardReportConfig } from '@shared/models/dashboard-report.models';
import {
  AggregationType,
  DAY,
  historyInterval,
  historyQuickInterval,
  HOUR, QuickTimeInterval,
  Timewindow
} from '@shared/models/time/time.models';
import { Direction } from '@shared/models/page/sort-order';
import { mergeDeep } from '@core/utils';
import {
  LineSeriesSettings, LineSeriesStepType, ThresholdLabelPosition,
  timeSeriesChartDefaultSettings,
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings,
  TimeSeriesChartStateSourceType, TimeSeriesChartThreshold, timeSeriesChartThresholdDefaultSettings,
  TimeSeriesChartYAxes,
  TimeSeriesChartYAxisSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  chartBarDefaultSettings,
  ChartBarSettings,
  ChartFillSettings,
  ChartFillType, ChartLabelPosition, ChartLineType, ChartShape, PieChartLabelPosition
} from '@home/components/widget/lib/chart/chart.models';
import {
  BarChartWithLabelsWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { TimeSeriesChartWidgetSettings } from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import { IntervalType } from '@shared/models/telemetry/telemetry.models';
import { LatestChartWidgetSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import { DoughnutLayout } from '@home/components/widget/lib/chart/doughnut-widget.models';
import { RangeChartWidgetSettings } from '@app/modules/home/components/widget/lib/chart/range-chart-widget.models';

export enum ReportComponentType {
  HEADING = 'HEADING',
  RICH_TEXT = 'RICH_TEXT',
  ENTITY_TABLE = 'ENTITY_TABLE',
  TIME_SERIES_TABLE = 'TIME_SERIES_TABLE',
  ALARM_TABLE = 'ALARM_TABLE',
  TIME_SERIES_CHART = 'TIME_SERIES_CHART',
  LATEST_CHART = 'LATEST_CHART',
  DASHBOARD = 'DASHBOARD',
  IMAGE = 'IMAGE',
  SUB_REPORT = 'SUB_REPORT',
  SPLIT_VIEW = 'SPLIT_VIEW',
  DIVIDER = 'DIVIDER',
  PAGE_BREAK = 'PAGE_BREAK'
}

export const reportComponentTypes: ReportComponentType[] = Object.keys(ReportComponentType) as ReportComponentType[];

export interface ReportComponentConfig {
  type: ReportComponentType;
  subType?: string;
}

export const isReportComponentConfig = (obj: any): obj is ReportComponentConfig => {
  return typeof obj === 'object' && obj !== null && 'type' in obj && reportComponentTypes.includes(obj.type);
}

export interface DataReportComponentConfig extends ReportComponentConfig {
  dataSources?: Datasource[];
}

export interface LayoutReportComponentConfig extends ReportComponentConfig {
  background?: string;
  margins?: Insets;
  paddings?: Insets;
  borderWidth?: number;
  borderRadius?: number;
  borderColor?: string;
}

export const isLayoutReportComponentConfig = (obj: any): obj is LayoutReportComponentConfig => {
  return typeof obj === 'object' && obj !== null && 'background' in obj && 'margins' in obj && 'paddings' in obj;
};

export interface ReportComponentLayoutSettings {
  background?: string;
  margins?: Insets;
  paddings?: Insets;
  borderWidth?: number;
  borderRadius?: number;
  borderColor?: string;
}

export const toReportComponentLayoutSettings = (config: LayoutReportComponentConfig): ReportComponentLayoutSettings => {
  return {
    background: config.background,
    margins: config.margins,
    paddings: config.paddings,
    borderWidth: config.borderWidth,
    borderRadius: config.borderRadius,
    borderColor: config.borderColor
  };
}

export const updateFromReportComponentLayoutSettings =
  (config: LayoutReportComponentConfig, settings: ReportComponentLayoutSettings): void => {
    config.background = settings.background;
    config.margins = settings.margins;
    config.paddings = settings.paddings;
    config.borderWidth = settings.borderWidth;
    config.borderRadius = settings.borderRadius;
    config.borderColor = settings.borderColor;
}

export interface DataWithLayoutReportComponentConfig extends DataReportComponentConfig, LayoutReportComponentConfig {}

export enum ReportDataKeySettingsType {
  DEFAULT = 'DEFAULT',
  COLUMN = 'COLUMN',
  TIME_SERIES_CHART = 'TIME_SERIES_CHART'
}

export interface ReportDataKeySettings {
  type: ReportDataKeySettingsType;
}

export interface TableReportCellSettings {
  font?: Font;
  color?: string;
  backgroundColor?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
}

export interface TableReportColumnSettings extends ReportDataKeySettings {
  type: ReportDataKeySettingsType.COLUMN;
  columnWidth?: string;
  header?: TableReportCellSettings;
  cell?: TableReportCellSettings;
}

const tableReportCellSettings = (header = false): FormProperty[] => ([
  {
    id: 'font',
    type: FormPropertyType.font,
    name: '{i18n:report-template.text-style}',
    forceSizeUnit: 'pt',
    allowedFontWeights: ['normal', 'bold'],
    allowedFontStyles: ['normal', 'italic'],
    default: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'pt',
      weight: header ? 'bold' : 'normal',
      style: 'normal'
    } as Font
  },
  {
    id: 'color',
    type: FormPropertyType.color,
    name: '{i18n:report-template.text-style}',
    default: 'rgba(0,0,0,0.87)'
  },
  {
    id: 'backgroundColor',
    type: FormPropertyType.color,
    name: '{i18n:report-template.background-color}',
    default: null
  },
  {
    id: 'textAlignment',
    type: FormPropertyType.select,
    name: '{i18n:report-template.alignment}',
    subLabel: '{i18n:report-template.horizontal}',
    fieldClass: 'standard-width',
    items: [
      {
        label: `{i18n:${alignmentTranslations.get('left')}}`,
        value: 'left'
      },
      {
        label: `{i18n:${alignmentTranslations.get('center')}}`,
        value: 'center'
      },
      {
        label: `{i18n:${alignmentTranslations.get('right')}}`,
        value: 'right'
      },
      {
        label: `{i18n:${alignmentTranslations.get('justify')}}`,
        value: 'justify'
      }
    ],
    default: header ? 'center' : 'left'
  },
  {
    id: 'verticalAlignment',
    type: FormPropertyType.select,
    name: '{i18n:report-template.alignment}',
    subLabel: '{i18n:report-template.vertical}',
    fieldClass: 'standard-width',
    items: [
      {
        label: `{i18n:${alignmentTranslations.get('top')}}`,
        value: 'top'
      },
      {
        label: `{i18n:${alignmentTranslations.get('middle')}}`,
        value: 'middle'
      },
      {
        label: `{i18n:${alignmentTranslations.get('bottom')}}`,
        value: 'bottom'
      }
    ],
    default: 'middle'
  }
]);

const severityReportCellSettings = (): FormProperty[] => {
  const settings = tableReportCellSettings();
  settings[0].default.weight = 'bold';
  settings[1].default = null;
  return settings;
}

const timeReportCellSettings = (): FormProperty[] => {
  const settings = tableReportCellSettings();
  settings[0].default.size = 9;
  return settings;
}

export const TableReportColumnSettingsForm: FormProperty[] = [
  {
    id: 'type',
    type: FormPropertyType.text,
    name: '',
    rowClass: '!hidden',
    default: ReportDataKeySettingsType.COLUMN
  },
  {
    id: 'columnWidth',
    type: FormPropertyType.cssSize,
    name: '{i18n:report-template.component.table.column-width}',
    allowedCssUnits: ['px', 'em', '%', 'pt', 'pc', 'in', 'cm', 'mm'],
    default: null
  },
  {
    id: 'header',
    type: FormPropertyType.fieldset,
    name: '{i18n:report-template.component.table.header}',
    properties: tableReportCellSettings(true),
    default: null
  },
  {
    id: 'cell',
    type: FormPropertyType.fieldset,
    name: '{i18n:report-template.component.table.cell}',
    properties: tableReportCellSettings(),
    default: null
  }
];

export const SeverityColumnSettingsForm: FormProperty[] = [
  TableReportColumnSettingsForm[0],
  TableReportColumnSettingsForm[1],
  TableReportColumnSettingsForm[2],
  {
    id: 'cell',
    type: FormPropertyType.fieldset,
    name: '{i18n:report-template.component.table.cell}',
    properties: severityReportCellSettings(),
    default: null
  }
];

export const TimeColumnSettingsForm: FormProperty[] = [
  TableReportColumnSettingsForm[0],
  TableReportColumnSettingsForm[1],
  TableReportColumnSettingsForm[2],
  {
    id: 'cell',
    type: FormPropertyType.fieldset,
    name: '{i18n:report-template.component.table.cell}',
    properties: timeReportCellSettings(),
    default: null
  }
];

export interface HeadingReportComponentConfig extends DataWithLayoutReportComponentConfig {
  value: string;
  font?: Font;
  color?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
  height?: number;
  type:  ReportComponentType.HEADING;
}

export interface RichTextReportComponentConfig extends DataWithLayoutReportComponentConfig {
  value: string;
  type: ReportComponentType.RICH_TEXT;
}

export interface Heading {
  text: string;
  font?: Font;
  color?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
  height?: number;
}

export interface TableSortOrder {
  column: string;
  direction: Direction;
}

export interface TableReportComponentConfig extends DataReportComponentConfig {
  showTableHeading: boolean;
  tableHeading: Heading;
  tableSortOrder: TableSortOrder;
}

export interface TableWithLayoutReportComponentConfig extends TableReportComponentConfig, LayoutReportComponentConfig {}

export interface EntityTableReportComponentConfig extends TableWithLayoutReportComponentConfig {
  type: ReportComponentType.ENTITY_TABLE;
}

export interface AlarmTableReportComponentConfig extends TableWithLayoutReportComponentConfig {
  alarmSource: Datasource;
  timewindow: Timewindow;
  type: ReportComponentType.ALARM_TABLE;
}

export interface TimeseriesTableReportComponentConfig extends TableWithLayoutReportComponentConfig {
  timewindow: Timewindow;
  showTimestamp: boolean;
  timestampLabel: string;
  timestampPattern: string;
  timestampColumnSettings?: TableReportColumnSettings;
  type: ReportComponentType.TIME_SERIES_TABLE;
}

export const imageSourceTypes = ['image', 'entityKey'];
type imageSourceTypeTuple = typeof imageSourceTypes;
export type imageSourceType = imageSourceTypeTuple[number];

export const imageSourceTypeTranslations = new Map<imageSourceType, string>(
  [
    ['image', 'report-template.component.image.source-image'],
    ['entityKey', 'report-template.component.image.source-entity-key']
  ]
);

export const imageWidthTypes = ['fitWidth', 'original', 'custom'];
type imageWidthTypeTuple = typeof imageWidthTypes;
export type imageWidthType = imageWidthTypeTuple[number];

export const imageWidthTypeTranslations = new Map<imageWidthType, string>(
  [
    ['fitWidth', 'report-template.component.image.width-fit-width'],
    ['original', 'report-template.component.image.width-original'],
    ['custom', 'report-template.component.image.width-custom']
  ]
);

export const imageAlignments = ['left', 'center', 'right'];
type imageAlignmentTuple = typeof imageAlignments;
export type imageAlignment = imageAlignmentTuple[number];

export const imageAlignmentTranslations = new Map<imageAlignment, string>(
  [
    ['left', 'report-template.component.image.alignment-left'],
    ['center', 'report-template.component.image.alignment-center'],
    ['right', 'report-template.component.image.alignment-right']
  ]
);

export interface BaseImageReportComponentConfig extends DataWithLayoutReportComponentConfig {
  widthType: imageWidthType;
  customWidth?: number;
  alignment: imageAlignment;
}

export interface ReportTimeSeriesChartKeySettings extends ReportDataKeySettings, Omit<TimeSeriesChartKeySettings, 'type'> {
  type: ReportDataKeySettingsType.TIME_SERIES_CHART;
  seriesType: TimeSeriesChartSeriesType;
}

export const toReportTimeSeriesChartKeySettings = (settings: TimeSeriesChartKeySettings): ReportTimeSeriesChartKeySettings => {
  settings = settings || {} as TimeSeriesChartKeySettings;
  const seriesType = settings?.type || TimeSeriesChartSeriesType.line;
  return {...settings, ...{ type: ReportDataKeySettingsType.TIME_SERIES_CHART, seriesType: seriesType }};
}

export const toTimeSeriesChartKeySettings = (settings: ReportTimeSeriesChartKeySettings): TimeSeriesChartKeySettings => {
  settings = settings || {} as ReportTimeSeriesChartKeySettings;
  const seriesType = settings?.seriesType || TimeSeriesChartSeriesType.line;
  return {...settings, ...{ type: seriesType }};
}

export interface ReportTimeSeriesChartSettings extends TimeSeriesChartSettings {
  showTitle?: boolean;
  title?: string;
  titleFont?: Font;
  titleColor?: string;
  titleAlignment?: alignment;
  showLegend: boolean;
  legendColumnTitleFont: Font;
  legendColumnTitleColor: string;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  legendConfig: LegendConfig;
}

export const reportTimeSeriesChartDefaultSettings: ReportTimeSeriesChartSettings = mergeDeep({} as ReportTimeSeriesChartSettings,
  timeSeriesChartDefaultSettings as ReportTimeSeriesChartSettings, {
    showTitle: true,
    title: 'Time series chart',
    titleFont: {
      family: 'Roboto',
      size: 18,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500'
    },
    titleColor: 'rgba(0, 0, 0, 0.87)',
    titleAlignment: 'center',
    showLegend: true,
    legendColumnTitleFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: 'normal'
    },
    legendColumnTitleColor: 'rgba(0, 0, 0, 0.38)',
    legendLabelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: 'normal'
    },
    legendLabelColor: 'rgba(0, 0, 0, 0.76)',
    legendValueFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500'
    },
    legendValueColor: 'rgba(0, 0, 0, 0.87)',
    legendConfig: {...defaultLegendConfig(widgetType.timeseries), position: LegendPosition.top},
    yAxes: {
      default: {
        labelFont: {
          weight: 'bold'
        }
      } as TimeSeriesChartYAxisSettings
    } as TimeSeriesChartYAxes
  } as ReportTimeSeriesChartSettings);

export const reportStateChartDefaultSettings: ReportTimeSeriesChartSettings = mergeDeep({} as ReportTimeSeriesChartSettings,
  reportTimeSeriesChartDefaultSettings,
  {
    states: [
      {
        label: 'Off',
        value: 0,
        sourceType: TimeSeriesChartStateSourceType.constant,
        sourceValue: false
      },
      {
        label: 'On',
        value: 1,
        sourceType: TimeSeriesChartStateSourceType.constant,
        sourceValue: true
      }
    ],
    legendConfig: {...defaultLegendConfig(null), position: LegendPosition.right},
  } as ReportTimeSeriesChartSettings);

export interface ReportBarChartWithLabelSettings extends ReportTimeSeriesChartSettings {
  showBarLabel: boolean;
  barLabelFont: Font;
  barLabelColor: string;
  showBarValue: boolean;
  barValueFont: Font;
  barValueColor: string;
  showBarBorder: boolean;
  barBorderWidth: number;
  barBorderRadius: number;
  barBackgroundSettings: ChartFillSettings;
  barUnits?: string;
  barDecimals?: number;
}

export const reportBarChartWithLabelsDefaultSettings: ReportBarChartWithLabelSettings = mergeDeep({} as ReportBarChartWithLabelSettings,
  reportTimeSeriesChartDefaultSettings as ReportBarChartWithLabelSettings,
  {
    showTitle: true,
    title: 'Bar chart with labels',
    barWidthSettings: {
      barGap: 0,
      intervalGap: 0.5
    },
    yAxes: {
      default: {
        showLine: false,
        showTicks: false,
        labelFont: {
          weight: 'bold'
        }
      } as TimeSeriesChartYAxisSettings
    } as TimeSeriesChartYAxes,
    xAxis: {
      showTicks: false,
      showSplitLines: false
    },
    legendConfig: {...defaultLegendConfig(null), position: LegendPosition.top},

    showBarLabel: true,
    barLabelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: 'normal',
      lineHeight: '12px'
    },
    barLabelColor: 'rgba(0, 0, 0, 0.54)',
    showBarValue: true,
    barValueFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: 'bold',
      lineHeight: '12px'
    },
    barValueColor: 'rgba(0, 0, 0, 0.76)',
    showBarBorder: false,
    barBorderWidth: 2,
    barBorderRadius: 0,
    barBackgroundSettings: {
      type: ChartFillType.none,
      opacity: 0.4,
      gradient: {
        start: 100,
        end: 0
      }
    },
    barUnits: '%',
    barDecimals: 0
  } as ReportBarChartWithLabelSettings);

export const toBarChartWithLabelsWidgetSettings = (
        reportBarChartWithLabelsSettings: ReportBarChartWithLabelSettings & TimeSeriesChartWidgetSettings): BarChartWithLabelsWidgetSettings => {
  return {
    dataZoom: false,
    showBarLabel: reportBarChartWithLabelsSettings.showBarLabel,
    barLabelFont: reportBarChartWithLabelsSettings.barLabelFont,
    barLabelColor: reportBarChartWithLabelsSettings.barLabelColor,
    showBarValue: reportBarChartWithLabelsSettings.showBarValue,
    barValueFont: reportBarChartWithLabelsSettings.barValueFont,
    barValueColor: reportBarChartWithLabelsSettings.barValueColor,
    showBarBorder: reportBarChartWithLabelsSettings.showBarBorder,
    barBorderWidth: reportBarChartWithLabelsSettings.barBorderWidth,
    barBorderRadius: reportBarChartWithLabelsSettings.barBorderRadius,
    barBackgroundSettings: reportBarChartWithLabelsSettings.barBackgroundSettings,
    noAggregationBarWidthSettings: reportBarChartWithLabelsSettings.noAggregationBarWidthSettings,
    grid: reportBarChartWithLabelsSettings.grid,
    yAxis: reportBarChartWithLabelsSettings.yAxes['default'],
    xAxis: reportBarChartWithLabelsSettings.xAxis,
    animation: reportBarChartWithLabelsSettings.animation,
    thresholds: reportBarChartWithLabelsSettings.thresholds,
    showLegend: reportBarChartWithLabelsSettings.showLegend,
    legendPosition: reportBarChartWithLabelsSettings.legendConfig.position,
    legendLabelFont: reportBarChartWithLabelsSettings.legendLabelFont,
    legendLabelColor: reportBarChartWithLabelsSettings.legendLabelColor,
    background: reportBarChartWithLabelsSettings.background,
    padding: reportBarChartWithLabelsSettings.padding,
    showTooltip: false
  } as BarChartWithLabelsWidgetSettings;
}

export interface ReportRangeChartSettings extends ReportTimeSeriesChartSettings {
  rangeColors: Array<ColorRange>;
  outOfRangeColor: string;
  showRangeThresholds: boolean;
  rangeThreshold: Partial<TimeSeriesChartThreshold>;
  fillArea: boolean;
  fillAreaOpacity: number;
  lineSettings: LineSeriesSettings;
  rangeUnits?: string;
  rangeDecimals?: number;
}

export const reportRangeChartDefaultSettings: ReportRangeChartSettings = mergeDeep({} as ReportRangeChartSettings,
  reportTimeSeriesChartDefaultSettings as ReportRangeChartSettings,
  {
    showTitle: true,
    title: 'Range chart',
    yAxes: {
      default: {
        showLine: false,
        showTicks: false
      } as TimeSeriesChartYAxisSettings
    } as TimeSeriesChartYAxes,
    xAxis: {
      showSplitLines: false
    },
    legendConfig: {...defaultLegendConfig(null), position: LegendPosition.top},
    legendLabelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: 'normal'
    },
    legendLabelColor: 'rgba(0, 0, 0, 0.76)',
    rangeColors: [
      {to: -20, color: '#234CC7'},
      {from: -20, to: 0, color: '#305AD7'},
      {from: 0, to: 10, color: '#7191EF'},
      {from: 10, to: 20, color: '#FFA600'},
      {from: 20, to: 30, color: '#F36900'},
      {from: 30, to: 40, color: '#F04022'},
      {from: 40, color: '#D81838'}
    ],
    outOfRangeColor: '#ccc',
    showRangeThresholds: true,
    rangeThreshold: mergeDeep({} as Partial<TimeSeriesChartThreshold>,
      timeSeriesChartThresholdDefaultSettings,
      { lineColor: '#37383b',
        lineType: ChartLineType.dashed,
        startSymbol: ChartShape.circle,
        startSymbolSize: 5,
        endSymbol: ChartShape.arrow,
        endSymbolSize: 7,
        labelPosition: ThresholdLabelPosition.insideEndTop,
        labelColor: '#37383b',
        enableLabelBackground: true}),
    fillArea: true,
    fillAreaOpacity: 0.7,
    lineSettings: mergeDeep({} as LineSeriesSettings, {
      showLine: true,
      step: false,
      stepType: LineSeriesStepType.start,
      smooth: false,
      lineType: ChartLineType.solid,
      lineWidth: 2,
      showPoints: false,
      showPointLabel: false,
      pointLabelPosition: ChartLabelPosition.top,
      pointLabelFont: {
        family: 'Roboto',
        size: 11,
        sizeUnit: 'px',
        style: 'normal',
        weight: '400',
        lineHeight: '1'
      },
      pointLabelColor: 'rgba(0, 0, 0, 0.76)',
      enablePointLabelBackground: false,
      pointLabelBackground: 'rgba(255,255,255,0.56)',
      pointShape: ChartShape.emptyCircle,
      pointSize: 4,
      fillAreaSettings: {
        type: ChartFillType.none,
        opacity: 0.4,
        gradient: {
          start: 100,
          end: 0
        }
      }
    }),
    rangeUnits: '°C',
    rangeDecimals: 0
  } as ReportRangeChartSettings);

export const toRangeChartWidgetSettings = (
  reportRangeChartSettings: ReportRangeChartSettings & TimeSeriesChartWidgetSettings): RangeChartWidgetSettings => {
  delete reportRangeChartSettings.rangeThreshold.value;
  return {
    dataZoom: false,
    rangeColors: reportRangeChartSettings.rangeColors,
    outOfRangeColor: reportRangeChartSettings.outOfRangeColor,
    showRangeThresholds: reportRangeChartSettings.showRangeThresholds,
    rangeThreshold: reportRangeChartSettings.rangeThreshold,
    fillArea: reportRangeChartSettings.fillArea,
    fillAreaOpacity: reportRangeChartSettings.fillAreaOpacity,
    showLine: reportRangeChartSettings.lineSettings.showLine,
    step: reportRangeChartSettings.lineSettings.step,
    stepType: reportRangeChartSettings.lineSettings.stepType,
    smooth: reportRangeChartSettings.lineSettings.smooth,
    lineType: reportRangeChartSettings.lineSettings.lineType,
    lineWidth: reportRangeChartSettings.lineSettings.lineWidth,
    showPoints: reportRangeChartSettings.lineSettings.showPoints,
    showPointLabel: reportRangeChartSettings.lineSettings.showPointLabel,
    pointLabelPosition: reportRangeChartSettings.lineSettings.pointLabelPosition,
    pointLabelFont: reportRangeChartSettings.lineSettings.pointLabelFont,
    pointLabelColor: reportRangeChartSettings.lineSettings.pointLabelColor,
    enablePointLabelBackground: reportRangeChartSettings.lineSettings.enablePointLabelBackground,
    pointLabelBackground: reportRangeChartSettings.lineSettings.pointLabelBackground,
    pointShape: reportRangeChartSettings.lineSettings.pointShape,
    pointSize: reportRangeChartSettings.lineSettings.pointSize,
    grid: reportRangeChartSettings.grid,
    yAxis: reportRangeChartSettings.yAxes['default'],
    xAxis: reportRangeChartSettings.xAxis,
    animation: reportRangeChartSettings.animation,
    thresholds: reportRangeChartSettings.thresholds,
    showLegend: reportRangeChartSettings.showLegend,
    legendPosition: reportRangeChartSettings.legendConfig.position,
    legendLabelFont: reportRangeChartSettings.legendLabelFont,
    legendLabelColor: reportRangeChartSettings.legendLabelColor,
    background: reportRangeChartSettings.background,
    padding: reportRangeChartSettings.padding,
    showTooltip: false
  } as RangeChartWidgetSettings;
};

export const defaultTimeSeriesChartTimewindow = mergeDeep<Timewindow>(
  {} as Timewindow,
  historyInterval(DAY),
  {
    history: {
      interval: HOUR
    },
    aggregation: {
      type: AggregationType.AVG,
      limit: 200
    }
  }
);

export const defaultStateChartTimewindow = mergeDeep<Timewindow>(
  {} as Timewindow,
  historyInterval(DAY),
  {
    history: {
      interval: HOUR
    },
    aggregation: {
      type: AggregationType.NONE,
      limit: 200
    }
  }
);

export const defaultBarChartWithLabelsTimewindow = mergeDeep<Timewindow>(
  {} as Timewindow,
  historyQuickInterval(QuickTimeInterval.CURRENT_HALF_YEAR),
  {
    history: {
      interval: IntervalType.MONTH
    },
    aggregation: {
      type: AggregationType.AVG,
      limit: 200
    }
  }
);

export interface ReportLatestChartSettings extends Omit<LatestChartWidgetSettings,
                                                       'showTooltip' | 'tooltipValueType' | 'tooltipValueDecimals' |
                                                       'tooltipValueFormater' | 'tooltipValueFont' | 'tooltipValueColor' |
                                                       'tooltipBackgroundColor' | 'tooltipBackgroundBlur' | 'animation' | 'background' | 'padding'> {
  showTitle?: boolean;
  title?: string;
  titleFont?: Font;
  titleColor?: string;
  titleAlignment?: alignment;

  units?: string;
  decimals?: number;
}

export const reportLatestChartDefaultSettings: ReportLatestChartSettings = {
  showTitle: true,
  title: 'Latest chart',
  titleFont: {
    family: 'Roboto',
    size: 18,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500'
  },
  titleColor: 'rgba(0, 0, 0, 0.87)',
  titleAlignment: 'center',

  units: '',
  decimals: 0,

  autoScale: false,
  sortSeries: false,
  showTotal: false,
  showLegend: true,

  legendPosition: LegendPosition.bottom,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: 'normal'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.38)',
  legendValueFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500'
  },
  legendValueColor: 'rgba(0, 0, 0, 0.87)',
  legendShowTotal: true
} as ReportLatestChartSettings;

export interface ReportBarChartSettings extends ReportLatestChartSettings {
  axisMin?: number;
  axisMax?: number;
  axisTickLabelFont: Font;
  axisTickLabelColor: string;
  barSettings: ChartBarSettings;
}

export const reportBarChartDefaultSettings: ReportBarChartSettings = mergeDeep<ReportBarChartSettings>(
  {} as ReportBarChartSettings,
  reportLatestChartDefaultSettings as ReportBarChartSettings,
  {
    axisTickLabelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400'
    },
    axisTickLabelColor: 'rgba(0, 0, 0, 0.54)',
    barSettings: mergeDeep({} as ChartBarSettings, chartBarDefaultSettings,
      {barWidth: 80, showLabel: true} as ChartBarSettings)
  } as ReportBarChartSettings
);

export interface ReportPieChartSettings extends ReportLatestChartSettings {
  showLabel: boolean;
  labelPosition: PieChartLabelPosition;
  labelFont: Font;
  labelColor: string;
  borderWidth: number;
  borderColor: string;
  radius: number;
  clockwise: boolean;
}

export const reportPieChartDefaultSettings: ReportPieChartSettings = mergeDeep<ReportPieChartSettings>(
  {} as ReportPieChartSettings,
  reportLatestChartDefaultSettings as ReportPieChartSettings,
  {
    showLabel: true,
    labelPosition: PieChartLabelPosition.outside,
    labelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: 'normal'
    },
    labelColor: '#000',
    borderWidth: 0,
    borderColor: '#000',
    radius: 80,
    clockwise: false
  } as ReportPieChartSettings
);

export interface ReportDoughnutChartSettings extends ReportLatestChartSettings {
  layout: DoughnutLayout;
  clockwise: boolean;
  totalValueFont: Font;
  totalValueColor: string;
}

export const reportDoughnutChartDefaultSettings = (horizontal: boolean): ReportDoughnutChartSettings =>
       (mergeDeep<ReportDoughnutChartSettings>(
  {} as ReportDoughnutChartSettings,
  reportLatestChartDefaultSettings as ReportDoughnutChartSettings,
  {
    autoScale: true,
    legendPosition: horizontal ? LegendPosition.right : LegendPosition.bottom,
    layout: DoughnutLayout.default,
    clockwise: false,
    totalValueFont: {
      family: 'Roboto',
      size: 24,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500'
    },
    totalValueColor: 'rgba(0, 0, 0, 0.87)'
  } as ReportDoughnutChartSettings
));

export interface BaseChartReportComponentConfig extends BaseImageReportComponentConfig {
  height: number;
}

export interface TimeseriesChartReportComponentConfig extends BaseChartReportComponentConfig {
  timewindow: Timewindow;
  timeSeriesChartSettings: ReportTimeSeriesChartSettings & ReportBarChartWithLabelSettings & ReportRangeChartSettings;
  type: ReportComponentType.TIME_SERIES_CHART;
}

export interface LatestChartReportComponentConfig<S extends ReportLatestChartSettings = ReportLatestChartSettings> extends BaseChartReportComponentConfig {
  latestChartSettings: S;
  type: ReportComponentType.LATEST_CHART;
}

export interface BarChartReportComponentConfig extends LatestChartReportComponentConfig<ReportBarChartSettings> {
  subType: 'latestBarChart'
}

export interface PieChartReportComponentConfig extends LatestChartReportComponentConfig<ReportPieChartSettings> {
  subType: 'pieChart'
}

export interface DoughnutChartReportComponentConfig extends LatestChartReportComponentConfig<ReportDoughnutChartSettings> {
  subType: 'doughnutChart'
}

export interface HorizontalDoughnutChartReportComponentConfig extends LatestChartReportComponentConfig<ReportDoughnutChartSettings> {
  subType: 'horizontalDoughnutChart'
}

export interface ImageReportComponentConfig extends BaseImageReportComponentConfig {
  sourceType: imageSourceType;
  imageUrl: string;
  type: ReportComponentType.IMAGE;
}

export interface DashboardReportComponentConfig extends BaseImageReportComponentConfig {
  config: Partial<DashboardReportConfig>;
  type: ReportComponentType.DASHBOARD;
}

export interface SubReportReportComponentConfig extends DataReportComponentConfig {
  templateId: ReportTemplateId;
  avoidPageBreakInside: boolean;
  type: ReportComponentType.SUB_REPORT;
}

export interface SplitViewReportComponentConfig extends LayoutReportComponentConfig {
  leftView?: ReportComponentConfig;
  rightView?: ReportComponentConfig;
  splitPosition: number;
  splitGap: number;
  leftVerticalAlignment: alignment;
  rightVerticalAlignment: alignment;
  type:  ReportComponentType.SPLIT_VIEW;
}

export enum BorderLength {
  LONG = 'LONG',
  SHORT = 'SHORT'
}

export const borderLengths = Object.keys(BorderLength) as BorderLength[];

export const borderLengthTranslations = new Map<BorderLength, string>(
  [
    [BorderLength.LONG, 'report-template.component.divider.divider-type-long'],
    [BorderLength.SHORT, 'report-template.component.divider.divider-type-short']
  ]
);

export enum BorderType {
  solid = 'solid',
  dashed = 'dashed',
  dotted = 'dotted'
}

export const borderTypes = Object.keys(BorderType) as BorderType[];

export const borderTypeTranslations = new Map<BorderType, string>(
  [
    [BorderType.solid, 'report-template.component.divider.line-type-solid'],
    [BorderType.dashed, 'report-template.component.divider.line-type-dashed'],
    [BorderType.dotted, 'report-template.component.divider.line-type-dotted']
  ]
);

export interface DividerReportComponentConfig extends LayoutReportComponentConfig {
  length: BorderLength;
  borderType: BorderType;
  widthPx: number;
  color: string;
  type: ReportComponentType.DIVIDER;
}

export interface PageBreakReportComponentConfig extends ReportComponentConfig {
  type: ReportComponentType.PAGE_BREAK;
}
