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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { CalculatedField, CalculatedFieldArgument, CalculatedFieldType } from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { COMMA, ENTER, SEMICOLON } from "@angular/cdk/keycodes";
import { MatChipInputEvent } from "@angular/material/chips";
import { AlarmRule, AlarmRuleConditionType, AlarmRuleExpressionType } from "@shared/models/alarm-rule.models";
import { deepTrim } from "@core/utils";

export interface AlarmRuleDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  isDirty?: boolean;
}

@Component({
  selector: 'tb-alarm-rule-dialog',
  templateUrl: './alarm-rule-dialog.component.html',
  styleUrls: ['./alarm-rule-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AlarmRuleDialogComponent extends DialogComponent<AlarmRuleDialogComponent, CalculatedField> {

  fieldFormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    type: [CalculatedFieldType.ALARM],
    debugSettings: [],
    configuration: this.fb.group({
      arguments: this.fb.control({}),
      propagate: [false],
      propagateToOwner: [false],
      propagateToTenant: [false],
      propagateRelationTypes: [null],
      createRules: [null],
      clearRule: [null],
    }),
  });

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  readonly EntityType = EntityType;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly ScriptLanguage = ScriptLanguage;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleDialogData,
              protected dialogRef: MatDialogRef<AlarmRuleDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private destroyRef: DestroyRef,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.observeIsLoading();
    this.applyDialogData();
  }

  get configFormGroup(): FormGroup {
    return this.fieldFormGroup.get('configuration') as FormGroup;
  }

  get arguments(): Record<string, CalculatedFieldArgument> {
    return this.fieldFormGroup.get('configuration.arguments').value;
  }

  public removeClearAlarmRule() {
    this.configFormGroup.patchValue({clearRule: null});
    this.fieldFormGroup.markAsDirty();
  }

  public addClearAlarmRule() {
    const clearAlarmRule: AlarmRule = {
      condition: {
        type: AlarmRuleConditionType.SIMPLE,
        expression: {
          type: AlarmRuleExpressionType.SIMPLE
        }
      }
    };
    this.configFormGroup.patchValue({clearRule: clearAlarmRule});
  }

  removeRelationType(key: string): void {
    const keys: string[] = this.configFormGroup.get('propagateRelationTypes').value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.configFormGroup.get('propagateRelationTypes').setValue(keys);
    }
  }

  addRelationType(event: MatChipInputEvent): void {
    const input = event.chipInput.inputElement;
    let value = (event.value ?? '').trim();
    if (value) {
      let keys: string[] = this.configFormGroup.get('propagateRelationTypes').value ?? [];
      if (keys.indexOf(value) === -1) {
        keys.push(value);
        this.configFormGroup.get('propagateRelationTypes').setValue(keys);
      }
    }
    if (input) {
      input.value = '';
    }
  }

  get fromGroupValue(): CalculatedField {
    return deepTrim(this.fieldFormGroup.value as CalculatedField);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      const alarmRule = { entityId: this.data.entityId, ...(this.data.value ?? {}),  ...this.fromGroupValue};
      alarmRule.configuration.type = CalculatedFieldType.ALARM;

      this.calculatedFieldsService.saveCalculatedField(alarmRule)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(calculatedField => this.dialogRef.close(calculatedField));
    }
  }

  private applyDialogData(): void {
    const { configuration = {}, type = CalculatedFieldType.ALARM, debugSettings = { failuresEnabled: true, allEnabled: true }, ...value } = this.data.value ?? {};
    this.fieldFormGroup.patchValue({ configuration, type, debugSettings, ...value }, {emitEvent: false});
  }

  private observeIsLoading(): void {
    this.isLoading$.pipe(takeUntilDestroyed()).subscribe(loading => {
      if (loading) {
        this.fieldFormGroup.disable({emitEvent: false});
      } else {
        this.fieldFormGroup.enable({emitEvent: false});
        if (this.data.isDirty) {
          this.fieldFormGroup.markAsDirty();
        }
      }
    });
  }
}
