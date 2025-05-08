///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, DestroyRef, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AbstractControl, UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  FormGroupDirective,
  NgForm,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {
  EntityKeyValueType,
  Filter, FilterPredicateValue,
  filterToUserFilterInfoList,
  UserFilterInputInfo
} from '@shared/models/query/query.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

  userFilterFormGroup: UntypedFormGroup;

  valueTypeEnum = EntityKeyValueType;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: UserFilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<UserFilterDialogComponent, Filter>,
              private fb: UntypedFormBuilder,
              public translate: TranslateService,
              private destroyRef: DestroyRef) {
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
    const predicateValue: FilterPredicateValue<string | number | boolean> = (userInput.info.keyFilterPredicate as any).value;
    const value = isDefinedAndNotNull(predicateValue.userValue) ? predicateValue.userValue : predicateValue.defaultValue;
    const userInputControl = this.fb.group({
      label: [userInput.label],
      valueType: [userInput.valueType],
      value: [value,
        userInput.valueType === EntityKeyValueType.NUMERIC ||
        userInput.valueType === EntityKeyValueType.DATE_TIME  ? [Validators.required] : []]
    });
    userInputControl.get('value').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(userValue => {
      (userInput.info.keyFilterPredicate as any).value.userValue = userValue;
    });
    return userInputControl;
  }

  userInputsFormArray(): UntypedFormArray {
    return this.userFilterFormGroup.get('userInputs') as UntypedFormArray;
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
    this.dialogRef.close(this.filter);
  }
}
