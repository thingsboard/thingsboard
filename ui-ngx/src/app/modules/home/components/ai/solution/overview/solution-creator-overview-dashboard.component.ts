// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterContentInit, Component, EventEmitter, HostBinding, Input, OnChanges, Output } from "@angular/core";
import { DashboardsOverview } from '@shared/models/solution-creator.models';
import { FormBuilder } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-solution-creator-overview-dashboard',
  templateUrl: './solution-creator-overview-dashboard.component.html',
  styleUrls: ['./solution-creator-overview.scss'],
  standalone: false
})
export class SolutionCreatorOverviewDashboardComponent implements OnChanges, AfterContentInit {

  @Input({required: true})
  dashboards: DashboardsOverview[];

  @Input({required: true})
  solutionName: string;

  @Output()
  updatedDashboards: EventEmitter<DashboardsOverview[]> = new EventEmitter<DashboardsOverview[]>();

  @HostBinding('class.entering')
  initComponent = false;

  dashboard = this.fb.control([''], {nonNullable: true});
  dashboardDescriptor = this.fb.control<DashboardsOverview[]>(null);
  solutionMode = this.fb.control('basic');
  description: string;

  constructor(private fb: FormBuilder,
              ) {
    this.dashboard.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      const dashboard = this.dashboards.find(item => item.name === value[0]);
      this.description = dashboard?.description ?? 'Not found';
    })

    this.solutionMode.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value === 'advanced') {
        this.dashboardDescriptor.setValue(deepClone(this.dashboards));
        this.dashboardDescriptor.markAsPristine();
      }
    })
  }

  ngOnChanges(): void {
    if (this.dashboards.length) {
      if (!this.dashboards.find(item => item.name === this.dashboard.value[0])) {
        this.dashboard.setValue([this.dashboards[0]?.name]);
      }
      if (this.dashboardDescriptor.pristine) {
        this.dashboardDescriptor.setValue(deepClone(this.dashboards));
      }
    }
  }

  ngAfterContentInit() {
    requestAnimationFrame(() => {
      this.initComponent = true;
    })
  }

  updatedDashboardsDescriptor($event: MouseEvent) {
    $event?.stopPropagation();
    this.dashboardDescriptor.markAsPristine();
    this.updatedDashboards.emit(this.dashboardDescriptor.value);
  }
}
