// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IncludeCustomersTableHeaderComponent } from '@home/components/entity/include-customers-table-header.component';
import { ReportTemplateFilter, ReportTemplateInfo } from '@shared/models/report.models';

@Component({
    selector: 'tb-report-template-table-header',
    templateUrl: './report-template-table-header.component.html',
    standalone: false
})
export class ReportTemplateTableHeaderComponent extends IncludeCustomersTableHeaderComponent<ReportTemplateInfo> {

  constructor(protected store: Store<AppState>) {
    super();
  }

  reportTemplateFilterChanged(filter: ReportTemplateFilter) {
    this.entitiesTableConfig.componentsData.reportTemplateFilterChanged(filter);
  }
}
