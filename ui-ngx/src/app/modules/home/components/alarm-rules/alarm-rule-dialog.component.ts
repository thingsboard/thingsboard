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

import { Component, DestroyRef, Inject, ViewChild, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { CalculatedField, CalculatedFieldArgument, CalculatedFieldType } from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { COMMA, ENTER, SEMICOLON } from "@angular/cdk/keycodes";
import { MatChipInputEvent } from "@angular/material/chips";
import {
  AlarmRule,
  AlarmRuleConditionType,
  AlarmRuleExpressionType,
  AlarmRuleTestScriptFn
} from "@shared/models/alarm-rule.models";
import { deepTrim } from "@core/utils";
import { Observable } from "rxjs";
import { switchMap } from "rxjs/operators";
import { EntityTypeSelectComponent } from "@shared/components/entity/entity-type-select.component";
import { EntityAutocompleteComponent } from "@shared/components/entity/entity-autocomplete.component";
import { EntityService } from "@core/http/entity.service";
import { RelationTypes } from "@shared/models/relation.models";
import { StringItemsOption } from "@shared/components/string-items-list.component";

export interface AlarmRuleDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  isDirty?: boolean;
  getTestScriptDialogFn: AlarmRuleTestScriptFn,
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
    entityId: this.fb.group({
      entityType: this.fb.control<EntityType | AliasEntityType | null>(EntityType.DEVICE_PROFILE, Validators.required),
      id: [null as null | string, Validators.required],
    }),
    configuration: this.fb.group({
      arguments: this.fb.control({}),
      propagate: [false],
      propagateToOwner: [false],
      propagateToTenant: [false],
      propagateRelationTypes: [null],
      createRules: [null, Validators.required],
      clearRule: [null],
    }),
  });

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  readonly EntityType = EntityType;
  readonly entityTypeTranslations = entityTypeTranslations;
  readonly alarmRuleEntityTypeList = [EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE];
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly ScriptLanguage = ScriptLanguage;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  entityName = this.data.entityName;

  disabledClearRuleButton = false;

  @ViewChild('entityTypeSelect') entityTypeSelect: EntityTypeSelectComponent;
  @ViewChild('entityAutocompleteComponent') entityAutocompleteComponent: EntityAutocompleteComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleDialogData,
              protected dialogRef: MatDialogRef<AlarmRuleDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private entityService: EntityService,
              private destroyRef: DestroyRef,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.applyDialogData();
    this.updateRulesValidators();

    this.fieldFormGroup.get('configuration.arguments').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateRulesValidators();
    });

    if (!this.entityName) {
      this.fieldFormGroup.get('entityId.id').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((entityId) => {
        if (entityId && (this.fieldFormGroup.get('entityId.entityType').value === EntityType.DEVICE_PROFILE ||
          this.fieldFormGroup.get('entityId.entityType').value === EntityType.ASSET_PROFILE)) {
          this.entityService.getEntity(this.fieldFormGroup.get('entityId.entityType').value as EntityType, entityId, {ignoreLoading: true, ignoreErrors: true}).subscribe(
            value => {
              this.entityName = value.name;
            }
          )
        }
      });
    }
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
    } else {
      this.fieldFormGroup.get('name').markAsTouched();
      this.entityTypeSelect.markAsTouched();
      this.entityAutocompleteComponent.markAsTouched();
    }
  }

  private applyDialogData(): void {
    const { configuration = {}, type = CalculatedFieldType.ALARM, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.data.entityId, ...value } = this.data.value ?? {};
    this.fieldFormGroup.patchValue({ configuration, type, debugSettings, entityId, ...value }, {emitEvent: false});
  }

  onTestScript(expression: string): Observable<string> {
    const calculatedFieldId = this.data.value?.id?.id;
    if (calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return this.data.getTestScriptDialogFn(this.fromGroupValue, expression, args, false);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
    }
    return this.data.getTestScriptDialogFn(this.fromGroupValue, expression, null, false);
  }

  private updateRulesValidators(): void {
    if (Object.keys(this.arguments ?? {}).length > 0) {
      this.fieldFormGroup.get('configuration.createRules').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.clearRule').enable({emitEvent: false});
      this.disabledClearRuleButton = true;
    } else {
      this.fieldFormGroup.get('configuration.createRules').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.clearRule').disable({emitEvent: false});
      this.disabledClearRuleButton = false;
    }
  }
  get predefinedTypeValues(): StringItemsOption[] {
    return RelationTypes.map(type => ({
      name: type,
      value: type
    }));
  }

}
