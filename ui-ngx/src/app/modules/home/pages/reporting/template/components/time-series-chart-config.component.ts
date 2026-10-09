// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { FormGroup, UntypedFormGroup, Validators } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  imageAlignments,
  imageAlignmentTranslations,
  imageWidthTypeTranslations,
  reportBarChartWithLabelsDefaultSettings,
  ReportBarChartWithLabelSettings,
  ReportRangeChartSettings,
  reportTimeSeriesChartDefaultSettings,
  ReportTimeSeriesChartSettings,
  TimeseriesChartReportComponentConfig,
  toReportTimeSeriesChartKeySettings,
  toTimeSeriesChartKeySettings
} from '@shared/models/report-component.models';
import {
  DataKey,
  Datasource,
  WidgetConfig,
  WidgetConfigMode,
  widgetType,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import {
  normalizeAxisLimit,
  TimeSeriesChartKeySettings, TimeSeriesChartThreshold, TimeSeriesChartType,
  TimeSeriesChartYAxes,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { deepClone, mergeDeep } from '@core/utils';
import { merge } from 'rxjs';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import { WidgetInfo, WidgetWithInfo } from '@home/models/widget-component.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-time-series-chart-config',
    templateUrl: './time-series-chart-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TimeSeriesChartConfigComponent extends AbstractReportComponentConfig<TimeseriesChartReportComponentConfig> {

  @Input()
  @coerceBoolean()
  barChartWithLabels = false;

  @Input()
  @coerceBoolean()
  rangeChart = false;

  @Input()
  chartType: TimeSeriesChartType = TimeSeriesChartType.default;

  TimeSeriesChartType = TimeSeriesChartType;

  public get yAxisIds(): TimeSeriesChartYAxisId[] {
    if (this.barChartWithLabels || this.rangeChart) {
      return ['default'];
    } else {
      const yAxes: TimeSeriesChartYAxes = this.reportConfigForm.get('yAxes').value;
      return yAxes ? Object.keys(yAxes) : [];
    }
  }

  public get widget(): WidgetWithInfo {
    return {
      type: widgetType.timeseries,
      config: {
        settings: this.reportComponentConfig.timeSeriesChartSettings
      } as WidgetConfig,
      widgetInfo: {
        typeParameters: {
          chartType: this.chartType
        } as WidgetTypeParameters
      } as WidgetInfo
    } as WidgetWithInfo;
  }

  TbTimeSeriesChart = TbTimeSeriesChart;

  imageWidthTypes = ['fitWidth', 'custom'];
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  basicMode = WidgetConfigMode.basic;

  settingsTab: 'data' | 'layout' = 'data';

  seriesMode = 'series';

  protected buildForm(reportComponentConfig: TimeseriesChartReportComponentConfig): FormGroup {

    let timeSeriesChartSettings: ReportTimeSeriesChartSettings | ReportBarChartWithLabelSettings;
    if (this.barChartWithLabels) {
      timeSeriesChartSettings = mergeDeep<ReportBarChartWithLabelSettings>({} as ReportBarChartWithLabelSettings, reportBarChartWithLabelsDefaultSettings, reportComponentConfig.timeSeriesChartSettings);
    } else {
      timeSeriesChartSettings = mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings, reportComponentConfig.timeSeriesChartSettings);
    }

    if (this.barChartWithLabels || this.rangeChart) {
      timeSeriesChartSettings.yAxes.default.min = normalizeAxisLimit(reportComponentConfig.timeSeriesChartSettings.yAxes.default.min);
      timeSeriesChartSettings.yAxes.default.max = normalizeAxisLimit(reportComponentConfig.timeSeriesChartSettings.yAxes.default.max);
    }

    const form: UntypedFormGroup = this.fb.group({
      timewindow: [reportComponentConfig.timewindow, []],
      dataSources: [reportComponentConfig.dataSources, []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      height: [reportComponentConfig.height || 400, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []],

      thresholds: [timeSeriesChartSettings.thresholds, []],

      showTitle: [timeSeriesChartSettings.showTitle, []],
      title: [timeSeriesChartSettings.title, []],
      titleFont: [timeSeriesChartSettings.titleFont, []],
      titleColor: [timeSeriesChartSettings.titleColor, []],
      titleAlignment: [timeSeriesChartSettings.titleAlignment, []],

      grid: [timeSeriesChartSettings.grid, []],

      xAxis: [timeSeriesChartSettings.xAxis, []],

      noAggregationBarWidthSettings: [timeSeriesChartSettings.noAggregationBarWidthSettings, []],

      showLegend: [timeSeriesChartSettings.showLegend, []],
      legendLabelFont: [timeSeriesChartSettings.legendLabelFont, []],
      legendLabelColor: [timeSeriesChartSettings.legendLabelColor, []],
      legendConfig: [timeSeriesChartSettings.legendConfig, []]

    });

    if (!this.rangeChart) {
      form.addControl('series', this.fb.control(this.getSeries(reportComponentConfig.dataSources), []));
    }

    if (this.barChartWithLabels) {

      const barChartWithLabelSettings = timeSeriesChartSettings as ReportBarChartWithLabelSettings;

      form.addControl('barUnits', this.fb.control(barChartWithLabelSettings.barUnits, []));
      form.addControl('barDecimals', this.fb.control(barChartWithLabelSettings.barDecimals, []));

      form.addControl('yAxis', this.fb.control(barChartWithLabelSettings.yAxes['default'], []));

      form.addControl('showBarLabel', this.fb.control(barChartWithLabelSettings.showBarLabel, []));
      form.addControl('barLabelFont', this.fb.control(barChartWithLabelSettings.barLabelFont, []));
      form.addControl('barLabelColor', this.fb.control(barChartWithLabelSettings.barLabelColor, []));
      form.addControl('showBarValue', this.fb.control(barChartWithLabelSettings.showBarValue, []));
      form.addControl('barValueFont', this.fb.control(barChartWithLabelSettings.barValueFont, []));
      form.addControl('barValueColor', this.fb.control(barChartWithLabelSettings.barValueColor, []));
      form.addControl('showBarBorder', this.fb.control(barChartWithLabelSettings.showBarBorder, []));
      form.addControl('barBorderWidth', this.fb.control(barChartWithLabelSettings.barBorderWidth, []));
      form.addControl('barBorderRadius', this.fb.control(barChartWithLabelSettings.barBorderRadius, []))
      form.addControl('barBackgroundSettings', this.fb.control(barChartWithLabelSettings.barBackgroundSettings, []));

    } else if (this.rangeChart) {

      const rangeChartSettings = timeSeriesChartSettings as ReportRangeChartSettings;

      form.addControl('rangeUnits', this.fb.control(rangeChartSettings.rangeUnits, []));
      form.addControl('rangeDecimals', this.fb.control(rangeChartSettings.rangeDecimals, []));

      form.addControl('rangeColors', this.fb.control(rangeChartSettings.rangeColors, []));
      form.addControl('outOfRangeColor', this.fb.control(rangeChartSettings.outOfRangeColor, []));
      form.addControl('showRangeThresholds', this.fb.control(rangeChartSettings.showRangeThresholds, []));
      form.addControl('rangeThreshold', this.fb.control<Partial<TimeSeriesChartThreshold>>(rangeChartSettings.rangeThreshold, []));
      form.addControl('fillArea', this.fb.control(rangeChartSettings.fillArea, []));
      form.addControl('fillAreaOpacity', this.fb.control(rangeChartSettings.fillAreaOpacity, []));
      form.addControl('lineSettings', this.fb.control(rangeChartSettings.lineSettings, []));

      form.addControl('yAxis', this.fb.control(rangeChartSettings.yAxes['default'], []));

    } else {
      form.addControl('yAxes', this.fb.control(timeSeriesChartSettings.yAxes, []));
      form.addControl('comparisonEnabled', this.fb.control(timeSeriesChartSettings.comparisonEnabled, []));
      form.addControl('timeForComparison', this.fb.control(timeSeriesChartSettings.timeForComparison, []));
      form.addControl('comparisonCustomIntervalValue', this.fb.control(timeSeriesChartSettings.comparisonCustomIntervalValue, []));
      form.addControl('comparisonXAxis', this.fb.control(timeSeriesChartSettings.comparisonXAxis, []))
      form.addControl('stack', this.fb.control(timeSeriesChartSettings.stack, []));

      form.addControl('legendValueFont', this.fb.control(timeSeriesChartSettings.legendValueFont, []));
      form.addControl('legendValueColor', this.fb.control(timeSeriesChartSettings.legendValueColor, []));
      form.addControl('legendColumnTitleFont', this.fb.control(timeSeriesChartSettings.legendColumnTitleFont, []));
      form.addControl('legendColumnTitleColor', this.fb.control(timeSeriesChartSettings.legendColumnTitleColor, []));
    }

    if (this.chartType === TimeSeriesChartType.state) {
      form.addControl('states', this.fb.control(timeSeriesChartSettings.states, []));
    }

    form.get('widthType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });

    if (this.barChartWithLabels) {
      merge(form.get('showBarLabel').valueChanges,
            form.get('showBarValue').valueChanges,
            form.get('showBarBorder').valueChanges).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators(form);
      });
    } else if (this.rangeChart) {
      merge(form.get('showRangeThresholds').valueChanges,
        form.get('fillArea').valueChanges).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators(form);
      });
    } else {
      form.get('comparisonEnabled').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateSeriesState();
        this.updateValidators(form);
      });
    }

    merge(form.get('showTitle').valueChanges,
          form.get('showLegend').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(form);
    });

    this.updateValidators(form);

    return form;
  }

  protected prepareOutputConfig(config: any): any {
    if (!this.rangeChart) {
      this.setSeries(config.series, config.dataSources);
      delete config.series;
    }
    if (!config.timeSeriesChartSettings) {
      config.timeSeriesChartSettings = {};
    }
    const timeSeriesChartSettings: ReportTimeSeriesChartSettings = config.timeSeriesChartSettings

    timeSeriesChartSettings.thresholds = config.thresholds;
    delete config.thresholds;

    timeSeriesChartSettings.showTitle = config.showTitle;
    delete config.showTitle;
    timeSeriesChartSettings.title = config.title;
    delete config.title;
    timeSeriesChartSettings.titleFont = config.titleFont;
    delete config.titleFont;
    timeSeriesChartSettings.titleColor = config.titleColor;
    delete config.titleColor;
    timeSeriesChartSettings.titleAlignment = config.titleAlignment;
    delete config.titleAlignment;

    timeSeriesChartSettings.grid = config.grid;
    delete config.grid;

    timeSeriesChartSettings.xAxis = config.xAxis;
    delete config.xAxis;

    timeSeriesChartSettings.noAggregationBarWidthSettings = config.noAggregationBarWidthSettings;
    delete config.noAggregationBarWidthSettings;

    timeSeriesChartSettings.showLegend = config.showLegend;
    delete config.showLegend;

    timeSeriesChartSettings.legendLabelFont = config.legendLabelFont;
    delete config.legendLabelFont;
    timeSeriesChartSettings.legendLabelColor = config.legendLabelColor;
    delete config.legendLabelColor;

    timeSeriesChartSettings.legendConfig = config.legendConfig;
    delete config.legendConfig;

    if (this.barChartWithLabels) {

      const barChartWithLabelSettings = timeSeriesChartSettings as ReportBarChartWithLabelSettings;

      barChartWithLabelSettings.yAxes = {
        'default': config.yAxis
      };
      delete config.yAxis;

      barChartWithLabelSettings.barUnits = config.barUnits;
      delete config.barUnits;

      barChartWithLabelSettings.barDecimals = config.barDecimals;
      delete config.barDecimals;

      barChartWithLabelSettings.showBarLabel = config.showBarLabel;
      delete config.showBarLabel;
      barChartWithLabelSettings.barLabelFont = config.barLabelFont;
      delete config.barLabelFont;
      barChartWithLabelSettings.barLabelColor = config.barLabelColor;
      delete config.barLabelColor;
      barChartWithLabelSettings.showBarValue = config.showBarValue;
      delete config.showBarValue;
      barChartWithLabelSettings.barValueFont = config.barValueFont;
      delete config.barValueFont;
      barChartWithLabelSettings.barValueColor = config.barValueColor;
      delete config.barValueColor;

      barChartWithLabelSettings.showBarBorder = config.showBarBorder;
      delete config.showBarBorder;
      barChartWithLabelSettings.barBorderWidth = config.barBorderWidth;
      delete config.barBorderWidth;
      barChartWithLabelSettings.barBorderRadius = config.barBorderRadius;
      delete config.barBorderRadius;
      barChartWithLabelSettings.barBackgroundSettings = config.barBackgroundSettings;
      delete config.barBackgroundSettings;

    } else if (this.rangeChart) {

      const rangeChartSettings = timeSeriesChartSettings as ReportRangeChartSettings;

      rangeChartSettings.yAxes = {
        'default': config.yAxis
      };
      delete config.yAxis;

      rangeChartSettings.rangeUnits = config.rangeUnits;
      delete config.rangeUnits;

      rangeChartSettings.rangeDecimals = config.rangeDecimals;
      delete config.rangeDecimals;

      rangeChartSettings.rangeColors = config.rangeColors;
      delete config.rangeColors;
      rangeChartSettings.outOfRangeColor = config.outOfRangeColor;
      delete config.outOfRangeColor;
      rangeChartSettings.showRangeThresholds = config.showRangeThresholds;
      delete config.showRangeThresholds;
      rangeChartSettings.rangeThreshold = config.rangeThreshold;
      delete config.rangeThreshold;
      rangeChartSettings.fillArea = config.fillArea;
      delete config.fillArea;
      rangeChartSettings.fillAreaOpacity = config.fillAreaOpacity;
      delete config.fillAreaOpacity;

      rangeChartSettings.lineSettings = config.lineSettings;
      delete config.lineSettings;

    } else {
      timeSeriesChartSettings.yAxes = config.yAxes;
      delete config.yAxes;

      timeSeriesChartSettings.comparisonEnabled = config.comparisonEnabled;
      delete config.comparisonEnabled;
      timeSeriesChartSettings.timeForComparison = config.timeForComparison;
      delete config.timeForComparison;
      timeSeriesChartSettings.comparisonCustomIntervalValue = config.comparisonCustomIntervalValue;
      delete config.comparisonCustomIntervalValue;
      timeSeriesChartSettings.comparisonXAxis = config.comparisonXAxis;
      delete config.comparisonXAxis;

      timeSeriesChartSettings.stack = config.stack;
      delete config.stack;

      timeSeriesChartSettings.legendColumnTitleFont = config.legendColumnTitleFont;
      delete config.legendColumnTitleFont;
      timeSeriesChartSettings.legendColumnTitleColor = config.legendColumnTitleColor;
      delete config.legendColumnTitleColor;

      timeSeriesChartSettings.legendValueFont = config.legendValueFont;
      delete config.legendValueFont;
      timeSeriesChartSettings.legendValueColor = config.legendValueColor;
      delete config.legendValueColor;
    }

    if (this.chartType === TimeSeriesChartType.state) {
      timeSeriesChartSettings.states = config.states;
      delete config.states;
    }

    return config;
  }

  seriesModeChange(seriesMode: string) {
    this.seriesMode = seriesMode;
    this.updateSeriesState();
  }

  public yAxisRemoved(yAxisId: TimeSeriesChartYAxisId): void {
    if (this.reportComponentConfig.dataSources && this.reportComponentConfig.dataSources.length > 1) {
      for (let i = 1; i < this.reportComponentConfig.dataSources.length; i++) {
        const datasource = this.reportComponentConfig.dataSources[i];
        this.removeYaxisId(datasource.dataKeys, yAxisId);
      }
    }
  }

  private removeYaxisId(series: DataKey[], yAxisId: TimeSeriesChartYAxisId): boolean {
    let changed = false;
    if (series) {
      series.forEach(key => {
        const keySettings = ((key.settings || {}) as TimeSeriesChartKeySettings);
        if (keySettings.yAxisId === yAxisId) {
          keySettings.yAxisId = 'default';
          changed = true;
        }
      });
    }
    return changed;
  }

  private updateSeriesState() {
    if (this.seriesMode === 'series') {
      this.reportConfigForm.get('series').enable({emitEvent: false});
    } else {
      const comparisonEnabled = this.reportConfigForm.get('comparisonEnabled').value;
      if (comparisonEnabled) {
        this.reportConfigForm.get('series').enable({emitEvent: false});
      } else {
        this.reportConfigForm.get('series').disable({emitEvent: false});
      }
    }
  }

  private updateCustomWidth() {
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = 200;
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }

  private getSeries(datasources?: Datasource[]): DataKey[] {
    let dataKeys: DataKey[] = [];
    if (datasources && datasources.length) {
      dataKeys = datasources[0].dataKeys || [];
    }
    dataKeys = dataKeys.map(key => {
      key = deepClone(key);
      if (!this.barChartWithLabels) {
        key.settings = toTimeSeriesChartKeySettings(key.settings);
      }
      return key;
    });
    return dataKeys;
  }

  private setSeries(series: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      series = series.map(key => {
        key = deepClone(key);
        if (!this.barChartWithLabels) {
          key.settings = toReportTimeSeriesChartKeySettings(key.settings);
        }
        return key;
      });
      datasources[0].dataKeys = series;
    }
  }

  private updateValidators(form: FormGroup) {
    if (this.barChartWithLabels) {
      const showBarLabel: boolean = form.get('showBarLabel').value;
      const showBarValue: boolean = form.get('showBarValue').value;
      const showBarBorder: boolean = form.get('showBarBorder').value;
      if (showBarLabel) {
        form.get('barLabelFont').enable({emitEvent: false});
        form.get('barLabelColor').enable({emitEvent: false});
      } else {
        form.get('barLabelFont').disable({emitEvent: false});
        form.get('barLabelColor').disable({emitEvent: false});
      }

      if (showBarValue) {
        form.get('barValueFont').enable({emitEvent: false});
        form.get('barValueColor').enable({emitEvent: false});
      } else {
        form.get('barValueFont').disable({emitEvent: false});
        form.get('barValueColor').disable({emitEvent: false});
      }
      if (showBarBorder) {
        form.get('barBorderWidth').enable({emitEvent: false});
      } else {
        form.get('barBorderWidth').disable({emitEvent: false});
      }
    } else if (this.rangeChart) {
      const showRangeThresholds: boolean = form.get('showRangeThresholds').value;
      const fillArea: boolean = form.get('fillArea').value;
      if (showRangeThresholds) {
        form.get('rangeThreshold').enable({emitEvent: false});
      } else {
        form.get('rangeThreshold').disable({emitEvent: false});
      }
      if (fillArea) {
        form.get('fillAreaOpacity').enable({emitEvent: false});
      } else {
        form.get('fillAreaOpacity').disable({emitEvent: false});
      }
    } else {
      const comparisonEnabled: boolean = form.get('comparisonEnabled').value;
      if (comparisonEnabled) {
        form.get('timeForComparison').enable({emitEvent: false});
        form.get('comparisonCustomIntervalValue').enable({emitEvent: false});
        form.get('comparisonXAxis').enable({emitEvent: false});
      } else {
        form.get('timeForComparison').disable({emitEvent: false});
        form.get('comparisonCustomIntervalValue').disable({emitEvent: false});
        form.get('comparisonXAxis').disable({emitEvent: false});
      }
    }


    const showTitle: boolean = form.get('showTitle').value;
    const showLegend: boolean = form.get('showLegend').value;

    if (showTitle) {
      form.get('title').enable({emitEvent: false});
      form.get('titleFont').enable({emitEvent: false});
      form.get('titleColor').enable({emitEvent: false});
      form.get('titleAlignment').enable({emitEvent: false});
    } else {
      form.get('title').disable({emitEvent: false});
      form.get('titleFont').disable({emitEvent: false});
      form.get('titleColor').disable({emitEvent: false});
      form.get('titleAlignment').disable({emitEvent: false});
    }

    if (showLegend) {
      form.get('legendLabelFont').enable({emitEvent: false});
      form.get('legendLabelColor').enable({emitEvent: false});
      form.get('legendConfig').enable({emitEvent: false});
      if (!this.barChartWithLabels && !this.rangeChart) {
        form.get('legendColumnTitleFont').enable({emitEvent: false});
        form.get('legendColumnTitleColor').enable({emitEvent: false});
        form.get('legendValueFont').enable({emitEvent: false});
        form.get('legendValueColor').enable({emitEvent: false});
      }
    } else {
      form.get('legendLabelFont').disable({emitEvent: false});
      form.get('legendLabelColor').disable({emitEvent: false});
      form.get('legendConfig').disable({emitEvent: false});
      if (!this.barChartWithLabels && !this.rangeChart) {
        form.get('legendColumnTitleFont').disable({emitEvent: false});
        form.get('legendColumnTitleColor').disable({emitEvent: false});
        form.get('legendValueFont').disable({emitEvent: false});
        form.get('legendValueColor').disable({emitEvent: false});
      }
    }

  }
}
