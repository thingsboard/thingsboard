///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { DashboardService } from '@core/http/dashboard.service';
import { forkJoin, Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export type ManageDashboardCustomersActionType = 'assign' | 'manage' | 'unassign';

export interface ManageDashboardCustomersDialogData {
  actionType: ManageDashboardCustomersActionType;
  dashboardIds: Array<string>;
  assignedCustomersIds?: Array<string>;
}

@Component({
  selector: 'tb-manage-dashboard-customers-dialog',
  templateUrl: './manage-dashboard-customers-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDashboardCustomersDialogComponent}],
  styleUrls: []
})
export class ManageDashboardCustomersDialogComponent extends
  DialogComponent<ManageDashboardCustomersDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  dashboardCustomersFormGroup: UntypedFormGroup;

  submitted = false;

  entityType = EntityType;

  titleText: string;
  labelText: string;
  actionName: string;

  assignedCustomersIds: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDashboardCustomersDialogData,
              private dashboardService: DashboardService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageDashboardCustomersDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.assignedCustomersIds = data.assignedCustomersIds || [];
    switch (data.actionType) {
      case 'assign':
        this.titleText = 'dashboard.assign-to-customers';
        this.labelText = 'dashboard.assign-to-customers-text';
        this.actionName = 'action.assign';
        break;
      case 'manage':
        this.titleText = 'dashboard.manage-assigned-customers';
        this.labelText = 'dashboard.assigned-customers';
        this.actionName = 'action.update';
        break;
      case 'unassign':
        this.titleText = 'dashboard.unassign-from-customers';
        this.labelText = 'dashboard.unassign-from-customers-text';
        this.actionName = 'action.unassign';
        break;
    }
  }

  ngOnInit(): void {
    this.dashboardCustomersFormGroup = this.fb.group({
      assignedCustomerIds: [[...this.assignedCustomersIds]]
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  submit(): void {
    this.submitted = true;
    const customerIds: Array<string> = this.dashboardCustomersFormGroup.get('assignedCustomerIds').value;
    const tasks: Observable<any>[] = [];

    this.data.dashboardIds.forEach(
      (dashboardId) => {
        tasks.push(this.getManageDashboardCustomersTask(dashboardId, customerIds));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

  private getManageDashboardCustomersTask(dashboardId: string, customerIds: Array<string>): Observable<any> {
    switch (this.data.actionType) {
      case 'assign':
        return this.dashboardService.addDashboardCustomers(dashboardId, customerIds);
      case 'manage':
        return this.dashboardService.updateDashboardCustomers(dashboardId, customerIds);
      case 'unassign':
        return this.dashboardService.removeDashboardCustomers(dashboardId, customerIds);
    }
  }
}
