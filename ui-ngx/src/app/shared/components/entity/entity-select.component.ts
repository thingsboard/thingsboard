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

import { AfterViewInit, Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntityId } from '@shared/models/id/entity-id';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { NULL_UUID } from '@shared/models/id/has-uuid';

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
export class EntitySelectComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entitySelectFormGroup: FormGroup;

  modelValue: EntityId = {entityType: null, id: null};

  @Input()
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  useAliasEntityTypes: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  displayEntityTypeSelect: boolean;

  AliasEntityType = AliasEntityType;

  private readonly defaultEntityType: EntityType | AliasEntityType = null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: FormBuilder) {

    const entityTypes = this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes,
                                                                         this.useAliasEntityTypes);
    if (entityTypes.length === 1) {
      this.displayEntityTypeSelect = false;
      this.defaultEntityType = entityTypes[0];
    } else {
      this.displayEntityTypeSelect = true;
    }

    this.entitySelectFormGroup = this.fb.group({
      entityType: [this.defaultEntityType],
      entityId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.entitySelectFormGroup.get('entityType').valueChanges.subscribe(
      (value) => {
        this.updateView(value, this.modelValue.id);
      }
    );
    this.entitySelectFormGroup.get('entityId').valueChanges.subscribe(
      (value) => {
        const id = value ? (typeof value === 'string' ? value : value.id) : null;
        this.updateView(this.modelValue.entityType, id);
      }
    );
  }

  ngAfterViewInit(): void {
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
      this.modelValue = value;
      this.entitySelectFormGroup.get('entityType').patchValue(value.entityType, {emitEvent: true});
      this.entitySelectFormGroup.get('entityId').patchValue(value, {emitEvent: true});
    } else {
      this.modelValue = {
        entityType: this.defaultEntityType,
        id: null
      };
      this.entitySelectFormGroup.get('entityType').patchValue(this.defaultEntityType, {emitEvent: true});
      this.entitySelectFormGroup.get('entityId').patchValue(null, {emitEvent: true});
    }
  }

  updateView(entityType: EntityType | AliasEntityType | null, entityId: string | null) {
    if (this.modelValue.entityType !== entityType || this.modelValue.id !== entityId) {
      this.modelValue = {
        entityType,
        id: this.modelValue.entityType !== entityType ? null : entityId
      };

      if (this.modelValue.entityType === AliasEntityType.CURRENT_TENANT
        || this.modelValue.entityType === AliasEntityType.CURRENT_USER
        || this.modelValue.entityType === AliasEntityType.CURRENT_USER_OWNER) {
        this.modelValue.id = NULL_UUID;
      }

      if (this.modelValue.entityType && this.modelValue.id) {
        this.propagateChange(this.modelValue);
      } else {
        this.propagateChange(null);
      }
    }
  }
}
