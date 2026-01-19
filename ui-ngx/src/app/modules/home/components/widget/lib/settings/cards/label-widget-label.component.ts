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
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { WidgetFont } from '@home/components/widget/lib/settings/common/widget-font.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface LabelWidgetLabel {
  pattern: string;
  x: number;
  y: number;
  backgroundColor: string;
  font: WidgetFont;
}

@Component({
  selector: 'tb-label-widget-label',
  templateUrl: './label-widget-label.component.html',
  styleUrls: ['./label-widget-label.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LabelWidgetLabelComponent),
      multi: true
    }
  ]
})
export class LabelWidgetLabelComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeLabel = new EventEmitter();

  private modelValue: LabelWidgetLabel;

  private propagateChange = null;

  public labelWidgetLabelFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.labelWidgetLabelFormGroup = this.fb.group({
      pattern: [null, [Validators.required]],
      x: [null, [Validators.min(0), Validators.max(100)]],
      y: [null, [Validators.min(0), Validators.max(100)]],
      backgroundColor: [null, []],
      font: [null, []]
    });
    this.labelWidgetLabelFormGroup.valueChanges.pipe(
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
      this.labelWidgetLabelFormGroup.disable({emitEvent: false});
    } else {
      this.labelWidgetLabelFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: LabelWidgetLabel): void {
    this.modelValue = value;
    this.labelWidgetLabelFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    const value: LabelWidgetLabel = this.labelWidgetLabelFormGroup.value;
    this.modelValue = value;
    if (this.labelWidgetLabelFormGroup.valid) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }
}
