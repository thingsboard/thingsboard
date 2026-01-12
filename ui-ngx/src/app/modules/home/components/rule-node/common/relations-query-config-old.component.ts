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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { EntitySearchDirection, entitySearchDirectionTranslations, PageComponent } from '@shared/public-api';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { RelationsQuery } from '../rule-node-config.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-relations-query-config-old',
  templateUrl: './relations-query-config-old.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RelationsQueryConfigOldComponent),
      multi: true
    }
  ]
})
export class RelationsQueryConfigOldComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  directionTypes = Object.keys(EntitySearchDirection);
  directionTypeTranslations = entitySearchDirectionTranslations;

  relationsQueryFormGroup: FormGroup;

  private propagateChange = null;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.relationsQueryFormGroup = this.fb.group({
      fetchLastLevelOnly: [false, []],
      direction: [null, [Validators.required]],
      maxLevel: [null, []],
      filters: [null]
    });
    this.relationsQueryFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((query: RelationsQuery) => {
      if (this.relationsQueryFormGroup.valid) {
        this.propagateChange(query);
      } else {
        this.propagateChange(null);
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.relationsQueryFormGroup.disable({emitEvent: false});
    } else {
      this.relationsQueryFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(query: RelationsQuery): void {
    this.relationsQueryFormGroup.reset(query || {}, {emitEvent: false});
  }
}
