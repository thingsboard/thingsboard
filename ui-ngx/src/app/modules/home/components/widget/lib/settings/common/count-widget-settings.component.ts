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

import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {
  alarmCountCardLayoutImages,
  CountCardLayout,
  countCardLayouts,
  countCardLayoutTranslations,
  CountWidgetSettings, entityCountCardLayoutImages
} from '@home/components/widget/lib/count/count-widget.models';
import {PageComponent} from '@shared/components/page.component';
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  valueCardLayoutImages,
  valueCardLayoutTranslations
} from '@home/components/widget/lib/cards/value-card-widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-count-widget-settings',
    templateUrl: './count-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CountWidgetSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CountWidgetSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @coerceBoolean()
  @Input()
  alarmElseEntity: boolean;

  @Input()
  predefinedValues: string[];
  
  private propagateChange = null;

  countCardLayouts = countCardLayouts;

  countCardLayoutTranslationMap = countCardLayoutTranslations;
  countCardLayoutImageMap: Map<CountCardLayout, string>;

  countWidgetConfigForm: UntypedFormGroup;
  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.countCardLayoutImageMap = this.alarmElseEntity ? alarmCountCardLayoutImages : entityCountCardLayoutImages;
    this.countWidgetConfigForm = this.fb.group({
      layout: [null, []],
      autoScale: [null, []],

      showLabel: [null, []],
      label: [null, []],
      labelFont: [null, []],
      labelColor: [null, []],

      showIcon: [null, []],
      iconSize: [null, [Validators.min(0)]],
      iconSizeUnit: [null, []],
      icon: [null, []],
      iconColor: [null, []],

      showIconBackground: [null, []],
      iconBackgroundSize: [null, [Validators.min(0)]],
      iconBackgroundSizeUnit: [null, []],
      iconBackgroundColor: [null, []],

      valueFont: [null, []],
      valueColor: [null, []],

      showChevron: [null, []],
      chevronSize: [null, [Validators.min(0)]],
      chevronSizeUnit: [null, []],
      chevronColor: [null, []],
    });
    this.countWidgetConfigForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    for (const trigger of ['showLabel', 'showIcon', 'showIconBackground', 'showChevron']) {
      const path = trigger.split('.');
      let control: AbstractControl = this.countWidgetConfigForm;
      for (const part of path) {
        control = this.countWidgetConfigForm.get(part);
      }
      control.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators();
      });
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }


  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.countWidgetConfigForm.disable({emitEvent: false});
    } else {
      this.countWidgetConfigForm.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: CountWidgetSettings): void {
    this.countWidgetConfigForm.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateModel() {
    const value: CountWidgetSettings = this.countWidgetConfigForm.value;
    this.propagateChange(value);
  }

  protected updateValidators() {
    const showLabel: boolean = this.countWidgetConfigForm.get('showLabel').value;
    const showIcon: boolean = this.countWidgetConfigForm.get('showIcon').value;
    const showIconBackground: boolean = this.countWidgetConfigForm.get('showIconBackground').value;
    const showChevron: boolean = this.countWidgetConfigForm.get('showChevron').value;

    if (showLabel) {
      this.countWidgetConfigForm.get('label').enable({emitEvent: false});
      this.countWidgetConfigForm.get('labelFont').enable({emitEvent: false});
      this.countWidgetConfigForm.get('labelColor').enable({emitEvent: false});
    } else {
      this.countWidgetConfigForm.get('label').disable({emitEvent: false});
      this.countWidgetConfigForm.get('labelFont').disable({emitEvent: false});
      this.countWidgetConfigForm.get('labelColor').disable({emitEvent: false});
    }

    if (showIcon) {
      this.countWidgetConfigForm.get('iconSize').enable({emitEvent: false});
      this.countWidgetConfigForm.get('iconSizeUnit').enable({emitEvent: false});
      this.countWidgetConfigForm.get('icon').enable({emitEvent: false});
      this.countWidgetConfigForm.get('iconColor').enable({emitEvent: false});
    } else {
      this.countWidgetConfigForm.get('iconSize').disable({emitEvent: false});
      this.countWidgetConfigForm.get('iconSizeUnit').disable({emitEvent: false});
      this.countWidgetConfigForm.get('icon').disable({emitEvent: false});
      this.countWidgetConfigForm.get('iconColor').disable({emitEvent: false});
    }

    if (showIconBackground) {
      this.countWidgetConfigForm.get('iconBackgroundSize').enable({emitEvent: false});
      this.countWidgetConfigForm.get('iconBackgroundSizeUnit').enable({emitEvent: false});
      this.countWidgetConfigForm.get('iconBackgroundColor').enable({emitEvent: false});
    } else {
      this.countWidgetConfigForm.get('iconBackgroundSize').disable({emitEvent: false});
      this.countWidgetConfigForm.get('iconBackgroundSizeUnit').disable({emitEvent: false});
      this.countWidgetConfigForm.get('iconBackgroundColor').disable({emitEvent: false});
    }

    if (showChevron) {
      this.countWidgetConfigForm.get('chevronSize').enable({emitEvent: false});
      this.countWidgetConfigForm.get('chevronSizeUnit').enable({emitEvent: false});
      this.countWidgetConfigForm.get('chevronColor').enable({emitEvent: false});
    } else {
      this.countWidgetConfigForm.get('chevronSize').disable({emitEvent: false});
      this.countWidgetConfigForm.get('chevronSizeUnit').disable({emitEvent: false});
      this.countWidgetConfigForm.get('chevronColor').disable({emitEvent: false});
    }
  }

  protected readonly valueCardLayoutTranslations = valueCardLayoutTranslations;
  protected readonly valueCardLayoutImages = valueCardLayoutImages;
}
