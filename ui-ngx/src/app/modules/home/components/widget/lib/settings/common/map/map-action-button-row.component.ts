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

import { Component, EventEmitter, forwardRef, Output } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { MapActionButtonSettings } from '@shared/models/widget/maps/map.models';
import { WidgetAction, WidgetActionType, widgetType } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEmptyStr } from '@core/utils';

@Component({
    selector: 'tb-map-action-button-row',
    templateUrl: 'map-action-button-row.component.html',
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapActionButtonRowComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MapActionButtonRowComponent),
            multi: true
        }],
    standalone: false
})
export class MapActionButtonRowComponent implements ControlValueAccessor, Validator {

  @Output()
  buttonRemoved = new EventEmitter();

  mapActionButton = this.fb.group({
    label: [''],
    icon: [''],
    color: [''],
    action: this.fb.control<WidgetAction>(null)
  }, {validators: this.validateButtonConfig()});

  additionalWidgetActionTypes = [WidgetActionType.placeMapItem];
  readonly widgetType = widgetType;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.mapActionButton.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.propagateChange(value))
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void { }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.mapActionButton.disable({emitEvent: false});
    } else {
      this.mapActionButton.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.mapActionButton.valid ? null : {
      mapButtonAction: false
    };
  }

  writeValue(value: MapActionButtonSettings) {
   this.mapActionButton.patchValue(value, {emitEvent: false});
  }

  private validateButtonConfig() {
    return (c: FormGroup) => {
      return !c.value.icon && isEmptyStr(c.value.label)  ? {
        invalidButtonConfig: true
      } : null;
    };
  }
}
