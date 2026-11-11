// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  inject,
  Input,
  OnDestroy,
  Type,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  reportBarChartWithLabelsDefaultSettings,
  ReportBarChartWithLabelSettings, reportRangeChartDefaultSettings, ReportRangeChartSettings,
  reportStateChartDefaultSettings,
  reportTimeSeriesChartDefaultSettings,
  ReportTimeSeriesChartSettings,
  TimeseriesChartReportComponentConfig,
  toBarChartWithLabelsWidgetSettings, toRangeChartWidgetSettings
} from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import {
  GenerateDataFunction,
  ReportWidgetContextService
} from '@home/pages/reporting/template/components/report-widget-context.service';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { TimeSeriesChartWidgetComponent } from '@home/components/widget/lib/chart/time-series-chart-widget.component';
import { IWidgetSubscription, WidgetSubscriptionCallbacks } from '@core/api/widget-api.models';
import { debounce, deepClone, mergeDeepIgnoreArray } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { BackgroundType, ComponentStyle, textStyle, ValueSourceType } from '@shared/models/widget-settings.models';
import { TimeSeriesChartWidgetSettings } from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import {
  TimeSeriesChartStateSourceType,
  TimeSeriesChartType
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  BarChartWithLabelsWidgetComponent
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.component';
import { ChartWidgetComponent } from '@home/components/widget/lib/chart/chart.models';
import { RangeChartWidgetComponent } from '@home/components/widget/lib/chart/range-chart-widget.component';
import { reportComponentTypesData } from '@home/pages/reporting/template/components/report-component.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-time-series-chart-preview',
    templateUrl: './time-series-chart-preview.component.html',
    styleUrls: ['./time-series-chart-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TimeSeriesChartPreviewComponent extends AbstractReportComponentPreview<TimeseriesChartReportComponentConfig>
  implements AfterViewInit, OnDestroy, WidgetSubscriptionCallbacks {

  @ViewChild('widgetContent', {read: ViewContainerRef, static: false}) widgetContainer: ViewContainerRef;

  @Input()
  @coerceBoolean()
  barChartWithLabels = false;

  @Input()
  @coerceBoolean()
  rangeChart = false;

  @Input()
  chartType: TimeSeriesChartType = TimeSeriesChartType.default;

  private reportWidgetContextService = inject(ReportWidgetContextService);
  private translate = inject(TranslateService);

  imageWidth: string = '100%';
  imageHeightPx: number = 400;

  imageAlign: string = 'center';

  showTitle: boolean;
  title: string;
  titleStyle: ComponentStyle;

  hasData = false;
  noDataMessage: string;

  chartTypeTitle: string;

  private viewInited = false;

  private widgetContext: WidgetContext;
  private widgetComponentRef: ComponentRef<ChartWidgetComponent>;
  private widgetComponent: ChartWidgetComponent;

  private updateWidgetPreview = debounce(() => {
    this.updateTimeSeriesWidgetPreview();
  }, 150);

  onComponentUpdated() {
    this.imageWidth = '100%';
    if (this.reportComponent.widthType === 'original') {
      this.imageWidth = 'auto';
    } else if (this.reportComponent.widthType === 'custom') {
      const customWidth = this.reportComponent.customWidth || 100;
      this.imageWidth = customWidth + 'px';
    }
    this.imageAlign = this.reportComponent.alignment || 'center';
    this.imageHeightPx = this.reportComponent.height || 400;

    this.showTitle = this.reportComponent.timeSeriesChartSettings.showTitle;
    this.title = this.reportComponent.timeSeriesChartSettings.title;
    this.titleStyle = textStyle(this.reportComponent.timeSeriesChartSettings.titleFont);
    this.titleStyle.color = this.reportComponent.timeSeriesChartSettings.titleColor;
    this.titleStyle.textAlign = this.reportComponent.timeSeriesChartSettings.titleAlignment;
    this.chartTypeTitle = this.translate.instant(reportComponentTypesData.getReportComponentTypeData(this.reportComponent.type, this.reportComponent.subType).title);

    const datasources = this.reportComponent.dataSources;
    if (datasources?.length) {
      const datasource = datasources[0];
      if (datasource.type === DatasourceType.device && datasource.deviceId || datasource.type === DatasourceType.entity && datasource.entityAliasId) {
        if (datasource.dataKeys?.length) {
          this.hasData = true;
        } else {
          this.hasData = false;
          this.noDataMessage = 'report-template.component.time-series-chart.no-series-configured';
        }
      } else {
        this.hasData = false;
        this.noDataMessage = 'report-template.component.time-series-chart.no-datasource-configured';
      }
    } else {
      this.hasData = false;
      this.noDataMessage = 'report-template.component.time-series-chart.no-datasource-configured';
    }

    if (this.viewInited) {
      this.updateWidgetPreview();
    }
  }

  ngAfterViewInit() {
    this.viewInited = true;
    this.updateTimeSeriesWidgetPreview();
  }

  ngOnDestroy() {
    this.destroyWidget();
  }

  onDataUpdated(_subscription: IWidgetSubscription, _detectChanges: boolean): void {
    if (this.widgetComponent) {
      this.widgetComponent.onDataUpdated();
    }
  }

  onLatestDataUpdated(_subscription: IWidgetSubscription, _detectChanges: boolean): void {
    if (this.widgetComponent && this.widgetComponent.onLatestDataUpdated) {
      this.widgetComponent.onLatestDataUpdated();
    }
  }

  private destroyWidget() {
    if (this.widgetContext) {
      this.reportWidgetContextService.destroyWidgetContext(this.widgetContext);
      this.widgetContext = null;
    }
    if (this.widgetComponentRef) {
      this.widgetComponentRef.destroy();
      this.widgetComponentRef = null;
      this.widgetComponent = null;
    }
  }

  private updateTimeSeriesWidgetPreview() {
    this.destroyWidget();
    if (this.widgetContainer) {
      this.widgetContainer.clear();
    }
    if (!this.hasData) {
      return;
    }
    const datasources = deepClone(this.reportComponent.dataSources || []);

    const defaultSettings = this.getDefaultSettings();

    const settings: ReportTimeSeriesChartSettings =
      mergeDeepIgnoreArray<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, defaultSettings, this.reportComponent.timeSeriesChartSettings, {
        barWidthSettings: defaultSettings.barWidthSettings,
        dataZoom: false,
        animation: {
          animation: false
        }
      } as ReportTimeSeriesChartSettings);
    (settings as TimeSeriesChartWidgetSettings).padding = '0';
    (settings as TimeSeriesChartWidgetSettings).background = {
      type: BackgroundType.color,
      color: 'rgba(0,0,0,0)',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    };
    if (settings.thresholds?.length) {
      for (const threshold of settings.thresholds) {
        if (threshold.type === ValueSourceType.entity) {
          threshold.type = ValueSourceType.latestKey;
          threshold.latestKeyType = threshold.entityKeyType;
          threshold.latestKey = threshold.entityKey;
          if (datasources.length) {
            const datasource = datasources[0];
            if (!datasource.latestDataKeys) {
              datasource.latestDataKeys = [];
            }
            let dataKey = datasource.latestDataKeys.find(d => d.type === threshold.latestKeyType && d.name === threshold.latestKey);
            if (!dataKey) {
              dataKey = {
                type: threshold.latestKeyType,
                name: threshold.latestKey,
                label: threshold.latestKey
              };
              datasource.latestDataKeys.push(dataKey);
            }
          }
        }
      }
    }
    let genDataFunc: GenerateDataFunction;
    if (this.chartType == TimeSeriesChartType.state) {
      const states = settings.states || [];
      const values = states.map(s => {
        if (s.sourceType === TimeSeriesChartStateSourceType.constant) {
          return s.sourceValue;
        } else if (s.sourceType === TimeSeriesChartStateSourceType.range){
          const from = s.sourceRangeFrom ?? 0;
          const to = s.sourceRangeTo ?? 0;
          return (to - from) / 2;
        }
      });
      if (values.length) {
        genDataFunc = (random, time) => {
          const index = Math.round((values.length - 1) * random());
          return values[index];
        }
      }
    }
    let widgetSettings: any = settings;
    let units = '';
    let decimals = 2;
    let widgetComponentType: Type<ChartWidgetComponent> = TimeSeriesChartWidgetComponent;
    if (this.barChartWithLabels) {
      const barChartWithLabelsSettings = settings as ReportBarChartWithLabelSettings & TimeSeriesChartWidgetSettings;
      widgetSettings = toBarChartWithLabelsWidgetSettings(barChartWithLabelsSettings);
      units = barChartWithLabelsSettings.barUnits;
      decimals = barChartWithLabelsSettings.barDecimals;
      widgetComponentType = BarChartWithLabelsWidgetComponent;
    } else if (this.rangeChart) {
      const rangeChartSettings = settings as ReportRangeChartSettings & TimeSeriesChartWidgetSettings;
      widgetSettings = toRangeChartWidgetSettings(rangeChartSettings);
      units = rangeChartSettings.rangeUnits;
      decimals = rangeChartSettings.rangeDecimals;
      widgetComponentType = RangeChartWidgetComponent;
    }
    this.reportWidgetContextService.createWidgetContext(widgetType.timeseries,
      widgetSettings, this.reportComponent.timewindow, datasources, units, decimals, this.chartType == TimeSeriesChartType.state, this, genDataFunc)
    .subscribe((ctx) => {
      this.widgetContext = ctx;
      this.widgetComponentRef = this.widgetContainer.createComponent(widgetComponentType);
      this.widgetContext.$container = $(this.widgetComponentRef.location.nativeElement);
      this.widgetContext.$containerParent = ctx.$container.parent();
      this.widgetComponent = this.widgetComponentRef.instance;
      this.widgetComponent.reportMode = true;
      this.widgetComponent.ctx = this.widgetContext;
      this.widgetContext.defaultSubscription.subscribe();
    });
  }

  private getDefaultSettings(): ReportTimeSeriesChartSettings {
    if (this.chartType === TimeSeriesChartType.state) {
      return reportStateChartDefaultSettings;
    } else if (this.barChartWithLabels) {
      return reportBarChartWithLabelsDefaultSettings;
    } else if (this.rangeChart) {
      return reportRangeChartDefaultSettings;
    } else {
      return reportTimeSeriesChartDefaultSettings;
    }
  }

}
