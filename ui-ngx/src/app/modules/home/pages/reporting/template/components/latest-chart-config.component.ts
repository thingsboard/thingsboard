// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import {
  imageAlignments, imageAlignmentTranslations, imageWidthTypeTranslations,
  LatestChartReportComponentConfig,
  reportBarChartDefaultSettings,
  ReportBarChartSettings, reportDoughnutChartDefaultSettings, ReportDoughnutChartSettings,
  ReportLatestChartSettings, reportPieChartDefaultSettings, ReportPieChartSettings
} from '@shared/models/report-component.models';
import { FormGroup, UntypedFormGroup, Validators } from '@angular/forms';
import { formatValue, mergeDeep } from '@core/utils';
import {
  DataKey,
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetConfig,
  WidgetConfigMode,
  widgetType,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { merge } from 'rxjs';
import { WidgetInfo, WidgetWithInfo } from '@home/models/widget-component.models';
import { pieChartLabelPositions, pieChartLabelPositionTranslations } from '@home/components/widget/lib/chart/chart.models';
import {
  DoughnutLayout, doughnutLayoutImages,
  doughnutLayouts,
  doughnutLayoutTranslations,
  horizontalDoughnutLayoutImages
} from '@home/components/widget/lib/chart/doughnut-widget.models';

@Component({
    selector: 'tb-latest-chart-config',
    templateUrl: './latest-chart-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class LatestChartConfigComponent extends AbstractReportComponentConfig<LatestChartReportComponentConfig> {

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  pieChartLabelPositions = pieChartLabelPositions;

  pieChartLabelPositionTranslationMap = pieChartLabelPositionTranslations;

  doughnutHorizontal = false;

  doughnutLayouts = doughnutLayouts;

  doughnutLayoutTranslationMap = doughnutLayoutTranslations;

  doughnutLayoutImageMap: Map<DoughnutLayout, string>;

  get doughnutTotalEnabled(): boolean {
    if (this.reportConfigForm?.contains('doughnutLayout')) {
      const layout: DoughnutLayout = this.reportConfigForm.get('doughnutLayout').value;
      return layout === DoughnutLayout.with_total;
    }
    return false;
  }

  imageWidthTypes = ['fitWidth', 'custom'];
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  subType: string;

  public get widget(): WidgetWithInfo {
    return {
      type: widgetType.timeseries,
      config: {
        settings: this.reportComponentConfig.latestChartSettings
      } as WidgetConfig,
      widgetInfo: {
        typeParameters: {
        } as WidgetTypeParameters
      } as WidgetInfo
    } as WidgetWithInfo;
  }

  valuePreviewFn = this._valuePreviewFn.bind(this);

  basicMode = WidgetConfigMode.basic;

  settingsTab: 'data' | 'layout' = 'data';

  protected buildForm(reportComponentConfig: LatestChartReportComponentConfig): FormGroup {
    this.subType = reportComponentConfig.subType;
    let latestChartSettings: ReportLatestChartSettings;
    if ('latestBarChart' === this.subType) {
      latestChartSettings = mergeDeep<ReportBarChartSettings>({} as ReportBarChartSettings, reportBarChartDefaultSettings, reportComponentConfig.latestChartSettings as ReportBarChartSettings);
    } else if ('pieChart' === this.subType) {
      latestChartSettings = mergeDeep<ReportPieChartSettings>({} as ReportPieChartSettings, reportPieChartDefaultSettings, reportComponentConfig.latestChartSettings as ReportPieChartSettings);
    } else if ('doughnutChart' === this.subType) {
      latestChartSettings = mergeDeep<ReportDoughnutChartSettings>({} as ReportDoughnutChartSettings, reportDoughnutChartDefaultSettings(false),
        reportComponentConfig.latestChartSettings as ReportDoughnutChartSettings);
      this.doughnutLayoutImageMap = doughnutLayoutImages;
    } else if ('horizontalDoughnutChart' === this.subType) {
      latestChartSettings = mergeDeep<ReportDoughnutChartSettings>({} as ReportDoughnutChartSettings, reportDoughnutChartDefaultSettings(true),
        reportComponentConfig.latestChartSettings as ReportDoughnutChartSettings);
      this.doughnutHorizontal = true;
      this.doughnutLayoutImageMap = horizontalDoughnutLayoutImages;
    }
    const form: UntypedFormGroup = this.fb.group({
      dataSources: [reportComponentConfig.dataSources, []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      height: [reportComponentConfig.height || 400, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []],

      series: [this.getSeries(reportComponentConfig.dataSources), []],

      showTitle: [latestChartSettings.showTitle, []],
      title: [latestChartSettings.title, []],
      titleFont: [latestChartSettings.titleFont, []],
      titleColor: [latestChartSettings.titleColor, []],
      titleAlignment: [latestChartSettings.titleAlignment, []],

      sortSeries: [latestChartSettings.sortSeries, []],

      units: [latestChartSettings.units, []],
      decimals: [latestChartSettings.decimals, []],

      showLegend: [latestChartSettings.showLegend, []],
      legendPosition: [latestChartSettings.legendPosition, []],
      legendLabelFont: [latestChartSettings.legendLabelFont, []],
      legendLabelColor: [latestChartSettings.legendLabelColor, []],
      legendValueFont: [latestChartSettings.legendValueFont, []],
      legendValueColor: [latestChartSettings.legendValueColor, []],
      legendShowTotal: [latestChartSettings.legendShowTotal, []],
    });

    if ('latestBarChart' === this.subType) {

      const barChartSettings = latestChartSettings as ReportBarChartSettings;

      form.addControl('barSettings', this.fb.control(barChartSettings.barSettings, []));

      form.addControl('axisMin', this.fb.control(barChartSettings.axisMin, []));
      form.addControl('axisMax', this.fb.control(barChartSettings.axisMax, []));
      form.addControl('axisTickLabelFont', this.fb.control(barChartSettings.axisTickLabelFont, []));
      form.addControl('axisTickLabelColor', this.fb.control(barChartSettings.axisTickLabelColor, []));
    } else if ('pieChart' === this.subType) {

      const pieChartSettings = latestChartSettings as ReportPieChartSettings;

      form.addControl('showLabel', this.fb.control(pieChartSettings.showLabel, []));
      form.addControl('labelPosition', this.fb.control(pieChartSettings.labelPosition, []));
      form.addControl('labelFont', this.fb.control(pieChartSettings.labelFont, []));
      form.addControl('labelColor', this.fb.control(pieChartSettings.labelColor, []));

      form.addControl('pieBorderWidth', this.fb.control(pieChartSettings.borderWidth, []));
      form.addControl('pieBorderColor', this.fb.control(pieChartSettings.borderColor, []));

      form.addControl('pieRadius', this.fb.control(pieChartSettings.radius, []));
      form.addControl('clockwise', this.fb.control(pieChartSettings.clockwise, []));

      form.get('showLabel').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators(form);
      });
    } else if ('doughnutChart' === this.subType || 'horizontalDoughnutChart' === this.subType) {

      const doughnutChartSettings = latestChartSettings as ReportDoughnutChartSettings;

      form.addControl('doughnutLayout', this.fb.control(doughnutChartSettings.layout, []));
      form.addControl('autoScale', this.fb.control(doughnutChartSettings.autoScale, []));
      form.addControl('clockwise', this.fb.control(doughnutChartSettings.clockwise, []));
      form.addControl('totalValueFont', this.fb.control(doughnutChartSettings.totalValueFont, []));
      form.addControl('totalValueColor', this.fb.control(doughnutChartSettings.totalValueColor, []));

      form.get('doughnutLayout').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators(form);
      });
    }

    form.get('widthType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });

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
    this.setSeries(config.series, config.dataSources);
    delete config.series;
    if (!config.latestChartSettings) {
      config.latestChartSettings = {};
    }
    const latestChartSettings: ReportLatestChartSettings = config.latestChartSettings;

    latestChartSettings.showTitle = config.showTitle;
    delete config.showTitle;
    latestChartSettings.title = config.title;
    delete config.title;
    latestChartSettings.titleFont = config.titleFont;
    delete config.titleFont;
    latestChartSettings.titleColor = config.titleColor;
    delete config.titleColor;
    latestChartSettings.titleAlignment = config.titleAlignment;
    delete config.titleAlignment;

    latestChartSettings.sortSeries = config.sortSeries;
    delete config.sortSeries;
    latestChartSettings.units = config.units;
    delete config.units;
    latestChartSettings.decimals = config.decimals;
    delete config.decimals;

    latestChartSettings.showLegend = config.showLegend;
    delete config.showLegend;

    latestChartSettings.legendPosition = config.legendPosition;
    delete config.legendPosition;
    latestChartSettings.legendLabelFont = config.legendLabelFont;
    delete config.legendLabelFont;
    latestChartSettings.legendLabelColor = config.legendLabelColor;
    delete config.legendLabelColor;
    latestChartSettings.legendValueFont = config.legendValueFont;
    delete config.legendValueFont;
    latestChartSettings.legendValueColor = config.legendValueColor;
    delete config.legendValueColor;
    latestChartSettings.legendShowTotal = config.legendShowTotal;
    delete config.legendShowTotal;

    if ('latestBarChart' === this.subType) {
      const barChartSettings = latestChartSettings as ReportBarChartSettings;

      barChartSettings.barSettings = config.barSettings;
      delete config.barSettings;

      barChartSettings.axisMin = config.axisMin;
      delete config.axisMin;
      barChartSettings.axisMax = config.axisMax;
      delete config.axisMax;
      barChartSettings.axisTickLabelFont = config.axisTickLabelFont;
      delete config.axisTickLabelFont;
      barChartSettings.axisTickLabelColor = config.axisTickLabelColor;
      delete config.axisTickLabelColor;
    } else if ('pieChart' === this.subType) {
      const pieChartSettings = latestChartSettings as ReportPieChartSettings;

      pieChartSettings.showLabel = config.showLabel;
      delete config.showLabel;
      pieChartSettings.labelPosition = config.labelPosition;
      delete config.labelPosition;
      pieChartSettings.labelFont = config.labelFont;
      delete config.labelFont;
      pieChartSettings.labelColor = config.labelColor;
      delete config.labelColor;
      pieChartSettings.borderWidth = config.pieBorderWidth;
      delete config.pieBorderWidth;
      pieChartSettings.borderColor = config.pieBorderColor;
      delete config.pieBorderColor;

      pieChartSettings.radius = config.pieRadius;
      delete config.pieRadius;
      pieChartSettings.clockwise = config.clockwise;
      delete config.clockwise;
    } else if ('doughnutChart' === this.subType || 'horizontalDoughnutChart' === this.subType) {
      const doughnutChartSettings = latestChartSettings as ReportDoughnutChartSettings;
      doughnutChartSettings.layout = config.doughnutLayout;
      delete config.doughnutLayout;
      doughnutChartSettings.autoScale = config.autoScale;
      delete config.autoScale;
      doughnutChartSettings.clockwise = config.clockwise;
      delete config.clockwise;
      doughnutChartSettings.totalValueFont = config.totalValueFont;
      delete config.totalValueFont;
      doughnutChartSettings.totalValueColor = config.totalValueColor;
      delete config.totalValueColor;
    }

    return config;
  }

  private updateCustomWidth() {
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = 200;
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }

  private getSeries(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setSeries(series: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = series;
    }
  }

  private updateValidators(form: FormGroup) {
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
      form.get('legendPosition').enable({emitEvent: false});
      form.get('legendLabelFont').enable({emitEvent: false});
      form.get('legendLabelColor').enable({emitEvent: false});
      form.get('legendValueFont').enable({emitEvent: false});
      form.get('legendValueColor').enable({emitEvent: false});
      form.get('legendShowTotal').enable({emitEvent: false});
    } else {
      form.get('legendPosition').disable({emitEvent: false});
      form.get('legendLabelFont').disable({emitEvent: false});
      form.get('legendLabelColor').disable({emitEvent: false});
      form.get('legendValueFont').disable({emitEvent: false});
      form.get('legendValueColor').disable({emitEvent: false});
      form.get('legendShowTotal').disable({emitEvent: false});
    }
    if (this.subType === 'pieChart') {
      const showLabel: boolean = form.get('showLabel').value;
      if (showLabel) {
        form.get('labelPosition').enable({emitEvent: false});
        form.get('labelFont').enable({emitEvent: false});
        form.get('labelColor').enable({emitEvent: false});
      } else {
        form.get('labelPosition').disable({emitEvent: false});
        form.get('labelFont').disable({emitEvent: false});
        form.get('labelColor').disable({emitEvent: false});
      }
    }
    if ('doughnutChart' === this.subType || 'horizontalDoughnutChart' === this.subType) {
      const layout: DoughnutLayout = form.get('doughnutLayout').value;
      const totalEnabled = layout === DoughnutLayout.with_total;
      if (totalEnabled) {
        form.get('totalValueFont').enable({emitEvent: false});
        form.get('totalValueColor').enable({emitEvent: false});
        form.get('legendShowTotal').disable({emitEvent: false});
      } else {
        form.get('totalValueFont').disable({emitEvent: false});
        form.get('totalValueColor').disable({emitEvent: false});
        form.get('legendShowTotal').enable({emitEvent: false});
      }
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.reportConfigForm.get('units').value;
    const decimals: number = this.reportConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }
}
