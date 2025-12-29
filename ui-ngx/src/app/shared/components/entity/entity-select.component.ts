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

import { Component, DestroyRef, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntityId } from '@shared/models/id/entity-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { BaseData } from '@shared/models/base-data';

@Component({
  selector: 'tb-entity-select',
  templateUrl: './entity-select.component.html',
  styleUrls: ['./entity-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntitySelectComponent),
    multi: true
  }]
})
export class EntitySelectComponent implements ControlValueAccessor, OnInit {

  entitySelectFormGroup: UntypedFormGroup;

  modelValue: EntityId = {entityType: null, id: null};

  @Input()
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  useAliasEntityTypes: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  additionEntityTypes: {[entityType in string]: string} = {};

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  useEntityDisplayName = false;

  @Input()
  filterAllowedEntityTypes: boolean;

  @Input()
  defaultEntityType: AliasEntityType | EntityType;

  @Input()
  entityTypeLabel: string;

  @Output()
  entityChanged = new EventEmitter<BaseData<EntityId>>();

  displayEntityTypeSelect: boolean;

  AliasEntityType = AliasEntityType;

  entityTypeNullUUID: Set<AliasEntityType | EntityType | string> = new Set([
    AliasEntityType.CURRENT_TENANT, AliasEntityType.CURRENT_USER, AliasEntityType.CURRENT_USER_OWNER
  ]);

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {

    const entityTypes = this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes,
                                                                         this.useAliasEntityTypes);

    let defaultEntityType: EntityType | AliasEntityType = null

    if (entityTypes.length === 1) {
      this.displayEntityTypeSelect = false;
      defaultEntityType = entityTypes[0];
    } else {
      this.displayEntityTypeSelect = true;
    }

    this.entitySelectFormGroup = this.fb.group({
      entityType: [defaultEntityType],
      entityId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.entitySelectFormGroup.get('entityType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (value) => {
        this.updateView(value, this.modelValue.id);
      }
    );
    this.entitySelectFormGroup.get('entityId').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (value) => {
        const id = value ? (typeof value === 'string' ? value : value.id) : null;
        this.updateView(this.modelValue.entityType, id);
      }
    );
    const additionNullUIIDEntityTypes = Object.keys(this.additionEntityTypes) as string[];
    if (additionNullUIIDEntityTypes.length > 0) {
      additionNullUIIDEntityTypes.forEach((entityType) => this.entityTypeNullUUID.add(entityType));
    }

    if (this.filterAllowedEntityTypes === false) {
      if (this.allowedEntityTypes?.length === 1) {
        this.displayEntityTypeSelect = false;
        this.entitySelectFormGroup.get('entityType').setValue(this.allowedEntityTypes[0])
      } else {
        this.displayEntityTypeSelect = true;
      }
    }

    if (this.defaultEntityType) {
      this.entitySelectFormGroup.get('entityType').setValue(this.defaultEntityType);
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.entitySelectFormGroup.disable();
    } else {
      this.entitySelectFormGroup.enable();
    }
  }

  writeValue(value: EntityId | null): void {
    if (value != null) {
      this.modelValue = {
        entityType: value.entityType,
        id: value.id !== NULL_UUID ? value.id : null
      };
    } else {
      this.modelValue = {
        entityType: this.defaultEntityType,
        id: null
      };
    }
    this.entitySelectFormGroup.get('entityType').patchValue(this.modelValue.entityType, {emitEvent: false});
    this.entitySelectFormGroup.get('entityId').patchValue(this.modelValue, {emitEvent: false});
  }

  updateView(entityType: EntityType | AliasEntityType | null, entityId: string | null) {
    if (this.modelValue.entityType !== entityType || this.modelValue.id !== entityId) {
      this.modelValue = {
        entityType,
        id: this.modelValue.entityType !== entityType ? null : entityId
      };

      if (this.entityTypeNullUUID.has(this.modelValue.entityType)) {
        this.modelValue.id = NULL_UUID;
      } else if (this.modelValue.entityType === AliasEntityType.CURRENT_CUSTOMER && !this.modelValue.id) {
        this.modelValue.id = NULL_UUID;
      }

      if (this.modelValue.entityType && this.modelValue.id) {
        this.propagateChange(this.modelValue);
      } else {
        this.propagateChange(null);
      }
    }
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityChanged.emit(entity);
  }
}
