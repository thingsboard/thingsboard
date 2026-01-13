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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { BreakpointId } from '@shared/models/dashboard.models';

export interface AddNewBreakpointDialogData {
  allowBreakpointIds: string[];
  selectedBreakpointIds: string[];
}

export interface AddNewBreakpointDialogResult {
  newBreakpointId: BreakpointId;
  copyFrom: BreakpointId;
}

@Component({
  selector: 'add-new-breakpoint-dialog',
  templateUrl: './add-new-breakpoint-dialog.component.html',
})
export class AddNewBreakpointDialogComponent extends DialogComponent<AddNewBreakpointDialogComponent, AddNewBreakpointDialogResult> {

  addBreakpointFormGroup: FormGroup;

  allowBreakpointIds = [];
  selectedBreakpointIds = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private fb: FormBuilder,
              @Inject(MAT_DIALOG_DATA) private data: AddNewBreakpointDialogData,
              protected dialogRef: MatDialogRef<AddNewBreakpointDialogComponent, AddNewBreakpointDialogResult>,
              private dashboardUtils: DashboardUtilsService,) {

    super(store, router, dialogRef);

    this.allowBreakpointIds = this.data.allowBreakpointIds;
    this.selectedBreakpointIds = this.data.selectedBreakpointIds;

    this.addBreakpointFormGroup = this.fb.group({
      newBreakpointId: [{value: this.allowBreakpointIds[0], disabled: this.allowBreakpointIds.length === 1}],
      copyFrom: [{value: 'default', disabled: this.selectedBreakpointIds.length === 1}],
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.addBreakpointFormGroup.getRawValue());
  }

  getName(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointName(breakpointId);
  }

  getIcon(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointIcon(breakpointId);
  }

  getSizeDescription(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointSizeDescription(breakpointId);
  }
}
