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
import {
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { DashboardStateInfo } from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component.models';
import { TranslateService } from '@ngx-translate/core';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface DashboardStateDialogData {
  states: {[id: string]: DashboardState };
  state: DashboardStateInfo;
  isAdd: boolean;
}

@Component({
  selector: 'tb-dashboard-state-dialog',
  templateUrl: './dashboard-state-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DashboardStateDialogComponent}],
  styleUrls: []
})
export class DashboardStateDialogComponent extends
  DialogComponent<DashboardStateDialogComponent, DashboardStateInfo>
  implements OnInit, ErrorStateMatcher {

  stateFormGroup: UntypedFormGroup;

  states: {[id: string]: DashboardState };
  state: DashboardStateInfo;
  prevStateId: string;

  stateIdTouched: boolean;

  isAdd: boolean;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardStateDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DashboardStateDialogComponent, DashboardStateInfo>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private dashboardUtils: DashboardUtilsService) {
    super(store, router, dialogRef);

    this.states = this.data.states;
    this.isAdd = this.data.isAdd;
    if (this.isAdd) {
      this.state = {id: '', ...this.dashboardUtils.createDefaultState('', false)};
      this.prevStateId = '';
    } else {
      this.state = this.data.state;
      this.prevStateId = this.state.id;
    }

    this.stateFormGroup = this.fb.group({
      name: [this.state.name, [Validators.required]],
      id: [this.state.id, [Validators.required, this.validateDuplicateStateId()]],
      root: [this.state.root, []],
    });

    this.stateFormGroup.get('name').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((name: string) => {
      this.checkStateName(name);
    });

    this.stateFormGroup.get('id').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((id: string) => {
      this.stateIdTouched = true;
    });
  }

  private checkStateName(name: string) {
    if (name && !this.stateIdTouched && this.isAdd) {
      this.stateFormGroup.get('id').setValue(
        name.toLowerCase().replace(/\W/g, '_'),
        { emitEvent: false }
      );
    }
  }

  private validateDuplicateStateId(): ValidatorFn {
    return (c: UntypedFormControl) => {
      const newStateId: string = c.value;
      if (newStateId) {
        const existing = this.states[newStateId];
        if (existing && newStateId !== this.prevStateId) {
          return {
            stateExists: true
          };
        }
      }
      return null;
    };
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
    this.state = {...this.state, ...this.stateFormGroup.value};
    this.state.id = this.state.id.trim();
    this.dialogRef.close(this.state);
  }
}
