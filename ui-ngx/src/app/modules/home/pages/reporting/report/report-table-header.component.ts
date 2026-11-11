// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IncludeCustomersTableHeaderComponent } from '@home/components/entity/include-customers-table-header.component';
import { ReportFilter, ReportInfo } from '@shared/models/report.models';

@Component({
    selector: 'tb-report-table-header',
    templateUrl: './report-table-header.component.html',
    standalone: false
})
export class ReportTableHeaderComponent extends IncludeCustomersTableHeaderComponent<ReportInfo> {

  constructor(protected store: Store<AppState>) {
    super();
  }

  reportFilterChanged(filter: ReportFilter) {
    this.entitiesTableConfig.componentsData.reportFilterChanged(filter);
  }
}
