// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import { ReportTemplateRoutingModule } from '@home/pages/reporting/template/report-template-routing.module';
import {
  ReportTemplateTableHeaderComponent
} from '@home/pages/reporting/template/report-template-table-header.component';
import { ReportTemplateTabsComponent } from '@home/pages/reporting/template/report-template-tabs.component';
import { ReportTemplateFormComponent } from '@home/pages/reporting/template/report-template-form.component';
import {
  ReportTemplateSettingsDialogComponent
} from '@home/pages/reporting/template/report-template-settings-dialog.component';
import {
  GenerateReportDialogComponent
} from '@home/pages/reporting/template/generate-report-dialog.component';
import {
  ReportTemplateComponentsModule
} from '@home/pages/reporting/template/components/report-template-components.module';
import { ReportTemplateSettingsComponent } from '@home/pages/reporting/template/report-template-settings.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import { ReportTemplateFilterComponent } from '@home/pages/reporting/template/report-template-filter.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import {
  ReportTemplateHeaderFooterComponent
} from '@home/pages/reporting/template/report-template-header-footer.component';
import { NotificationBellModule } from '@home/components/notification/notification-bell.module';

@NgModule({
  declarations: [
    ReportTemplateFilterComponent,
    ReportTemplateTableHeaderComponent,
    ReportTemplateTabsComponent,
    ReportTemplateFormComponent,
    ReportTemplateHeaderFooterComponent,
    ReportTemplatePageComponent,
    ReportTemplateSettingsComponent,
    ReportTemplateSettingsDialogComponent,
    GenerateReportDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    NotificationBellModule,
    HomeDialogsModule,
    ReportTemplateComponentsModule,
    ReportTemplateRoutingModule,
    WidgetSettingsCommonModule,
    WidgetConfigComponentsModule
  ]
})
export class ReportTemplateModule { }
