// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    standalone: false
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
