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
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  calculatedFieldsEntityTypeList,
  CalculatedFieldTestScriptFn,
  CalculatedFieldType,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations,
  OutputStrategyType
} from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { pairwise, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { Observable } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { deepTrim, isDefined } from '@core/utils';
import { EntityTypeSelectComponent } from '@shared/components/entity/entity-type-select.component';
import { EntityAutocompleteComponent } from '@shared/components/entity/entity-autocomplete.component';
import { EntityService } from '@core/http/entity.service';

export interface CalculatedFieldDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  getTestScriptDialogFn: CalculatedFieldTestScriptFn;
  isDirty?: boolean;
}

@Component({
  selector: 'tb-calculated-field-dialog',
  templateUrl: './calculated-field-dialog.component.html',
  styleUrls: ['./calculated-field-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CalculatedFieldDialogComponent extends DialogComponent<CalculatedFieldDialogComponent, CalculatedField> {

  fieldFormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    entityId: this.fb.group({
      entityType: this.fb.control<EntityType | AliasEntityType | null>(EntityType.DEVICE_PROFILE, Validators.required),
      id: [null as null | string, Validators.required],
    }),
    type: [CalculatedFieldType.SIMPLE],
    debugSettings: [],
    configuration: this.fb.control<CalculatedFieldConfiguration>({} as CalculatedFieldConfiguration),
  });

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  entityName = this.data.entityName;

  readonly EntityType = EntityType;
  readonly calculatedFieldsEntityTypeList = calculatedFieldsEntityTypeList;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly fieldTypes = calculatedFieldTypes;
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  @ViewChild('entityTypeSelect') entityTypeSelect: EntityTypeSelectComponent;
  @ViewChild('entityAutocompleteComponent') entityAutocompleteComponent: EntityAutocompleteComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private entityService: EntityService,
              private destroyRef: DestroyRef,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.observeIsLoading();
    this.observeType();
    this.applyDialogData();

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

  get fromGroupValue(): CalculatedField {
    return deepTrim(this.fieldFormGroup.value as CalculatedField);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      this.calculatedFieldsService.saveCalculatedField({ entityId: this.data.entityId, ...(this.data.value ?? {}),  ...this.fromGroupValue})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(calculatedField => this.dialogRef.close(calculatedField));
    } else {
      this.fieldFormGroup.get('name').markAsTouched();
      this.entityTypeSelect?.markAsTouched();
      this.entityAutocompleteComponent?.markAsTouched();
    }
  }

  onTestScript(expression?: string): Observable<string> {
    const calculatedFieldId = this.data.value?.id?.id;
    if (calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return this.data.getTestScriptDialogFn(this.fromGroupValue, args, false, expression);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
    }
    return this.data.getTestScriptDialogFn(this.fromGroupValue, null, false, expression);
  }

  get entityId(): EntityId {
    return this.data.entityId || this.fieldFormGroup.get('entityId').value;
  }

  private applyDialogData(): void {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.SIMPLE, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.data.entityId, ...value } = this.data.value ?? {};
    if (configuration.type !== CalculatedFieldType.ALARM) {
      if (isDefined(configuration?.output) && !configuration?.output?.strategy) {
          configuration.output.strategy = {type: OutputStrategyType.RULE_CHAIN};
      }
    }
    this.fieldFormGroup.patchValue({ configuration, type, debugSettings, entityId, ...value }, {emitEvent: false});
    setTimeout(() => this.fieldFormGroup.get('type').updateValueAndValidity({onlySelf: true}));
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

  private observeType(): void {
    this.fieldFormGroup.get('type').valueChanges.pipe(
      pairwise(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(([prevType, nextType]) => {
      if (![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(prevType) ||
          ![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(nextType)) {
        this.fieldFormGroup.get('configuration').setValue(({} as CalculatedFieldConfiguration), {emitEvent: false});
      }
    });
  }
}
