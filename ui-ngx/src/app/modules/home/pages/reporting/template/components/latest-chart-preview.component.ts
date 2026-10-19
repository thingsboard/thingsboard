// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  inject,
  OnDestroy,
  Type,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import {
  LatestChartReportComponentConfig,
  reportBarChartDefaultSettings,
  ReportBarChartSettings, reportDoughnutChartDefaultSettings, ReportDoughnutChartSettings,
  reportLatestChartDefaultSettings,
  ReportLatestChartSettings, reportPieChartDefaultSettings, ReportPieChartSettings
} from '@shared/models/report-component.models';
import { IWidgetSubscription, WidgetSubscriptionCallbacks } from '@core/api/widget-api.models';
import { ReportWidgetContextService } from '@home/pages/reporting/template/components/report-widget-context.service';
import { BackgroundType, ComponentStyle, constantColor, textStyle } from '@shared/models/widget-settings.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { ChartWidgetComponent } from '@home/components/widget/lib/chart/chart.models';
import { debounce, deepClone, mergeDeep } from '@core/utils';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { LatestChartWidgetSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import { BarChartWidgetComponent } from '@home/components/widget/lib/chart/bar-chart-widget.component';
import { PieChartWidgetComponent } from '@home/components/widget/lib/chart/pie-chart-widget.component';
import { DoughnutWidgetSettings } from '@home/components/widget/lib/chart/doughnut-widget.models';
import { DoughnutWidgetComponent } from '@home/components/widget/lib/chart/doughnut-widget.component';
import { reportComponentTypesData } from '@home/pages/reporting/template/components/report-component.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-latest-chart-preview',
    templateUrl: './latest-chart-preview.component.html',
    styleUrls: ['./latest-chart-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LatestChartPreviewComponent extends AbstractReportComponentPreview<LatestChartReportComponentConfig>
  implements AfterViewInit, OnDestroy, WidgetSubscriptionCallbacks {

  @ViewChild('widgetContent', {read: ViewContainerRef, static: false}) widgetContainer: ViewContainerRef;

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
    this.updateLatestWidgetPreview();
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

    this.showTitle = this.reportComponent.latestChartSettings.showTitle;
    this.title = this.reportComponent.latestChartSettings.title;
    this.titleStyle = textStyle(this.reportComponent.latestChartSettings.titleFont);
    this.titleStyle.color = this.reportComponent.latestChartSettings.titleColor;
    this.titleStyle.textAlign = this.reportComponent.latestChartSettings.titleAlignment;
    this.chartTypeTitle = this.translate.instant(reportComponentTypesData.getReportComponentTypeData(this.reportComponent.type, this.reportComponent.subType).title);

    const datasources = this.reportComponent.dataSources;
    if (datasources?.length) {
      const datasource = datasources[0];
      if (datasource.type === DatasourceType.device && datasource.deviceId || datasource.type === DatasourceType.entity && datasource.entityAliasId) {
        if (datasource.dataKeys?.length) {
          this.hasData = true;
        } else {
          this.hasData = false;
          this.noDataMessage = 'report-template.component.latest-chart.no-series-configured';
        }
      } else {
        this.hasData = false;
        this.noDataMessage = 'report-template.component.latest-chart.no-datasource-configured';
      }
    } else {
      this.hasData = false;
      this.noDataMessage = 'report-template.component.latest-chart.no-datasource-configured';
    }

    if (this.viewInited) {
      this.updateWidgetPreview();
    }
  }

  ngAfterViewInit() {
    this.viewInited = true;
    this.updateLatestWidgetPreview();
  }

  ngOnDestroy() {
    this.destroyWidget();
  }

  onDataUpdated(_subscription: IWidgetSubscription, _detectChanges: boolean): void {
    if (this.widgetComponent) {
      this.widgetComponent.onDataUpdated();
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

  private updateLatestWidgetPreview() {
    this.destroyWidget();
    if (this.widgetContainer) {
      this.widgetContainer.clear();
    }
    if (!this.hasData) {
      return;
    }
    const datasources = deepClone(this.reportComponent.dataSources || []);


    const widgetSettings = this.prepareWidgetSettings();
    const units = this.reportComponent.latestChartSettings.units;
    const decimals = this.reportComponent.latestChartSettings.decimals;

    let widgetComponentType: Type<ChartWidgetComponent>;
    const subType = this.reportComponent.subType;
    if ('latestBarChart' === subType) {
      widgetComponentType = BarChartWidgetComponent;
    } else if ('pieChart' === subType) {
      widgetComponentType = PieChartWidgetComponent;
    } else if ('doughnutChart' === subType || 'horizontalDoughnutChart' === subType) {
      widgetComponentType = DoughnutWidgetComponent;
    }

    if (widgetComponentType) {
      this.reportWidgetContextService.createWidgetContext(widgetType.latest,
        widgetSettings, null, datasources, units, decimals, false, this, null)
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
  }

  private prepareWidgetSettings(): any {
    const subType = this.reportComponent.subType;
    let latestChartSettings: ReportLatestChartSettings;
    if ('latestBarChart' === subType) {
      latestChartSettings = mergeDeep<ReportBarChartSettings>({} as ReportBarChartSettings, reportBarChartDefaultSettings, this.reportComponent.latestChartSettings as ReportBarChartSettings);
    } else if ('pieChart' === subType) {
      latestChartSettings = mergeDeep<ReportPieChartSettings>({} as ReportPieChartSettings, reportPieChartDefaultSettings, this.reportComponent.latestChartSettings as ReportPieChartSettings);
    } else if ('doughnutChart' === subType || 'horizontalDoughnutChart' === subType) {
      latestChartSettings = mergeDeep<ReportDoughnutChartSettings>({} as ReportDoughnutChartSettings, reportDoughnutChartDefaultSettings('horizontalDoughnutChart' === subType),
        this.reportComponent.latestChartSettings as ReportDoughnutChartSettings);
      (latestChartSettings as DoughnutWidgetSettings).totalValueColor = constantColor((latestChartSettings as ReportDoughnutChartSettings).totalValueColor);
    } else {
      latestChartSettings = mergeDeep<ReportLatestChartSettings>({} as ReportLatestChartSettings, reportLatestChartDefaultSettings, this.reportComponent.latestChartSettings as ReportLatestChartSettings);
    }
    return mergeDeep<LatestChartWidgetSettings>(
      {} as LatestChartWidgetSettings,
      latestChartSettings as LatestChartWidgetSettings,
      {
        animation: {
          animation: false
        },
        padding: '0',
        background: {
          type: BackgroundType.color,
          color: 'rgba(0,0,0,0)',
          overlay: {
            enabled: false,
            color: 'rgba(255,255,255,0.72)',
            blur: 3
          }
        }
      } as LatestChartWidgetSettings
    );
  }
}
