// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { IncludeCustomersTableHeaderComponent } from '@home/components/entity/include-customers-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Job, JobFilter, TaskManagerConfig } from '@shared/models/job.models';
import { TimePageLink } from '@shared/models/page/page-link';

@Component({
    selector: 'tb-task-manager-table-header',
    templateUrl: './task-manager-header.component.html',
    styleUrls: ['./task-manager-header.component.scss'],
    standalone: false
})
export class TaskManagerHeaderComponent extends IncludeCustomersTableHeaderComponent<Job, TimePageLink> {

  get taskManagerTableConfig(): TaskManagerConfig {
    return this.entitiesTableConfig as TaskManagerConfig;
  }

  constructor(protected store: Store<AppState>) {
    super();
  }

  taskManagerChanged(jobFilter: JobFilter) {
    this.taskManagerTableConfig.componentsData.filter = jobFilter;
    this.taskManagerTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
