///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  AbstractControl, FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {
  EntityKeyValueType,
  Filter,
  filterToUserFilterInfoList,
  UserFilterInputInfo
} from '@shared/models/query/query.models';

export interface UserFilterDialogData {
  filter: Filter;
}

@Component({
  selector: 'tb-user-filter-dialog',
  templateUrl: './user-filter-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: UserFilterDialogComponent}],
  styleUrls: []
})
export class UserFilterDialogComponent extends DialogComponent<UserFilterDialogComponent, Filter>
  implements OnInit, ErrorStateMatcher {

  filter: Filter;

  userFilterFormGroup: FormGroup;

  valueTypeEnum = EntityKeyValueType;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: UserFilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<UserFilterDialogComponent, Filter>,
              private fb: FormBuilder,
              public translate: TranslateService) {
    super(store, router, dialogRef);
    this.filter = data.filter;
    const userInputs = filterToUserFilterInfoList(this.filter, translate);

    const userInputControls: Array<AbstractControl> = [];
    for (const userInput of userInputs) {
      userInputControls.push(this.createUserInputFormControl(userInput));
    }

    this.userFilterFormGroup = this.fb.group({
      userInputs: this.fb.array(userInputControls)
    });
  }

  private createUserInputFormControl(userInput: UserFilterInputInfo): AbstractControl {
    const userInputControl = this.fb.group({
      label: [userInput.label],
      valueType: [userInput.valueType],
      value: [(userInput.info.keyFilterPredicate as any).value,
        userInput.valueType === EntityKeyValueType.NUMERIC ||
        userInput.valueType === EntityKeyValueType.DATE_TIME  ? [Validators.required] : []]
    });
    userInputControl.get('value').valueChanges.subscribe(value => {
      (userInput.info.keyFilterPredicate as any).value = value;
    });
    return userInputControl;
  }

  userInputsFormArray(): FormArray {
    return this.userFilterFormGroup.get('userInputs') as FormArray;
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.filter);
  }
}
