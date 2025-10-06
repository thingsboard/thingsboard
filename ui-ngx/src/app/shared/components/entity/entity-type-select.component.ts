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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@app/shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldAppearance } from '@angular/material/form-field';

@Component({
  selector: 'tb-entity-type-select',
  templateUrl: './entity-type-select.component.html',
  styleUrls: ['./entity-type-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityTypeSelectComponent),
    multi: true
  }]
})
export class EntityTypeSelectComponent implements ControlValueAccessor, OnInit, OnChanges {

  entityTypeFormGroup: UntypedFormGroup;

  modelValue: EntityType | AliasEntityType | null;

  @Input()
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  useAliasEntityTypes: boolean;

  @Input()
  filterAllowedEntityTypes = true;

  @Input()
  @coerceBoolean()
  showLabel: boolean;

  @Input()
  label = this.translate.instant('entity.type');

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  additionEntityTypes: {[key in string]: string} = {};

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  entityTypes: Array<EntityType | AliasEntityType | string>;

  private propagateChange = (_v: any) => { };

  constructor(private entityService: EntityService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    this.entityTypeFormGroup = this.fb.group({
      entityType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.entityTypes = this.filterAllowedEntityTypes
      ? this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes, this.useAliasEntityTypes)
      : this.allowedEntityTypes;
    const additionEntityTypes = Object.keys(this.additionEntityTypes);
    if (additionEntityTypes.length > 0) {
      this.entityTypes.push(...additionEntityTypes);
    }
    this.entityTypeFormGroup.get('entityType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (value) => {
        let modelValue: EntityType | AliasEntityType;
        if (!value || value === '') {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
      }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'allowedEntityTypes') {
          this.entityTypes = this.filterAllowedEntityTypes ?
            this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes, this.useAliasEntityTypes) : this.allowedEntityTypes;
          const additionEntityTypes = Object.keys(this.additionEntityTypes);
          if (additionEntityTypes.length > 0) {
            this.entityTypes.push(...additionEntityTypes);
          }
          const currentEntityType: EntityType | AliasEntityType = this.entityTypeFormGroup.get('entityType').value;
          if (currentEntityType && !this.entityTypes.includes(currentEntityType)) {
            this.entityTypeFormGroup.get('entityType').patchValue(null, {emitEvent: true});
          }
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.entityTypeFormGroup.disable();
    } else {
      this.entityTypeFormGroup.enable();
    }
  }

  writeValue(value: EntityType | AliasEntityType | null): void {
    if (value != null) {
      this.modelValue = value;
      this.entityTypeFormGroup.get('entityType').patchValue(value, {emitEvent: true});
    } else {
      this.modelValue = null;
      this.entityTypeFormGroup.get('entityType').patchValue(null, {emitEvent: true});
    }
  }

  updateView(value: EntityType | AliasEntityType | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityTypeFn(entityType?: EntityType | AliasEntityType | null): string | undefined {
    if (this.additionEntityTypes[entityType]) {
      return this.additionEntityTypes[entityType];
    } else if (entityType) {
      return this.translate.instant(entityTypeTranslations.get(entityType as EntityType).type);
    } else {
      return '';
    }
  }
}
