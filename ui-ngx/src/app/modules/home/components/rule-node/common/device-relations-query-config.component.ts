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
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { PageComponent } from '@shared/components/page.component';
import { EntitySearchDirection, entitySearchDirectionTranslations } from '@app/shared/models/relation.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface DeviceRelationsQuery {
  fetchLastLevelOnly: boolean;
  direction: EntitySearchDirection;
  maxLevel?: number;
  relationType?: string;
  deviceTypes: string[];
}

@Component({
    selector: 'tb-device-relations-query-config',
    templateUrl: './device-relations-query-config.component.html',
    styleUrls: ['./device-relations-query-config.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceRelationsQueryConfigComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DeviceRelationsQueryConfigComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  directionTypes: Array<EntitySearchDirection> = Object.values(EntitySearchDirection);
  directionTypeTranslations = entitySearchDirectionTranslations;

  entityType = EntityType;

  deviceRelationsQueryFormGroup: FormGroup;

  private propagateChange = null;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.deviceRelationsQueryFormGroup = this.fb.group({
      fetchLastLevelOnly: [false, []],
      direction: [null, [Validators.required]],
      maxLevel: [null, [Validators.min(1)]],
      relationType: [null],
      deviceTypes: [null, [Validators.required]]
    });
    this.deviceRelationsQueryFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((query: DeviceRelationsQuery) => {
      if (this.deviceRelationsQueryFormGroup.valid) {
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
      this.deviceRelationsQueryFormGroup.disable({emitEvent: false});
    } else {
      this.deviceRelationsQueryFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(query: DeviceRelationsQuery): void {
    this.deviceRelationsQueryFormGroup.reset(query, {emitEvent: false});
  }
}
