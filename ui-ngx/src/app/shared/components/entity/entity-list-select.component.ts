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

interface EntityListSelectModel {
  entityType: EntityType | AliasEntityType;
  ids: Array<string>;
}

@Component({
  selector: 'tb-entity-list-select',
  templateUrl: './entity-list-select.component.html',
  styleUrls: ['./entity-list-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityListSelectComponent),
    multi: true
  }]
})

export class EntityListSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entityListSelectFormGroup: FormGroup;

  modelValue: EntityListSelectModel = {entityType: null, ids: []};

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

    this.entityListSelectFormGroup = this.fb.group({
      entityType: [this.defaultEntityType],
      entityIds: [[]]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.entityListSelectFormGroup.get('entityType').valueChanges.subscribe(
      (value) => {
        this.updateView(value, this.modelValue.ids);
      }
    );
    this.entityListSelectFormGroup.get('entityIds').valueChanges.subscribe(
      (values) => {
        this.updateView(this.modelValue.entityType, values);
      }
    );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.entityListSelectFormGroup.disable();
    } else {
      this.entityListSelectFormGroup.enable();
    }
  }

  writeValue(value: Array<EntityId> | null): void {
    if (value != null && value.length > 0) {
      const id = value[0];
      this.modelValue = {
        entityType: id.entityType,
        ids: value.map(val => val.id)
      };
    } else {
      this.modelValue = {
        entityType: this.defaultEntityType,
        ids: []
      };
    }
    this.entityListSelectFormGroup.get('entityType').patchValue(this.modelValue.entityType, {emitEvent: true});
    this.entityListSelectFormGroup.get('entityIds').patchValue([...this.modelValue.ids], {emitEvent: true});
  }

  updateView(entityType: EntityType | AliasEntityType | null, entityIds: Array<string> | null) {
    if (this.modelValue.entityType !== entityType ||
      !this.compareIds(this.modelValue.ids, entityIds)) {
      this.modelValue = {
        entityType,
        ids: this.modelValue.entityType !== entityType || !entityIds ? [] : [...entityIds]
      };
      this.propagateChange(this.toEntityIds(this.modelValue));
    }
  }

  compareIds(ids1: Array<string> | null, ids2: Array<string> | null): boolean {
    if (ids1 !== null && ids2 !== null) {
      return JSON.stringify(ids1) === JSON.stringify(ids2);
    } else {
      return ids1 === ids2;
    }
  }

  toEntityIds(modelValue: EntityListSelectModel): Array<EntityId> {
    if (modelValue !== null && modelValue.entityType && modelValue.ids && modelValue.ids.length > 0) {
      const entityType = modelValue.entityType;
      return modelValue.ids.map(id => ({entityType, id}));
    } else {
      return null;
    }
  }

}
