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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  calculatedFieldsEntityTypeList,
  CalculatedFieldTestScriptFn,
  CalculatedFieldType,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations
} from '@shared/models/calculated-field.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { Observable } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { deepTrim } from '@core/utils';
import { BaseData } from '@shared/models/base-data';
import { CalculatedFieldFormService } from '@core/services/calculated-field-form.service';
import { FormGroup } from '@angular/forms';
import { AssetInfo } from '@shared/models/asset.models';
import { DeviceInfo } from '@shared/models/device.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';

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
  disabledSelectType?: boolean;
}

@Component({
  selector: 'tb-calculated-field-dialog',
  templateUrl: './calculated-field-dialog.component.html',
  styleUrls: ['./calculated-field-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CalculatedFieldDialogComponent extends DialogComponent<CalculatedFieldDialogComponent, CalculatedField> {

  fieldFormGroup: FormGroup;

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  entityName = this.data.entityName;
  ownerId = this.data.ownerId;

  disabledConfiguration = false;
  isLoading = false;

  readonly EntityType = EntityType;
  readonly calculatedFieldsEntityTypeList = calculatedFieldsEntityTypeList;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly fieldTypes = calculatedFieldTypes;
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private destroyRef: DestroyRef,
              private cfFormService: CalculatedFieldFormService) {
    super(store, router, dialogRef);
    this.fieldFormGroup = this.cfFormService.buildForm();
    this.cfFormService.setupTypeChange(this.fieldFormGroup, this.destroyRef);
    this.applyDialogData();

    if (this.data.isDirty) {
      this.fieldFormGroup.markAsDirty();
    }

    if (!this.data.entityId) {
      this.fieldFormGroup.get('entityId').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((entityId) => {
        this.disabledConfiguration = !entityId;
        if (this.disabledConfiguration) {
          this.fieldFormGroup.get('configuration').disable({emitEvent: false});
        } else {
          this.fieldFormGroup.get('configuration').enable({emitEvent: false});
          this.fieldFormGroup.get('configuration').updateValueAndValidity({emitEvent: false});
        }
      });
    }

    if (this.data.disabledSelectType) {
      this.fieldFormGroup.get('type').disable({emitEvent: false});
    }
  }

  get fromGroupValue(): CalculatedField {
    return deepTrim(this.fieldFormGroup.getRawValue() as CalculatedField);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      this.isLoading = true;
      this.calculatedFieldsService.saveCalculatedField({ entityId: this.data.entityId, ...(this.data.value ?? {}),  ...this.fromGroupValue})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: calculatedField => this.dialogRef.close(calculatedField),
          error: () => this.isLoading = false
        });
    } else {
      this.fieldFormGroup.get('name').markAsTouched();
    }
  }

  onTestScript(expression?: string): Observable<string> {
    return this.cfFormService.testScript(
      this.data.value?.id?.id,
      this.fromGroupValue,
      this.data.getTestScriptDialogFn,
      this.destroyRef,
      expression
    );
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity.name;
    if (this.isAssignedToCustomer(entity as AssetInfo | DeviceInfo)) {
      this.ownerId = (entity as AssetInfo | DeviceInfo).customerId;
    }
  }

  get entityId(): EntityId {
    return this.data.entityId || this.fieldFormGroup.get('entityId').value;
  }

  private applyDialogData(): void {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.SIMPLE, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.data.entityId, ...value } = this.data.value ?? {};
    const preparedConfig = this.cfFormService.prepareConfig(configuration);
    this.fieldFormGroup.patchValue({ configuration: preparedConfig, type, debugSettings, entityId, ...value }, {emitEvent: false});
    setTimeout(() => this.fieldFormGroup.get('type').updateValueAndValidity({onlySelf: true}));
    if (!this.data.entityId) {
      this.fieldFormGroup.get('configuration').disable({emitEvent: false});
      this.disabledConfiguration = true;
    }
  }

  private isAssignedToCustomer(entity: AssetInfo | DeviceInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }
}
