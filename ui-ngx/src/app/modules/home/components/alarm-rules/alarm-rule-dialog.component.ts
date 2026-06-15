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

import { Component, DestroyRef, Inject, ViewChild, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CalculatedFieldAlarmRule,
  CalculatedFieldArgument,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { AlarmRulesService } from '@core/http/alarm-rules.service';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { COMMA, ENTER, SEMICOLON } from "@angular/cdk/keycodes";
import {
  AlarmRule,
  AlarmRuleConditionType,
  alarmRuleEntityTypeList,
  AlarmRuleExpressionType,
  AlarmRuleTestScriptFn
} from "@shared/models/alarm-rule.models";
import { deepTrim } from "@core/utils";
import { combineLatest, forkJoin, Observable, of } from 'rxjs';
import { debounceTime, map, startWith, switchMap } from 'rxjs/operators';
import { RelationTypes } from "@shared/models/relation.models";
import { StringItemsOption } from "@shared/components/string-items-list.component";
import { BaseData } from "@shared/models/base-data";
import { CalculatedFieldFormService } from '@core/services/calculated-field-form.service';
import { EntitySelectComponent } from '@shared/components/entity/entity-select.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { AssetInfo } from '@shared/models/asset.models';
import { DeviceInfo } from '@shared/models/device.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { EntityInfoData } from '@shared/models/entity.models';

export interface AlarmRuleDialogData {
  value?: CalculatedFieldAlarmRule;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedFieldAlarmRule) => void>;
  isDirty?: boolean;
  getTestScriptDialogFn: AlarmRuleTestScriptFn,
}

@Component({
    selector: 'tb-alarm-rule-dialog',
    templateUrl: './alarm-rule-dialog.component.html',
    styleUrls: ['./alarm-rule-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class AlarmRuleDialogComponent extends DialogComponent<AlarmRuleDialogComponent, CalculatedFieldAlarmRule> {

  @ViewChild('entitySelect') entitySelect!: EntitySelectComponent;

  fieldFormGroup: FormGroup ;

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  readonly EntityType = EntityType;
  readonly entityTypeTranslations = entityTypeTranslations;
  readonly alarmRuleEntityTypeList = alarmRuleEntityTypeList;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly ScriptLanguage = ScriptLanguage;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  entityName = this.data.entityName;
  ownerId = this.data.ownerId;

  disabledClearRuleButton = false;
  disabledArguments = false;
  isLoading = false;
  createNew = false;

  entityTypeControl = new FormControl<EntityType>(EntityType.DEVICE_PROFILE, { nonNullable: true, validators: Validators.required });

  argsEntityId: EntityId | EntityId[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleDialogData,
              protected dialogRef: MatDialogRef<AlarmRuleDialogComponent, CalculatedFieldAlarmRule>,
              private alarmRulesService: AlarmRulesService,
              private deviceProfileService: DeviceProfileService,
              private assetProfileService: AssetProfileService,
              private destroyRef: DestroyRef,
              private cfFormService: CalculatedFieldFormService) {
    super(store, router, dialogRef);
    this.fieldFormGroup = this.cfFormService.buildAlarmRuleForm();
    this.applyDialogData();
    this.updateRulesValidators();

    if (this.data.isDirty) {
      this.fieldFormGroup.markAsDirty();
    }

    this.fieldFormGroup.get('configuration.arguments').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateRulesValidators();
    });

    if (!this.data.entityId) {
      this.createNew = true;
      this.entityTypeControl.valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(() => this.fieldFormGroup.get('entityId')!.reset(null));

      combineLatest([
        this.fieldFormGroup.get('entityId')!.valueChanges.pipe(startWith(this.fieldFormGroup.get('entityId')!.value)),
        this.fieldFormGroup.get('name')!.valueChanges.pipe(startWith(this.fieldFormGroup.get('name')!.value))
      ]).pipe(
        debounceTime(50),
        takeUntilDestroyed()
      ).subscribe(([entityId, name]) => {
        this.disabledArguments = Array.isArray(entityId) ? !entityId.length : !entityId || !name?.length;
        this.argsEntityId = Array.isArray(entityId) ? entityId.map(id => ({ entityType: this.entityTypeControl.value, id })) : entityId;
        const argsControl = this.fieldFormGroup.get('configuration.arguments')!;
        if (this.disabledArguments) {
          argsControl.disable({ emitEvent: false });
        } else {
          argsControl.enable({ emitEvent: false });
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

  get fromGroupValue(): CalculatedFieldAlarmRule {
    return deepTrim(this.fieldFormGroup.value as CalculatedFieldAlarmRule);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid && Object.keys(this.arguments ?? {}).length > 0) {
      this.isLoading = true;
      this.resolveEntityIds().pipe(
        switchMap(entityIds =>
          entityIds.length
            ? forkJoin(entityIds.map((entityId: EntityId) => {
                const alarmRule = { ...(this.data.value ?? {}), ...this.fromGroupValue, entityId };
                alarmRule.configuration.type = CalculatedFieldType.ALARM;
                return this.alarmRulesService.saveAlarmRule(alarmRule);
              }))
            : of([] as CalculatedFieldAlarmRule[])),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (calculatedFields: CalculatedFieldAlarmRule[]) =>
          calculatedFields.length ? this.dialogRef.close(calculatedFields[0]) : this.isLoading = false,
        error: () => this.isLoading = false
      });
    } else {
      this.fieldFormGroup.get('name').markAsTouched();
      this.fieldFormGroup.get('entityId')?.markAsTouched();
      this.entitySelect?.entityAutocompleteMarkAsTouched();
    }
  }

  private resolveEntityIds(): Observable<EntityId[]> {
    const value = this.fieldFormGroup.get('entityId').value;
    if (!Array.isArray(value)) {
      return of([value ?? this.data.entityId]);
    }
    switch (this.entityTypeControl.value) {
      case EntityType.DEVICE_PROFILE:
        return this.deviceProfileService.getDeviceProfileNames(false).pipe(
          map(profiles => this.toProfileEntityIds(profiles, value))
        );
      case EntityType.ASSET_PROFILE:
        return this.assetProfileService.getAssetProfileNames(false).pipe(
          map(profiles => this.toProfileEntityIds(profiles, value))
        );
      default:
        return of(value.map(id => ({ entityType: this.entityTypeControl.value, id })));
    }
  }

  private toProfileEntityIds(profiles: EntityInfoData[], names: string[]): EntityId[] {
    const idByName = new Map(profiles.map(profile => [profile.name, profile.id]));
    return names.map(name => idByName.get(name)).filter((id): id is EntityId => !!id);
  }

  private applyDialogData(): void {
    const { configuration = {}, type = CalculatedFieldType.ALARM, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.data.entityId, ...value } = this.data.value ?? {};
    this.fieldFormGroup.patchValue({ configuration, type, debugSettings, entityId, ...value }, {emitEvent: false});
  }

  onTestScript(expression: string): Observable<string> {
    return this.cfFormService.testScript(
      this.data.value?.id?.id,
      this.fromGroupValue,
      this.data.getTestScriptDialogFn,
      this.destroyRef,
      expression
    );
  }

  private updateRulesValidators(): void {
    if (Object.keys(this.arguments ?? {}).length > 0) {
      this.fieldFormGroup.get('configuration.createRules').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.clearRule').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagate').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToOwner').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToTenant').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateRelationTypes').enable({emitEvent: false});
      this.disabledClearRuleButton = false;
    } else {
      this.fieldFormGroup.get('configuration.createRules').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.clearRule').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagate').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToOwner').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToTenant').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateRelationTypes').disable({emitEvent: false});
      this.disabledClearRuleButton = true;
    }
  }
  get predefinedTypeValues(): StringItemsOption[] {
    return RelationTypes.map(type => ({
      name: type,
      value: type
    }));
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity.name;
    if (this.isAssignedToCustomer(entity as AssetInfo | DeviceInfo)) {
      this.ownerId = (entity as AssetInfo | DeviceInfo).customerId;
    }
  }

  private isAssignedToCustomer(entity: AssetInfo | DeviceInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }
}
