// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AlarmTableReportComponentConfig,
  BarChartReportComponentConfig,
  BorderLength,
  BorderType,
  DashboardReportComponentConfig,
  defaultBarChartWithLabelsTimewindow,
  defaultStateChartTimewindow,
  defaultTimeSeriesChartTimewindow,
  DividerReportComponentConfig,
  DoughnutChartReportComponentConfig,
  EntityTableReportComponentConfig,
  HeadingReportComponentConfig,
  HorizontalDoughnutChartReportComponentConfig,
  ImageReportComponentConfig, isReportComponentConfig,
  PageBreakReportComponentConfig,
  PieChartReportComponentConfig,
  reportBarChartDefaultSettings,
  ReportBarChartSettings,
  reportBarChartWithLabelsDefaultSettings,
  ReportBarChartWithLabelSettings,
  ReportComponentConfig,
  ReportComponentType,
  ReportDataKeySettingsType,
  reportDoughnutChartDefaultSettings,
  ReportDoughnutChartSettings,
  reportPieChartDefaultSettings,
  ReportPieChartSettings, reportRangeChartDefaultSettings, ReportRangeChartSettings,
  reportStateChartDefaultSettings,
  reportTimeSeriesChartDefaultSettings,
  ReportTimeSeriesChartSettings,
  RichTextReportComponentConfig,
  SubReportReportComponentConfig,
  TimeseriesChartReportComponentConfig,
  TimeseriesTableReportComponentConfig,
  toReportTimeSeriesChartKeySettings, SplitViewReportComponentConfig
} from '@shared/models/report-component.models';
import { Type } from '@angular/core';
import { HeadingPreviewComponent } from '@home/pages/reporting/template/components/heading-preview.component';
import { RichTextPreviewComponent } from '@home/pages/reporting/template/components/rich-text-preview.component';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { HeadingConfigComponent } from '@home/pages/reporting/template/components/heading-config.component';
import { RichTextConfigComponent } from '@home/pages/reporting/template/components/rich-text-config.component';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityService } from '@core/http/entity.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { PageBreakPreviewComponent } from '@home/pages/reporting/template/components/page-break-preview.component';
import { EmptyReportConfigComponent } from '@home/pages/reporting/template/components/empty-report-config.component';
import { EntityTablePreviewComponent } from '@home/pages/reporting/template/components/entity-table-preview.component';
import { EntityTableConfigComponent } from '@home/pages/reporting/template/components/entity-table-config.component';
import {
  EntityAliasSelectCallbacks
} from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component.models';
import {
  FilterSelectCallbacks
} from '@home/components/widget/lib/settings/common/filter/filter-select.component.models';
import { SubReportPreviewComponent } from '@home/pages/reporting/template/components/sub-report-preview.component';
import { SubReportConfigComponent } from '@home/pages/reporting/template/components/sub-report-config.component';
import { ImagePreviewComponent } from '@home/pages/reporting/template/components/image-preview.component';
import { ImageConfigComponent } from '@home/pages/reporting/template/components/image-config.component';

import keyImageTemplate from './key-image-svg.raw';
import { deepClone, insertVariable, mergeDeep, stringToBase64 } from '@core/utils';
import { DataKey, DatasourceType } from '@shared/models/widget.models';
import { DashboardPreviewComponent } from '@home/pages/reporting/template/components/dashboard-preview.component';
import { DashboardConfigComponent } from '@home/pages/reporting/template/components/dashboard-config.component';
import { AlarmTablePreviewComponent } from '@home/pages/reporting/template/components/alarm-table-preview.component';
import { AlarmTableConfigComponent } from '@home/pages/reporting/template/components/alarm-table-config.component';
import {
  TimeseriesTablePreviewComponent
} from '@home/pages/reporting/template/components/timeseries-table-preview.component';
import {
  TimeseriesTableConfigComponent
} from '@home/pages/reporting/template/components/timeseries-table-config.component';
import { TbReportFormat } from '@shared/models/report.models';
import { Font } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { AggregationType, DAY, historyInterval } from '@shared/models/time/time.models';
import { DividerPreviewComponent } from '@home/pages/reporting/template/components/divider-preview.component';
import { DividerConfigComponent } from '@home/pages/reporting/template/components/divider-config.component';
import { Direction } from '@shared/models/page/sort-order';
import {
  TimeSeriesChartPreviewComponent
} from '@home/pages/reporting/template/components/time-series-chart-preview.component';
import {
  TimeSeriesChartConfigComponent
} from '@home/pages/reporting/template/components/time-series-chart-config.component';
import { TimeSeriesChartType } from '@home/components/widget/lib/chart/time-series-chart.models';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import { LatestChartPreviewComponent } from '@home/pages/reporting/template/components/latest-chart-preview.component';
import { LatestChartConfigComponent } from '@home/pages/reporting/template/components/latest-chart-config.component';
import { SplitViewPreviewComponent } from '@home/pages/reporting/template/components/split-view-preview.component';
import { SplitViewConfigComponent } from '@home/pages/reporting/template/components/split-view-config.component';
import { CdkDragMove, CdkDragRelease, CdkDropList } from '@angular/cdk/drag-drop';

export enum ReportComponentLibraryGroup {
  textAndImages = 'textAndImages',
  dataAndTables = 'dataAndTables',
  charts = 'charts',
  branding = 'branding',
  reportInfoAndLayout = 'reportInfoAndLayout'
}

export const reportComponentLibraryGroups: ReportComponentLibraryGroup[] = Object.keys(ReportComponentLibraryGroup) as ReportComponentLibraryGroup[];

export const reportComponentLibraryGroupTranslations = new Map<ReportComponentLibraryGroup, string>(
  [
    [ReportComponentLibraryGroup.textAndImages, 'report-template.component.group.text-and-images'],
    [ReportComponentLibraryGroup.dataAndTables, 'report-template.component.group.data-and-tables'],
    [ReportComponentLibraryGroup.charts, 'report-template.component.group.charts'],
    [ReportComponentLibraryGroup.branding, 'report-template.component.group.branding'],
    [ReportComponentLibraryGroup.reportInfoAndLayout, 'report-template.component.group.report-info-and-layout']
  ]
);

export interface ReportComponentLibraryItem<C extends ReportComponentConfig = ReportComponentConfig> {
  title: string;
  previewImage: string;
  type: ReportComponentType;
  defaultConfig: C;
}

export const reportComponentGroups = new Map<string, Array<string>>(
  [
    [
      ReportComponentLibraryGroup.textAndImages,
      ['heading', 'richText', 'image', 'textSection', 'textImage', 'imageText']
    ],
    [
      ReportComponentLibraryGroup.dataAndTables,
      ['entityTable', 'timeSeriesTable', 'alarmTable', 'subReport', 'dashboard']
    ],
    [
      ReportComponentLibraryGroup.charts,
      ['timeSeriesChart', 'lineChart', 'barChart',
        'pointChart', 'stateChart', 'barChartWithLabels',
        'rangeChart', 'latestBarChart', 'pieChart',
        'doughnutChart', 'horizontalDoughnutChart']
    ],
    [
      ReportComponentLibraryGroup.branding,
      ['logoHeading', 'headingLogo', 'logoText', 'textLogo',
        'logoText2', 'footer1', 'footer2', 'footer3']
    ],
    [
      ReportComponentLibraryGroup.reportInfoAndLayout,
      ['splitView', 'pageNumber', 'createdTime', 'divider', 'pageBreak']
    ]
  ]
);

export const reportComponentsLibrary = new Map<string, ReportComponentLibraryItem>(
  [
    [
      'heading',
      {
        title: 'report-template.component.heading.type',
        previewImage: '/assets/report/components/heading.svg',
        type: ReportComponentType.HEADING,
        defaultConfig: {
          type: ReportComponentType.HEADING,
          value: 'Heading',
          font: {
            size: 40,
            sizeUnit: 'pt',
            weight: 'normal',
            style: 'normal',
            family: 'Roboto'
          } as Font,
          color: '#000',
          textAlignment: 'center',
          verticalAlignment: 'middle',
          height: undefined,
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as HeadingReportComponentConfig
      }
    ],
    [
      'richText',
      {
        title: 'report-template.component.rich-text.type',
        previewImage: '/assets/report/components/rich-text.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<p style="line-height: 1.5;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'textSection',
      {
        title: 'report-template.component.text-section',
        previewImage: '/assets/report/components/text-section.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<p><span style="font-size: 14px; color: rgb(0, 105, 92); font-weight: 500;">SUBHEADING</span></p>\n' +
            '<p><span style="font-size: 28px; font-weight: 500;">Heading</span></p>\n' +
            '<p style="line-height: 1.5;" >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'textImage',
      {
        title: 'report-template.component.text-image',
        previewImage: '/assets/report/components/text-image.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: null,
          background: null,
          leftView: {
            type: ReportComponentType.RICH_TEXT,
            value: '<p><span style="font-size: 28px; font-weight: 500;">Heading</span></p>\n' +
              '<p style="line-height: 1.5;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as RichTextReportComponentConfig,
          rightView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: null,
            widthType: 'fitWidth',
            alignment: 'center',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'imageText',
      {
        title: 'report-template.component.image-text',
        previewImage: '/assets/report/components/image-text.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: null,
          background: null,
          leftView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: null,
            widthType: 'fitWidth',
            alignment: 'center',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig,
          rightView: {
            type: ReportComponentType.RICH_TEXT,
            value: '<p><span style="font-size: 28px; font-weight: 500;">Heading</span></p>\n' +
              '<p style="line-height: 1.5;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as RichTextReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'entityTable',
      {
        title: 'report-template.component.entity-table.type',
        previewImage: '/assets/report/components/entity-table.svg',
        type: ReportComponentType.ENTITY_TABLE,
        defaultConfig: {
          type: ReportComponentType.ENTITY_TABLE,
          showTableHeading: false,
          tableHeading: {
            text: "Entities",
            font: {
              size: 20,
              sizeUnit: 'pt',
              weight: 'normal',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'center',
            verticalAlignment: 'middle',
            height: 40
          },
          tableSortOrder: {
            column: 'Name',
            direction: Direction.ASC
          },
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'name',
                  type: DataKeyType.entityField,
                  label: 'Name'
                }
              ]
            }
          ],
          margins: {
            top: 20
          },
          paddings: null,
          background: null
        } as EntityTableReportComponentConfig
      }
    ],
    [
      'timeSeriesTable',
      {
        title: 'report-template.component.timeseries-table.type',
        previewImage: '/assets/report/components/timeseries-table.svg',
        type: ReportComponentType.TIME_SERIES_TABLE,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_TABLE,
          showTableHeading: true,
          tableHeading: {
            text: '${entityName}',
            font: {
              size: 20,
              sizeUnit: 'pt',
              weight: 'normal',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'center',
            verticalAlignment: 'middle',
            height: 40
          },
          tableSortOrder: {
            column: 'Timestamp',
            direction: Direction.DESC
          },
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  units: '°C',
                  decimals: 0
                }
              ]
            }
          ],
          timewindow: {...historyInterval(DAY),
            aggregation: {
              type: AggregationType.NONE,
              limit: 200
            }
          },
          showTimestamp: true,
          timestampLabel: 'Timestamp',
          timestampPattern: 'yyyy-MM-dd HH:mm:ss',
          timestampColumnSettings: {
            type: ReportDataKeySettingsType.COLUMN
          },
          margins: {
            top: 20
          },
          paddings: null,
          background: null
        } as TimeseriesTableReportComponentConfig
      }
    ],
    [
      'alarmTable',
      {
        title: 'report-template.component.alarm-table.type',
        previewImage: '/assets/report/components/alarm-table.svg',
        type: ReportComponentType.ALARM_TABLE,
        defaultConfig: {
          type: ReportComponentType.ALARM_TABLE,
          showTableHeading: false,
          tableHeading: {
            text: "Alarms",
            font: {
              size: 20,
              sizeUnit: 'pt',
              weight: 'normal',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'center',
            verticalAlignment: 'middle',
            height: 40
          },
          tableSortOrder: {
            column: 'Created time',
            direction: Direction.DESC
          },
          alarmSource: {
            type: DatasourceType.entity,
            alarmFilterConfig: {},
            dataKeys: [
              {
                name: 'createdTime',
                type: DataKeyType.alarm,
                label: "Created time"
              },
              {
                name: 'originator',
                type: DataKeyType.alarm,
                label: "Originator"
              },
              {
                name: 'type',
                type: DataKeyType.alarm,
                label: "Type"
              },
              {
                name: 'severity',
                type: DataKeyType.alarm,
                label: "Severity"
              },
              {
                name: 'status',
                type: DataKeyType.alarm,
                label: "Status"
              },
              {
                name: 'assignee',
                type: DataKeyType.alarm,
                label: "Assignee"
              }
            ]
          },
          timewindow: historyInterval(DAY),
          margins: {
            top: 20
          },
          paddings: null,
          background: null
        } as AlarmTableReportComponentConfig
      }
    ],
    [
      'timeSeriesChart',
      {
        title: 'report-template.component.time-series-chart.type',
        previewImage: '/assets/report/components/time-series-chart.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'default',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  color: '#2196f3',
                  units: '°C',
                  decimals: 0,
                  settings: toReportTimeSeriesChartKeySettings(TbTimeSeriesChart.dataKeySettings(TimeSeriesChartType.default)(null, false))
                }
              ]
            }
          ],
          timewindow: defaultTimeSeriesChartTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'lineChart',
      {
        title: 'report-template.component.line-chart',
        previewImage: '/assets/report/components/line-chart.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'lineChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  color: '#2196f3',
                  units: '°C',
                  decimals: 0,
                  settings: toReportTimeSeriesChartKeySettings(TbTimeSeriesChart.dataKeySettings(TimeSeriesChartType.line)(null, false))
                }
              ]
            }
          ],
          timewindow: defaultTimeSeriesChartTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings,
            { title: 'Line chart' } as ReportTimeSeriesChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'barChart',
      {
        title: 'report-template.component.bar-chart',
        previewImage: '/assets/report/components/bar-chart.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'barChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  color: '#2196f3',
                  units: '°C',
                  decimals: 0,
                  settings: toReportTimeSeriesChartKeySettings(TbTimeSeriesChart.dataKeySettings(TimeSeriesChartType.bar)(null, false))
                }
              ]
            }
          ],
          timewindow: defaultTimeSeriesChartTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings,
            { title: 'Bar chart' } as ReportTimeSeriesChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'pointChart',
      {
        title: 'report-template.component.point-chart',
        previewImage: '/assets/report/components/point-chart.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'pointChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  color: '#2196f3',
                  units: '°C',
                  decimals: 0,
                  settings: toReportTimeSeriesChartKeySettings(TbTimeSeriesChart.dataKeySettings(TimeSeriesChartType.point)(null, false))
                }
              ]
            }
          ],
          timewindow: defaultTimeSeriesChartTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings,
            { title: 'Point chart' } as ReportTimeSeriesChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'stateChart',
      {
        title: 'report-template.component.state-chart',
        previewImage: '/assets/report/components/state-chart.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'stateChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'state',
                  type: DataKeyType.timeseries,
                  label: 'State',
                  color: '#2196f3',
                  units: '',
                  decimals: 0,
                  settings: toReportTimeSeriesChartKeySettings(TbTimeSeriesChart.dataKeySettings(TimeSeriesChartType.state)(null, false))
                }
              ]
            }
          ],
          timewindow: defaultStateChartTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportStateChartDefaultSettings,
            { title: 'State chart' } as ReportTimeSeriesChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'barChartWithLabels',
      {
        title: 'report-template.component.bar-chart-with-labels',
        previewImage: '/assets/report/components/bar-chart-with-labels.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'barChartWithLabels',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'humidity',
                  type: DataKeyType.timeseries,
                  label: 'Humidity',
                  color: '#2196f3',
                  settings: {}
                }
              ]
            }
          ],
          timewindow: defaultBarChartWithLabelsTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportBarChartWithLabelSettings>({} as ReportBarChartWithLabelSettings, reportBarChartWithLabelsDefaultSettings,
            { title: 'Bar chart with labels' } as ReportBarChartWithLabelSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'rangeChart',
      {
        title: 'report-template.component.range-chart',
        previewImage: '/assets/report/components/range-chart.svg',
        type: ReportComponentType.TIME_SERIES_CHART,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_CHART,
          subType: 'rangeChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  settings: {}
                }
              ]
            }
          ],
          timewindow: defaultTimeSeriesChartTimewindow,
          timeSeriesChartSettings: mergeDeep<ReportRangeChartSettings>({} as ReportRangeChartSettings, reportRangeChartDefaultSettings,
            { title: 'Range chart' } as ReportRangeChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as TimeseriesChartReportComponentConfig
      }
    ],
    [
      'latestBarChart',
      {
        title: 'report-template.component.bars',
        previewImage: '/assets/report/components/bars.svg',
        type: ReportComponentType.LATEST_CHART,
        defaultConfig: {
          type: ReportComponentType.LATEST_CHART,
          subType: 'latestBarChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'windPower',
                  type: DataKeyType.timeseries,
                  label: 'Wind',
                  color: '#08872B',
                  settings: {}
                },
                {
                  name: 'solarPower',
                  type: DataKeyType.timeseries,
                  label: 'Solar',
                  color: '#FF4D5A',
                  settings: {}
                },
                {
                  name: 'hydroelectricPower',
                  type: DataKeyType.timeseries,
                  label: 'Hydroelectric',
                  color: '#FFDE30',
                  settings: {}
                }
              ]
            }
          ],
          latestChartSettings: mergeDeep<ReportBarChartSettings>({} as ReportBarChartSettings, reportBarChartDefaultSettings,
            { title: 'Bars' } as ReportBarChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as BarChartReportComponentConfig
      }
    ],
    [
      'pieChart',
      {
        title: 'report-template.component.pie',
        previewImage: '/assets/report/components/pie-chart.svg',
        type: ReportComponentType.LATEST_CHART,
        defaultConfig: {
          type: ReportComponentType.LATEST_CHART,
          subType: 'pieChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'windPower',
                  type: DataKeyType.timeseries,
                  label: 'Wind',
                  color: '#08872B',
                  settings: {}
                },
                {
                  name: 'solarPower',
                  type: DataKeyType.timeseries,
                  label: 'Solar',
                  color: '#FF4D5A',
                  settings: {}
                },
                {
                  name: 'hydroelectricPower',
                  type: DataKeyType.timeseries,
                  label: 'Hydroelectric',
                  color: '#FFDE30',
                  settings: {}
                }
              ]
            }
          ],
          latestChartSettings: mergeDeep<ReportPieChartSettings>({} as ReportPieChartSettings, reportPieChartDefaultSettings,
            { title: 'Pie' } as ReportPieChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as PieChartReportComponentConfig
      }
    ],
    [
      'doughnutChart',
      {
        title: 'report-template.component.doughnut',
        previewImage: '/assets/report/components/doughnut-chart.svg',
        type: ReportComponentType.LATEST_CHART,
        defaultConfig: {
          type: ReportComponentType.LATEST_CHART,
          subType: 'doughnutChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'windPower',
                  type: DataKeyType.timeseries,
                  label: 'Wind',
                  color: '#08872B',
                  settings: {}
                },
                {
                  name: 'solarPower',
                  type: DataKeyType.timeseries,
                  label: 'Solar',
                  color: '#FF4D5A',
                  settings: {}
                }
              ]
            }
          ],
          latestChartSettings: mergeDeep<ReportDoughnutChartSettings>({} as ReportDoughnutChartSettings, reportDoughnutChartDefaultSettings(false),
            { title: 'Doughnut' } as ReportDoughnutChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as DoughnutChartReportComponentConfig
      }
    ],
    [
      'horizontalDoughnutChart',
      {
        title: 'report-template.component.horizontal-doughnut',
        previewImage: '/assets/report/components/horizontal-doughnut-chart.svg',
        type: ReportComponentType.LATEST_CHART,
        defaultConfig: {
          type: ReportComponentType.LATEST_CHART,
          subType: 'horizontalDoughnutChart',
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'windPower',
                  type: DataKeyType.timeseries,
                  label: 'Wind',
                  color: '#08872B',
                  settings: {}
                },
                {
                  name: 'solarPower',
                  type: DataKeyType.timeseries,
                  label: 'Solar',
                  color: '#FF4D5A',
                  settings: {}
                }
              ]
            }
          ],
          latestChartSettings: mergeDeep<ReportDoughnutChartSettings>({} as ReportDoughnutChartSettings, reportDoughnutChartDefaultSettings(true),
            { title: 'Doughnut' } as ReportDoughnutChartSettings),
          height: 400,
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as HorizontalDoughnutChartReportComponentConfig
      }
    ],
    [
      'image',
      {
        title: 'report-template.component.image.type',
        previewImage: '/assets/report/components/image.svg',
        type: ReportComponentType.IMAGE,
        defaultConfig: {
          type: ReportComponentType.IMAGE,
          sourceType: 'image',
          imageUrl: null,
          widthType: 'original',
          alignment: 'center',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as ImageReportComponentConfig
      }
    ],
    [
      'dashboard',
      {
        title: 'report-template.component.dashboard.type',
        previewImage: '/assets/report/components/dashboard.svg',
        type: ReportComponentType.DASHBOARD,
        defaultConfig: {
          type: ReportComponentType.DASHBOARD,
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: []
            }
          ],
          config: {
            type: 'png'
          },
          widthType: 'fitWidth',
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as DashboardReportComponentConfig
      }
    ],
    [
      'subReport',
      {
        title: 'report-template.component.sub-report.type',
        previewImage: '/assets/report/components/subreport.svg',
        type: ReportComponentType.SUB_REPORT,
        defaultConfig: {
          type: ReportComponentType.SUB_REPORT,
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: []
            }
          ],
          templateId: null,
          avoidPageBreakInside: false
        } as SubReportReportComponentConfig
      }
    ],
    [
      'logoHeading',
      {
        title: 'report-template.component.logo-heading',
        previewImage: '/assets/report/components/logo-heading.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null,
          leftView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: 'tb-image;/assets/report/components/logo-placeholder.svg',
            widthType: 'custom',
            customWidth: 140,
            alignment: 'left',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig,
          rightView: {
            type: ReportComponentType.HEADING,
            value: 'Heading',
            font: {
              size: 15,
              sizeUnit: 'pt',
              weight: 'bold',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'right',
            verticalAlignment: 'middle',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as HeadingReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'headingLogo',
      {
        title: 'report-template.component.heading-logo',
        previewImage: '/assets/report/components/heading-logo.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null,
          leftView: {
            type: ReportComponentType.HEADING,
            value: 'Heading',
            font: {
              size: 15,
              sizeUnit: 'pt',
              weight: 'bold',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'left',
            verticalAlignment: 'middle',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as HeadingReportComponentConfig,
          rightView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: 'tb-image;/assets/report/components/logo-placeholder.svg',
            widthType: 'custom',
            customWidth: 140,
            alignment: 'right',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'logoText',
      {
        title: 'report-template.component.logo-text',
        previewImage: '/assets/report/components/logo-text.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null,
          leftView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: 'tb-image;/assets/report/components/logo-placeholder.svg',
            widthType: 'custom',
            customWidth: 140,
            alignment: 'left',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig,
          rightView: {
            type: ReportComponentType.RICH_TEXT,
            value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;">\n' +
                      '<tbody>\n' +
                          '<tr>\n' +
                            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: right; line-height: 1.2;">\n' +
                              '<span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span>\n' +
                            '</td>\n' +
                          '</tr>\n' +
                      '</tbody>\n' +
                   '</table>',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as RichTextReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'textLogo',
      {
        title: 'report-template.component.text-logo',
        previewImage: '/assets/report/components/text-logo.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null,
          leftView: {
            type: ReportComponentType.RICH_TEXT,
            value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;">\n' +
              '<tbody>\n' +
              '<tr>\n' +
              '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: left; line-height: 1.2;">\n' +
              '<span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span>\n' +
              '</td>\n' +
              '</tr>\n' +
              '</tbody>\n' +
              '</table>',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as RichTextReportComponentConfig,
          rightView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: 'tb-image;/assets/report/components/logo-placeholder.svg',
            widthType: 'custom',
            customWidth: 140,
            alignment: 'right',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'logoText2',
      {
        title: 'report-template.component.logo-text-2',
        previewImage: '/assets/report/components/logo-text-2.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col style="width: 33%;"><col style="width: 33%;"><col style="width: 33%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: right; line-height: 1.2;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York,<br>New York(NY), 10037</span></td>\n' +
            '<td style="border-style: none; padding: 0px;"><img style="display: block; margin-left: auto; margin-right: auto;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="23px"></td>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; line-height: 1.2; text-align: left;"><span style="font-size: 14px; color: rgb(117, 117, 117);">Company name<br>+1 (727) 441-2403</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'footer1',
      {
        title: 'report-template.component.footer-1',
        previewImage: '/assets/report/components/footer-1.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; height: 44px;" border="1"><colgroup><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr style="height: 24px;">\n' +
            '<td style="border-width: 0px; height: 24px; line-height: 1.5;"><span style="font-size: 16px; font-weight: 500;">Company name</span></td>\n' +
            '</tr>\n' +
            '<tr style="height: 20px;">\n' +
            '<td style="border-width: 0px; height: 20px; line-height: 1.5;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'footer2',
      {
        title: 'report-template.component.footer-2',
        previewImage: '/assets/report/components/footer-2.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: {
            left: 6,
            right: 6
          },
          background: null,
          leftView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: 'tb-image;/assets/report/components/logo-placeholder.svg',
            widthType: 'custom',
            customWidth: 140,
            alignment: 'left',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig,
          rightView: {
            type: ReportComponentType.RICH_TEXT,
            value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; height: 48px;" border="1"><colgroup><col style="width: 100%;"></colgroup>\n' +
              '<tbody>\n' +
              '<tr style="height: 24px;">\n' +
              '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: right;"><span style="font-size: 16px; font-weight: 500;">Company name</span></td>\n' +
              '</tr>\n' +
              '<tr style="height: 24px;">\n' +
              '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: right;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
              '</tr>\n' +
              '</tbody>\n' +
              '</table>',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as RichTextReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'footer3',
      {
        title: 'report-template.component.footer-3',
        previewImage: '/assets/report/components/footer-3.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          type: ReportComponentType.SPLIT_VIEW,
          splitPosition: 50,
          splitGap: 10,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: {
            left: 6,
            right: 6
          },
          background: null,
          leftView: {
            type: ReportComponentType.RICH_TEXT,
            value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; height: 48px;" border="1"><colgroup><col style="width: 100%;"></colgroup>\n' +
              '<tbody>\n' +
              '<tr style="height: 24px;">\n' +
              '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: left;"><span style="font-size: 16px; font-weight: 500;">Company name</span></td>\n' +
              '</tr>\n' +
              '<tr style="height: 24px;">\n' +
              '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: left;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
              '</tr>\n' +
              '</tbody>\n' +
              '</table>',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as RichTextReportComponentConfig,
          rightView: {
            type: ReportComponentType.IMAGE,
            sourceType: 'image',
            imageUrl: 'tb-image;/assets/report/components/logo-placeholder.svg',
            widthType: 'custom',
            customWidth: 140,
            alignment: 'right',
            dataSources: [],
            margins: null,
            paddings: null,
            background: null
          } as ImageReportComponentConfig
        } as SplitViewReportComponentConfig
      }
    ],
    [
      'pageNumber',
      {
        title: 'report-template.component.page-number',
        previewImage: '/assets/report/components/page-number.svg',
        type: ReportComponentType.HEADING,
        defaultConfig: {
          type: ReportComponentType.HEADING,
          value: 'Page: ${pageNumber}/${totalPages}',
          font: {
            size: 9,
            sizeUnit: 'pt',
            weight: '500',
            style: 'normal',
            family: 'Roboto'
          } as Font,
          color: '#000',
          textAlignment: 'center',
          verticalAlignment: 'middle',
          height: undefined,
          dataSources: [],
          margins: null,
          paddings: {
            top: 6,
            bottom: 6,
            left: 6,
            right: 6
          },
          background: null
        } as HeadingReportComponentConfig
      }
    ],
    [
      'createdTime',
      {
        title: 'report-template.component.created-time',
        previewImage: '/assets/report/components/created-time.svg',
        type: ReportComponentType.HEADING,
        defaultConfig: {
          type: ReportComponentType.HEADING,
          value: 'Created: ${reportCreatedTime}',
          font: {
            size: 9,
            sizeUnit: 'pt',
            weight: '500',
            style: 'normal',
            family: 'Roboto'
          } as Font,
          color: '#000',
          textAlignment: 'center',
          verticalAlignment: 'middle',
          height: undefined,
          dataSources: [],
          margins: null,
          paddings: {
            top: 6,
            bottom: 6,
            left: 6,
            right: 6
          },
          background: null
        } as HeadingReportComponentConfig
      }
    ],
    [
      'divider',
      {
        title: 'report-template.component.divider.type',
        previewImage: '/assets/report/components/divider.svg',
        type: ReportComponentType.DIVIDER,
        defaultConfig: {
          type: ReportComponentType.DIVIDER,
          length: BorderLength.LONG,
          borderType: BorderType.solid,
          widthPx: 1,
          color: '#d6d6d6',
          margins: null,
          paddings: {
            top: 15,
            bottom: 15,
            left: 8,
            right: 8
          },
          background: null
        } as DividerReportComponentConfig
      }
    ],
    [
      'pageBreak',
      {
        title: 'report-template.component.page-break.type',
        previewImage: '/assets/report/components/page-break.svg',
        type: ReportComponentType.PAGE_BREAK,
        defaultConfig: {
          type: ReportComponentType.PAGE_BREAK
        } as PageBreakReportComponentConfig
      }
    ],
    [
      'splitView',
      {
        title: 'report-template.component.split-view.type',
        previewImage: '/assets/report/components/split-view.svg',
        type: ReportComponentType.SPLIT_VIEW,
        defaultConfig: {
          leftView: null,
          rightView: null,
          splitPosition: 50,
          splitGap: 8,
          leftVerticalAlignment: 'middle',
          rightVerticalAlignment: 'middle',
          margins: null,
          paddings: null,
          background: null,
          type: ReportComponentType.SPLIT_VIEW
        } as SplitViewReportComponentConfig
      }
    ]
  ]
);

export interface ReportComponentTypeData<C extends ReportComponentConfig = ReportComponentConfig> {
  title: string;
  previewComponent: Type<AbstractReportComponentPreview<C>>;
  configComponent: Type<AbstractReportComponentConfig<C>>;
  editable: boolean;
  container?: boolean;
  pageBreak?: boolean;
  preferredSettingsWidthPx?: number;
  configContext?: {[key: string]: any};
  previewContext?: {[key: string]: any};
}

export class ReportComponentTypesData {

  private reportComponentsTypeMap = new Map<ReportComponentType, Map<string, ReportComponentTypeData>>();

  constructor() {
  }

  public registerReportComponentType(type: ReportComponentType, typeData: ReportComponentTypeData) {
    this.registerReportComponentSubType(type, 'default', typeData);
  }

  public registerReportComponentSubType(type: ReportComponentType, subType: string, typeData: ReportComponentTypeData) {
    let subTypeMap: Map<string, ReportComponentTypeData>;
    if (!this.reportComponentsTypeMap.has(type)) {
      subTypeMap = new Map<string, ReportComponentTypeData>();
      this.reportComponentsTypeMap.set(type, subTypeMap);
    } else {
      subTypeMap = this.reportComponentsTypeMap.get(type);
    }
    subTypeMap.set(subType, typeData);
  }

  public getReportComponentTypeData(type: ReportComponentType, subType: string = ''): ReportComponentTypeData {
    const subTypeMap = this.reportComponentsTypeMap.get(type);
    return subTypeMap.get(subType || 'default');
  }

  public getReportComponentTypes(): ReportComponentType[] {
    return Array.from(this.reportComponentsTypeMap.keys());
  }
}

export const reportComponentTypesData = new ReportComponentTypesData();

reportComponentTypesData.registerReportComponentType(ReportComponentType.HEADING,  {
  title: 'report-template.component.heading.type',
  previewComponent: HeadingPreviewComponent,
  configComponent: HeadingConfigComponent,
  editable: true
});

reportComponentTypesData.registerReportComponentType(ReportComponentType.RICH_TEXT,
  {
    title: 'report-template.component.rich-text.type',
    previewComponent: RichTextPreviewComponent,
    configComponent: RichTextConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.ENTITY_TABLE,
  {
    title: 'report-template.component.entity-table.type',
    previewComponent: EntityTablePreviewComponent,
    configComponent: EntityTableConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.TIME_SERIES_TABLE,
  {
    title: 'report-template.component.timeseries-table.type',
    previewComponent: TimeseriesTablePreviewComponent,
    configComponent: TimeseriesTableConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.ALARM_TABLE,
  {
    title: 'report-template.component.alarm-table.type',
    previewComponent: AlarmTablePreviewComponent,
    configComponent: AlarmTableConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.TIME_SERIES_CHART,
  {
    title: 'report-template.component.time-series-chart.type',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      chartType: TimeSeriesChartType.default
    },
    previewContext: {
      chartType: TimeSeriesChartType.default
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.TIME_SERIES_CHART,
  'lineChart',
  {
    title: 'report-template.component.line-chart',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      chartType: TimeSeriesChartType.line
    },
    previewContext: {
      chartType: TimeSeriesChartType.line
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.TIME_SERIES_CHART,
  'barChart',
  {
    title: 'report-template.component.bar-chart',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      chartType: TimeSeriesChartType.bar
    },
    previewContext: {
      chartType: TimeSeriesChartType.bar
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.TIME_SERIES_CHART,
  'pointChart',
  {
    title: 'report-template.component.point-chart',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      chartType: TimeSeriesChartType.point
    },
    previewContext: {
      chartType: TimeSeriesChartType.point
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.TIME_SERIES_CHART,
  'stateChart',
  {
    title: 'report-template.component.state-chart',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      chartType: TimeSeriesChartType.state
    },
    previewContext: {
      chartType: TimeSeriesChartType.state
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.TIME_SERIES_CHART,
  'barChartWithLabels',
  {
    title: 'report-template.component.bar-chart-with-labels',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      barChartWithLabels: true
    },
    previewContext: {
      barChartWithLabels: true
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.TIME_SERIES_CHART,
  'rangeChart',
  {
    title: 'report-template.component.range-chart',
    previewComponent: TimeSeriesChartPreviewComponent,
    configComponent: TimeSeriesChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
      rangeChart: true
    },
    previewContext: {
      rangeChart: true
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.LATEST_CHART,
  'latestBarChart',
  {
    title: 'report-template.component.bars',
    previewComponent: LatestChartPreviewComponent,
    configComponent: LatestChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
    },
    previewContext: {
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.LATEST_CHART,
  'pieChart',
  {
    title: 'report-template.component.pie',
    previewComponent: LatestChartPreviewComponent,
    configComponent: LatestChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
    },
    previewContext: {
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.LATEST_CHART,
  'doughnutChart',
  {
    title: 'report-template.component.doughnut',
    previewComponent: LatestChartPreviewComponent,
    configComponent: LatestChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
    },
    previewContext: {
    }
  });

reportComponentTypesData.registerReportComponentSubType(ReportComponentType.LATEST_CHART,
  'horizontalDoughnutChart',
  {
    title: 'report-template.component.horizontal-doughnut',
    previewComponent: LatestChartPreviewComponent,
    configComponent: LatestChartConfigComponent,
    editable: true,
    preferredSettingsWidthPx: 1000,
    configContext: {
    },
    previewContext: {
    }
  });


reportComponentTypesData.registerReportComponentType(ReportComponentType.IMAGE,
  {
    title: 'report-template.component.image.type',
    previewComponent: ImagePreviewComponent,
    configComponent: ImageConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.DASHBOARD,
  {
    title: 'report-template.component.dashboard.type',
    previewComponent: DashboardPreviewComponent,
    configComponent: DashboardConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.SUB_REPORT,
  {
    title: 'report-template.component.sub-report.type',
    previewComponent: SubReportPreviewComponent,
    configComponent: SubReportConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.SPLIT_VIEW,
  {
    title: 'report-template.component.split-view.type',
    previewComponent: SplitViewPreviewComponent,
    configComponent: SplitViewConfigComponent,
    editable: true,
    container: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.DIVIDER,
  {
    title: 'report-template.component.divider.type',
    previewComponent: DividerPreviewComponent,
    configComponent: DividerConfigComponent,
    editable: true
  });

reportComponentTypesData.registerReportComponentType(ReportComponentType.PAGE_BREAK,
  {
    title: 'report-template.component.page-break.type',
    previewComponent: PageBreakPreviewComponent,
    configComponent: EmptyReportConfigComponent,
    editable: false,
    pageBreak: true
  });

export const reportComponentTypes = reportComponentTypesData.getReportComponentTypes();

export const csvReportComponentTypes: ReportComponentType[] =
  [
    ReportComponentType.ENTITY_TABLE,
    ReportComponentType.TIME_SERIES_TABLE,
    ReportComponentType.ALARM_TABLE,
    ReportComponentType.SUB_REPORT
  ];

export class ReportDragDropContext {

  dropLists: CdkDropList[] = [];
  currentHoverDropListId?: string;

  constructor() {
  }

  public register(dropList: CdkDropList) {
    this.dropLists.push(dropList);
  }

  public deregister(dropList: CdkDropList) {
    const index = this.dropLists.indexOf(dropList);
    if (index > -1) {
      this.dropLists.splice(index, 1);
    }
  }

  dragMoved(event: CdkDragMove) {
    const elementFromPoint = document.elementFromPoint(
      event.pointerPosition.x,
      event.pointerPosition.y
    );

    if (!elementFromPoint) {
      this.currentHoverDropListId = undefined;
      return;
    }

    const dropList = (elementFromPoint.classList.contains('cdk-drop-list') ||
                               elementFromPoint.classList.contains('tb-drop-list-placeholder'))
      ? elementFromPoint
      : (elementFromPoint.closest('.cdk-drop-list') || elementFromPoint.closest('.tb-drop-list-placeholder'));

    if (!dropList) {
      this.currentHoverDropListId = undefined;
      return;
    }

    this.currentHoverDropListId = dropList.id;
  }

  dragReleased(event: CdkDragRelease) {
    this.currentHoverDropListId = undefined;
  }

}


export interface ReportComponentContext {
  translate: TranslateService,
  utils: UtilsService,
  entityService: EntityService;
  aliasController: IAliasController;
  aliasAndFilterCallbacks: EntityAliasSelectCallbacks & FilterSelectCallbacks;
  format: TbReportFormat;
  dragDropCtx: ReportDragDropContext;
}

export const assignReportComponent = (reportComponent: ReportComponentConfig, sourceReportComponent: ReportComponentConfig): void => {
  const ignoreFields: string[] = [];
  for (const key of Object.keys(reportComponent)) {
    if (isReportComponentConfig(reportComponent[key])) {
      ignoreFields.push(key);
    }
  }
  const temp = {} as any;
  for (const field of ignoreFields) {
    temp[field] = reportComponent[field];
  }
  Object.assign(reportComponent, sourceReportComponent);
  for(const key in reportComponent){
    if(!(key in sourceReportComponent))
      delete reportComponent[key];
  }
  for (const field of ignoreFields) {
    reportComponent[field] = temp[field];
  }
}

export const editReportComponent = (reportComponent: ReportComponentConfig): ReportComponentConfig => {
  const ignoreFields: string[] = [];
  for (const key of Object.keys(reportComponent)) {
    if (isReportComponentConfig(reportComponent[key])) {
      ignoreFields.push(key);
    }
  }
  const result = deepClone(reportComponent, ignoreFields);
  for (const field of ignoreFields) {
    delete result[field];
  }
  return result;
}

export const pointsToPixels = (points: number): number => points * 1.3333343412075;

export type ReportVariableType = 'entityKey' | 'pageVariable';

export interface ReportVariable {
  type: ReportVariableType;
  name: string;
  dataKey?: DataKey;
}

export const pageVariables: ReportVariable[] = [
  {
    type: 'pageVariable',
    name: 'pageNumber'
  },
  {
    type: 'pageVariable',
    name: 'totalPages'
  },
  {
    type: 'pageVariable',
    name: 'reportCreatedTime'
  }
];

export const keyImage = (key: string): string => {
  const result = insertVariable(keyImageTemplate, 'key', `\${${key}}`);
  const encodedSvg = stringToBase64(result);
  return `data:image/svg+xml;base64,${encodedSvg}`;
}

export const imagePlaceholder = '/assets/report/components/image-placeholder.svg';

const variablePattern = /^\${([^}]*)}$/;

export const isKeyVariable = (test: string): boolean => {
  return variablePattern.test(test);
}

export const extractKeyFromVariable = (variable: string): string => {
  const match = variablePattern.exec(variable);
  if (match !== null) {
    return match[1];
  } else {
    return '';
  }
}
