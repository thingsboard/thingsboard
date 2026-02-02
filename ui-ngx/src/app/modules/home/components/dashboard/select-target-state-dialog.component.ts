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
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

export interface SelectTargetStateDialogData {
  states: {[id: string]: DashboardState };
}

@Component({
  selector: 'tb-select-target-state-dialog',
  templateUrl: './select-target-state-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: SelectTargetStateDialogComponent}],
  styleUrls: []
})
export class SelectTargetStateDialogComponent extends
  DialogComponent<SelectTargetStateDialogComponent, string>
  implements OnInit, ErrorStateMatcher {

  states: {[id: string]: DashboardState };
  stateFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SelectTargetStateDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<SelectTargetStateDialogComponent, string>,
              private fb: UntypedFormBuilder,
              private dashboardUtils: DashboardUtilsService) {
    super(store, router, dialogRef);

    this.states = this.data.states;

    this.stateFormGroup = this.fb.group(
      {
        stateId: [this.dashboardUtils.getRootStateId(this.states), [Validators.required]]
      }
    );
  }

  ngOnInit(): void {
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const stateId: string = this.stateFormGroup.get('stateId').value;
    this.dialogRef.close(stateId);
  }
}
