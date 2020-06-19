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
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@app/shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

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
export class EntityTypeSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entityTypeFormGroup: FormGroup;

  modelValue: EntityType | AliasEntityType | null;

  @Input()
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  useAliasEntityTypes: boolean;

  private showLabelValue: boolean;
  get showLabel(): boolean {
    return this.showLabelValue;
  }
  @Input()
  set showLabel(value: boolean) {
    this.showLabelValue = coerceBooleanProperty(value);
  }

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

  entityTypes: Array<EntityType | AliasEntityType>;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: FormBuilder) {
    this.entityTypeFormGroup = this.fb.group({
      entityType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.entityTypes = this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes, this.useAliasEntityTypes);
    this.entityTypeFormGroup.get('entityType').valueChanges.subscribe(
      (value) => {
        let modelValue;
        if (!value || value === '') {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
      }
    );
  }

  ngAfterViewInit(): void {
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
    if (entityType) {
      return this.translate.instant(entityTypeTranslations.get(entityType as EntityType).type);
    } else {
      return '';
    }
  }
}
