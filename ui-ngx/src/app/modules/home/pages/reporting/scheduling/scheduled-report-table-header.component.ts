// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IncludeCustomersTableHeaderComponent } from '@home/components/entity/include-customers-table-header.component';
import { ReportFilter, ScheduledReportInfo } from '@shared/models/report.models';

@Component({
    selector: 'tb-scheduled-report-table-header',
    templateUrl: './scheduled-report-table-header.component.html',
    standalone: false
})
export class ScheduledReportTableHeaderComponent extends IncludeCustomersTableHeaderComponent<ScheduledReportInfo> {

  constructor(protected store: Store<AppState>) {
    super();
  }

  reportFilterChanged(filter: ReportFilter) {
    this.entitiesTableConfig.componentsData.reportFilterChanged(filter);
  }
}
