///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, forwardRef, HostBinding, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';

export interface LabelWidgetFont {
  family: string;
  size: number;
  style: 'normal' | 'italic' | 'oblique';
  weight: 'normal' | 'bold' | 'bolder' | 'lighter' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900';
  color: string;
}

@Component({
  selector: 'tb-label-widget-font',
  templateUrl: './label-widget-font.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LabelWidgetFontComponent),
      multi: true
    }
  ]
})
export class LabelWidgetFontComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.display') display = 'block';

  @Input()
  disabled: boolean;

  private modelValue: LabelWidgetFont;

  private propagateChange = null;

  public labelWidgetFontFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.labelWidgetFontFormGroup = this.fb.group({
      family: [null, []],
      size: [null, [Validators.min(1)]],
      style: [null, []],
      weight: [null, []],
      color: [null, []]
    });
    this.labelWidgetFontFormGroup.valueChanges.subscribe(() => {
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
      this.labelWidgetFontFormGroup.disable({emitEvent: false});
    } else {
      this.labelWidgetFontFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: LabelWidgetFont): void {
    this.modelValue = value;
    this.labelWidgetFontFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    const value: LabelWidgetFont = this.labelWidgetFontFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
