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

import { ValueSourceProperty } from '@home/components/widget/lib/settings/common/value-source.component';
import {
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
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
import { TbFlotKeyThreshold } from '@home/components/widget/lib/flot-widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-flot-threshold',
    templateUrl: './flot-threshold.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FlotThresholdComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class FlotThresholdComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Output()
  removeThreshold = new EventEmitter();

  private modelValue: TbFlotKeyThreshold;

  private propagateChange = null;

  public thresholdFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.thresholdFormGroup = this.fb.group({
      valueSource: [null, []],
      lineWidth: [null, [Validators.min(0)]],
      color: [null, []]
    });
    this.thresholdFormGroup.valueChanges.pipe(
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
      this.thresholdFormGroup.disable({emitEvent: false});
    } else {
      this.thresholdFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TbFlotKeyThreshold): void {
    this.modelValue = value;
    const valueSource: ValueSourceProperty = {
      valueSource: value?.thresholdValueSource,
      entityAlias: value?.thresholdEntityAlias,
      attribute: value?.thresholdAttribute,
      value: value?.thresholdValue
    };
    this.thresholdFormGroup.patchValue(
      {valueSource, lineWidth: value?.lineWidth, color: value?.color}, {emitEvent: false}
    );
  }

  private updateModel() {
    const value: {valueSource: ValueSourceProperty; lineWidth: number; color: string} = this.thresholdFormGroup.value;
    this.modelValue = {
      thresholdValueSource: value?.valueSource?.valueSource,
      thresholdEntityAlias: value?.valueSource?.entityAlias,
      thresholdAttribute: value?.valueSource?.attribute,
      thresholdValue: value?.valueSource?.value,
      lineWidth: value?.lineWidth,
      color: value?.color
    };
    this.propagateChange(this.modelValue);
  }
}
