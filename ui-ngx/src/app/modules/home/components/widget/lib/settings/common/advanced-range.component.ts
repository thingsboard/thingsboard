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

import { Component, DestroyRef, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IAliasController } from '@core/api/widget-api.models';
import { AdvancedColorRange } from '@shared/models/widget-settings.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-advanced-range',
    templateUrl: './advanced-range.component.html',
    styleUrls: ['./advanced-range.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AdvancedRangeComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AdvancedRangeComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Output()
  removeAdvancedRange = new EventEmitter();

  private modelValue: AdvancedColorRange;

  private propagateChange = (v: any) => { };

  public advancedRangeLevelFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.advancedRangeLevelFormGroup = this.fb.group({
      from: [null, []],
      to: [null, []],
      color: [null, [Validators.required]]
    });
    this.advancedRangeLevelFormGroup.valueChanges.pipe(
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
      this.advancedRangeLevelFormGroup.disable({emitEvent: false});
    } else {
      this.advancedRangeLevelFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AdvancedColorRange): void {
    this.modelValue = value;
    this.advancedRangeLevelFormGroup.patchValue(value, {emitEvent: false});
  }

  private updateModel() {
    this.modelValue = this.advancedRangeLevelFormGroup.value;
    this.propagateChange(this.modelValue);
  }
}
