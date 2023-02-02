///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { isNumber } from '@core/utils';

export interface GaugeHighlight {
  from: number;
  to: number;
  color: string;
}

@Component({
  selector: 'tb-gauge-highlight',
  templateUrl: './gauge-highlight.component.html',
  styleUrls: ['./gauge-highlight.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GaugeHighlightComponent),
      multi: true
    }
  ]
})
export class GaugeHighlightComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeHighlight = new EventEmitter();

  private modelValue: GaugeHighlight;

  private propagateChange = null;

  public gaugeHighlightFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.gaugeHighlightFormGroup = this.fb.group({
      from: [null, []],
      to: [null, []],
      color: [null, []]
    });
    this.gaugeHighlightFormGroup.valueChanges.subscribe(() => {
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
      this.gaugeHighlightFormGroup.disable({emitEvent: false});
    } else {
      this.gaugeHighlightFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: GaugeHighlight): void {
    this.modelValue = value;
    this.gaugeHighlightFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  highlightRangeText(): string {
    const value: GaugeHighlight = this.gaugeHighlightFormGroup.value;
    const from = isNumber(value.from) ? value.from : 0;
    const to = isNumber(value.to) ? value.to : 0;
    return `${from} - ${to}`;
  }

  private updateModel() {
    const value: GaugeHighlight = this.gaugeHighlightFormGroup.value;
    this.modelValue = value;
    if (this.gaugeHighlightFormGroup.valid) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }
}
