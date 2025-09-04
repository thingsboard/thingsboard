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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { keyFilterInfosToKeyFilters, keyFiltersToKeyFilterInfos } from '@shared/models/query/query.models';
import { AlarmCondition, AlarmConditionType, AlarmConditionTypeTranslationMap } from '@shared/models/device.models';
import { TimeUnit, timeUnitTranslationMap } from '@shared/models/time/time.models';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface AlarmRuleConditionDialogData {
  readonly: boolean;
  condition: AlarmCondition;
  entityId?: EntityId;
}

@Component({
  selector: 'tb-alarm-rule-condition-dialog',
  templateUrl: './alarm-rule-condition-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AlarmRuleConditionDialogComponent}],
  styleUrls: ['./alarm-rule-condition-dialog.component.scss']
})
export class AlarmRuleConditionDialogComponent extends DialogComponent<AlarmRuleConditionDialogComponent, AlarmCondition>
  implements OnInit, ErrorStateMatcher {

  timeUnits = Object.values(TimeUnit);
  timeUnitTranslations = timeUnitTranslationMap;
  alarmConditionTypes = Object.values(AlarmConditionType);
  AlarmConditionType = AlarmConditionType;
  alarmConditionTypeTranslation = AlarmConditionTypeTranslationMap;
  readonly = this.data.readonly;
  condition = this.data.condition;
  entityId = this.data.entityId;

  conditionFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleConditionDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AlarmRuleConditionDialogComponent, AlarmCondition>,
              private fb: UntypedFormBuilder,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.conditionFormGroup = this.fb.group({
      keyFilters: [keyFiltersToKeyFilterInfos(this.condition?.condition), Validators.required],
      spec: this.fb.group({
        type: [AlarmConditionType.SIMPLE, Validators.required],
        unit: [null, Validators.required],
        predicate: [null, Validators.required]
      })
    });
    this.conditionFormGroup.patchValue({spec: this.condition?.spec});
    this.conditionFormGroup.get('spec.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type) => {
      this.updateValidators(type, true, true);
    });
    if (this.readonly) {
      this.conditionFormGroup.disable({emitEvent: false});
    } else {
      this.updateValidators(this.condition?.spec?.type);
    }
  }

  ngOnInit(): void {
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  private updateValidators(type: AlarmConditionType, resetDuration = false, emitEvent = false) {
    switch (type) {
      case AlarmConditionType.DURATION:
        this.conditionFormGroup.get('spec.unit').enable();
        this.conditionFormGroup.get('spec.predicate').enable();
        if (resetDuration) {
          this.conditionFormGroup.get('spec').patchValue({
            predicate: null
          });
        }
        break;
      case AlarmConditionType.REPEATING:
        this.conditionFormGroup.get('spec.predicate').enable();
        this.conditionFormGroup.get('spec.unit').disable();
        if (resetDuration) {
          this.conditionFormGroup.get('spec').patchValue({
            unit: null,
            predicate: null
          });
        }
        break;
      case AlarmConditionType.SIMPLE:
        this.conditionFormGroup.get('spec.unit').disable();
        this.conditionFormGroup.get('spec.predicate').disable();
        if (resetDuration) {
          this.conditionFormGroup.get('spec').patchValue({
            unit: null,
            predicate: null
          });
        }
        break;
    }
    this.conditionFormGroup.get('spec.predicate').updateValueAndValidity({emitEvent});
    this.conditionFormGroup.get('spec.unit').updateValueAndValidity({emitEvent});
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.condition = {
      condition: keyFilterInfosToKeyFilters(this.conditionFormGroup.get('keyFilters').value),
      spec: this.conditionFormGroup.get('spec').value
    };
    this.dialogRef.close(this.condition);
  }
}
