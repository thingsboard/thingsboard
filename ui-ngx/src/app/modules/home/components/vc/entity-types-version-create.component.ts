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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import {
  entityTypesWithoutRelatedData,
  EntityTypeVersionCreateConfig,
  exportableEntityTypes,
  SyncStrategy,
  syncStrategyTranslationMap,
  typesWithCalculatedFields
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-entity-types-version-create',
    templateUrl: './entity-types-version-create.component.html',
    styleUrls: ['./entity-types-version.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityTypesVersionCreateComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => EntityTypesVersionCreateComponent),
            multi: true
        }
    ],
    standalone: false
})
export class EntityTypesVersionCreateComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: {[entityType: string]: EntityTypeVersionCreateConfig};

  private propagateChange = null;

  public entityTypesVersionCreateFormGroup: UntypedFormGroup;

  syncStrategies = Object.values(SyncStrategy);

  syncStrategyTranslations = syncStrategyTranslationMap;

  entityTypes = EntityType;
  entityTypesWithoutRelatedData = entityTypesWithoutRelatedData;

  loading = true;

  readonly typesWithCalculatedFields = typesWithCalculatedFields;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.entityTypesVersionCreateFormGroup = this.fb.group({
      entityTypes: this.fb.array([], [])
    });
    this.entityTypesVersionCreateFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityTypesVersionCreateFormGroup.disable({emitEvent: false});
    } else {
      this.entityTypesVersionCreateFormGroup.enable({emitEvent: false});
      (this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray).controls.forEach(
        control => this.updateEntityTypeValidators(control)
      );
    }
  }

  writeValue(value: {[entityType: string]: EntityTypeVersionCreateConfig} | undefined): void {
    this.modelValue = value;
    this.entityTypesVersionCreateFormGroup.setControl('entityTypes',
      this.prepareEntityTypesFormArray(value), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    return this.entityTypesVersionCreateFormGroup.valid && this.entityTypesFormGroupArray().length ? null : {
      entityTypes: {
        valid: false,
      },
    };
  }

  private prepareEntityTypesFormArray(entityTypes: {[entityType: string]: EntityTypeVersionCreateConfig} | undefined): UntypedFormArray {
    const entityTypesControls: Array<AbstractControl> = [];
    if (entityTypes) {
      for (const entityType of Object.keys(entityTypes)) {
        const config = entityTypes[entityType];
        entityTypesControls.push(this.createEntityTypeControl(entityType as EntityType, config));
      }
    }
    return this.fb.array(entityTypesControls);
  }

  private createEntityTypeControl(entityType: EntityType, config: EntityTypeVersionCreateConfig): AbstractControl {
    const entityTypeControl = this.fb.group(
      {
        entityType: [entityType, [Validators.required]],
        config: this.fb.group({
          syncStrategy: [config.syncStrategy === null ? 'default' : config.syncStrategy, []],
          saveRelations: [config.saveRelations, []],
          saveAttributes: [config.saveAttributes, []],
          saveCredentials: [config.saveCredentials, []],
          saveCalculatedFields: [config.saveCalculatedFields, []],
          allEntities: [config.allEntities, []],
          entityIds: [config.entityIds, [Validators.required]]
        })
      }
    );
    this.updateEntityTypeValidators(entityTypeControl);
    entityTypeControl.get('config.allEntities').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateEntityTypeValidators(entityTypeControl);
    });
    return entityTypeControl;
  }

  private updateEntityTypeValidators(entityTypeControl: AbstractControl): void {
    const allEntities: boolean = entityTypeControl.get('config').get('allEntities').value;
    if (allEntities) {
      entityTypeControl.get('config').get('entityIds').disable({emitEvent: false});
    } else {
      entityTypeControl.get('config').get('entityIds').enable({emitEvent: false});
    }
    entityTypeControl.get('config').get('entityIds').updateValueAndValidity({emitEvent: false});
  }

  entityTypesFormGroupArray(): UntypedFormGroup[] {
    return (this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray).controls as UntypedFormGroup[];
  }

  entityTypesFormGroupExpanded(entityTypeControl: AbstractControl): boolean {
    return !!(entityTypeControl as any).expanded;
  }

  public removeEntityType(index: number) {
    (this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray).removeAt(index);
  }

  public addEnabled(): boolean {
    const entityTypesArray = this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray;
    return entityTypesArray.length < exportableEntityTypes.length;
  }

  public addEntityType() {
    const entityTypesArray = this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray;
    const config: EntityTypeVersionCreateConfig = {
      syncStrategy: null,
      saveAttributes: true,
      saveRelations: true,
      saveCredentials: true,
      saveCalculatedFields: true,
      allEntities: true,
      entityIds: []
    };
    const allowed = this.allowedEntityTypes();
    let entityType: EntityType = null;
    if (allowed.length) {
      entityType = allowed[0];
    }
    const entityTypeControl = this.createEntityTypeControl(entityType, config);
    (entityTypeControl as any).expanded = true;
    entityTypesArray.push(entityTypeControl);
    this.entityTypesVersionCreateFormGroup.updateValueAndValidity();
  }

  public removeAll() {
    const entityTypesArray = this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray;
    entityTypesArray.clear();
    this.entityTypesVersionCreateFormGroup.updateValueAndValidity();
  }

  entityTypeText(entityTypeControl: AbstractControl): string {
    const entityType: EntityType = entityTypeControl.get('entityType').value;
    const config: EntityTypeVersionCreateConfig = entityTypeControl.get('config').value;
    let count = config?.entityIds?.length;
    if (!isDefinedAndNotNull(count)) {
      count = 0;
    }
    if (entityType) {
      return this.translate.instant((config?.allEntities ? entityTypeTranslations.get(entityType).typePlural
        : entityTypeTranslations.get(entityType).list), { count });
    } else {
      return 'Undefined';
    }
  }

  allowedEntityTypes(entityTypeControl?: AbstractControl): Array<EntityType> {
    let res = [...exportableEntityTypes];
    const currentEntityType: EntityType = entityTypeControl?.get('entityType')?.value;
    const value: [{entityType: string; config: EntityTypeVersionCreateConfig}] =
      this.entityTypesVersionCreateFormGroup.get('entityTypes').value || [];
    const usedEntityTypes = value.map(val => val.entityType).filter(val => val);
    res = res.filter(entityType => !usedEntityTypes.includes(entityType) || entityType === currentEntityType);
    return res;
  }

  private updateModel() {
    const value: [{entityType: string; config: EntityTypeVersionCreateConfig}] =
      this.entityTypesVersionCreateFormGroup.get('entityTypes').value || [];
    let modelValue: {[entityType: string]: EntityTypeVersionCreateConfig} = null;
    if (value && value.length) {
      modelValue = {};
      value.forEach((val) => {
        modelValue[val.entityType] = val.config;
        if ((modelValue[val.entityType].syncStrategy as any) === 'default') {
          modelValue[val.entityType].syncStrategy = null;
        }
      });
    }
    this.modelValue = modelValue;
    this.propagateChange(this.modelValue);
  }
}
