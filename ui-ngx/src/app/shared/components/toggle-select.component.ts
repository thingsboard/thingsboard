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

import { Component, forwardRef, HostBinding, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { _ToggleBase, ToggleHeaderAppearance } from '@shared/components/toggle-header.component';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-toggle-select',
    templateUrl: './toggle-select.component.html',
    styleUrls: ['./toggle-select.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ToggleSelectComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ToggleSelectComponent extends _ToggleBase implements ControlValueAccessor {

  @HostBinding('style.maxWidth')
  get maxWidth() { return '100%'; }

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  selectMediaBreakpoint: string;

  @Input()
  appearance: ToggleHeaderAppearance = 'stroked';

  @Input()
  @coerceBoolean()
  disablePagination = false;

  @Input()
  @coerceBoolean()
  fillHeight = false;

  @Input()
  @coerceBoolean()
  extraPadding = false;

  @Input()
  @coerceBoolean()
  primaryBackground = false;

  modelValue: any;

  private propagateChange = null;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    this.modelValue = value;
  }

  updateModel(value: any) {
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
