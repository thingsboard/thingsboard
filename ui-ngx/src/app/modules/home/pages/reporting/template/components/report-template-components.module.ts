// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  EditReportComponentTooltipComponent,
  ReportComponentComponent
} from '@home/pages/reporting/template/components/report-component.component';
import { ReportComponentsComponent } from '@home/pages/reporting/template/components/report-components.component';
import { HeadingPreviewComponent } from '@home/pages/reporting/template/components/heading-preview.component';
import { RichTextPreviewComponent } from '@home/pages/reporting/template/components/rich-text-preview.component';
import {
  ReportComponentLibraryComponent
} from '@home/pages/reporting/template/components/report-component-library.component';
import {
  ReportComponentConfigComponent
} from '@home/pages/reporting/template/components/report-component-config.component';
import { HeadingConfigComponent } from '@home/pages/reporting/template/components/heading-config.component';
import { RichTextConfigComponent } from '@home/pages/reporting/template/components/rich-text-config.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import { ReportInsetsComponent } from '@home/pages/reporting/template/components/report-insets.component';
import { PageBreakPreviewComponent } from '@home/pages/reporting/template/components/page-break-preview.component';
import { EmptyReportConfigComponent } from '@home/pages/reporting/template/components/empty-report-config.component';
import { EntityTablePreviewComponent } from '@home/pages/reporting/template/components/entity-table-preview.component';
import { EntityTableConfigComponent } from '@home/pages/reporting/template/components/entity-table-config.component';
import { BasicWidgetConfigModule } from '@home/components/widget/config/basic/basic-widget-config.module';
import { SubReportPreviewComponent } from '@home/pages/reporting/template/components/sub-report-preview.component';
import { SubReportConfigComponent } from '@home/pages/reporting/template/components/sub-report-config.component';
import { ImagePreviewComponent } from '@home/pages/reporting/template/components/image-preview.component';
import { ImageConfigComponent } from '@home/pages/reporting/template/components/image-config.component';
import { ReportImageDialogComponent } from '@home/pages/reporting/template/components/report-image-dialog.component';
import { ReportRichTextComponent } from '@home/pages/reporting/template/components/report-rich-text.component';
import { DashboardPreviewComponent } from '@home/pages/reporting/template/components/dashboard-preview.component';
import { DashboardConfigComponent } from '@home/pages/reporting/template/components/dashboard-config.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { AlarmTablePreviewComponent } from '@home/pages/reporting/template/components/alarm-table-preview.component';
import { AlarmTableConfigComponent } from '@home/pages/reporting/template/components/alarm-table-config.component';
import {
  TimeseriesTablePreviewComponent
} from '@home/pages/reporting/template/components/timeseries-table-preview.component';
import {
  TimeseriesTableConfigComponent
} from '@home/pages/reporting/template/components/timeseries-table-config.component';
import { ReportHeadingComponent } from '@home/pages/reporting/template/components/report-heading.component';
import {
  ReportComponentLayoutSettingsComponent
} from '@home/pages/reporting/template/components/report-component-layout-settings.component';
import { DividerPreviewComponent } from '@home/pages/reporting/template/components/divider-preview.component';
import { DividerConfigComponent } from '@home/pages/reporting/template/components/divider-config.component';
import { TableSortOrderComponent } from '@home/pages/reporting/template/components/table-sort-order.component';
import {
  TimeSeriesChartPreviewComponent
} from '@home/pages/reporting/template/components/time-series-chart-preview.component';
import {
  TimeSeriesChartConfigComponent
} from '@home/pages/reporting/template/components/time-series-chart-config.component';
import { ReportWidgetContextService } from '@home/pages/reporting/template/components/report-widget-context.service';
import { LatestChartConfigComponent } from '@home/pages/reporting/template/components/latest-chart-config.component';
import { LatestChartPreviewComponent } from '@home/pages/reporting/template/components/latest-chart-preview.component';
import { WidgetSettingsModule } from '@home/components/widget/lib/settings/widget-settings.module';
import {
  ReportComponentLibraryGroupComponent
} from '@home/pages/reporting/template/components/report-component-library-group.component';
import {
  ReportComponentLibraryGroupsComponent
} from '@home/pages/reporting/template/components/report-component-library-groups.component';
import { SplitViewConfigComponent } from '@home/pages/reporting/template/components/split-view-config.component';
import { ReportDropBlockComponent } from '@home/pages/reporting/template/components/report-drop-block.component';
import { SplitViewPreviewComponent } from '@home/pages/reporting/template/components/split-view-preview.component';

@NgModule({
  providers: [
    ReportWidgetContextService
  ],
  declarations: [
    EditReportComponentTooltipComponent,
    ReportComponentComponent,
    ReportComponentsComponent,
    ReportComponentLibraryComponent,
    ReportComponentLibraryGroupComponent,
    ReportComponentLibraryGroupsComponent,
    ReportInsetsComponent,
    ReportComponentLayoutSettingsComponent,
    ReportHeadingComponent,
    TableSortOrderComponent,
    ReportImageDialogComponent,
    ReportRichTextComponent,
    EmptyReportConfigComponent,
    HeadingPreviewComponent,
    HeadingConfigComponent,
    RichTextPreviewComponent,
    RichTextConfigComponent,
    DividerPreviewComponent,
    DividerConfigComponent,
    PageBreakPreviewComponent,
    EntityTablePreviewComponent,
    EntityTableConfigComponent,
    AlarmTablePreviewComponent,
    AlarmTableConfigComponent,
    TimeseriesTablePreviewComponent,
    TimeseriesTableConfigComponent,
    ImagePreviewComponent,
    ImageConfigComponent,
    DashboardPreviewComponent,
    DashboardConfigComponent,
    SubReportPreviewComponent,
    SubReportConfigComponent,
    TimeSeriesChartPreviewComponent,
    TimeSeriesChartConfigComponent,
    LatestChartPreviewComponent,
    LatestChartConfigComponent,
    ReportComponentConfigComponent,
    ReportDropBlockComponent,
    SplitViewConfigComponent,
    SplitViewPreviewComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    WidgetConfigComponentsModule,
    BasicWidgetConfigModule,
    WidgetSettingsModule
  ],
  exports: [
    ReportComponentsComponent,
    ReportComponentLibraryComponent,
    ReportComponentLibraryGroupComponent,
    ReportComponentLibraryGroupsComponent,
    ReportComponentConfigComponent,
    ReportInsetsComponent
  ]
})
export class ReportTemplateComponentsModule { }
