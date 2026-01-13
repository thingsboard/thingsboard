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

import { Component, DestroyRef, forwardRef, HostBinding, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface WidgetFont {
  family: string;
  size: number;
  style: 'normal' | 'italic' | 'oblique';
  weight: 'normal' | 'bold' | 'bolder' | 'lighter' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900';
  color: string;
  shadowColor?: string;
}

@Component({
  selector: 'tb-widget-font',
  templateUrl: './widget-font.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetFontComponent),
      multi: true
    }
  ]
})
export class WidgetFontComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.display') display = 'block';

  @Input()
  disabled: boolean;

  @Input()
  hasShadowColor = false;

  @Input()
  sizeTitle = 'widgets.widget-font.size';

  private modelValue: WidgetFont;

  private propagateChange = null;

  public widgetFontFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.widgetFontFormGroup = this.fb.group({
      family: [null, []],
      size: [null, [Validators.min(1)]],
      style: [null, []],
      weight: [null, []],
      color: [null, []]
    });
    if (this.hasShadowColor) {
      this.widgetFontFormGroup.addControl('shadowColor', this.fb.control(null, []));
    }
    this.widgetFontFormGroup.valueChanges.pipe(
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
      this.widgetFontFormGroup.disable({emitEvent: false});
    } else {
      this.widgetFontFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: WidgetFont): void {
    this.modelValue = value;
    this.widgetFontFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    const value: WidgetFont = this.widgetFontFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
