// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  WidgetButtonAppearance,
  widgetButtonStates, widgetButtonStatesTranslations,
  widgetButtonTypeImages,
  widgetButtonTypes,
  widgetButtonTypeTranslations
} from '@shared/components/button/widget-button.models';
import { merge } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-widget-button-appearance',
    templateUrl: './widget-button-appearance.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => WidgetButtonAppearanceComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class WidgetButtonAppearanceComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled = false;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  @coerceBoolean()
  withAutoScale = true;

  @Input()
  @coerceBoolean()
  withBorderRadius = false;

  widgetButtonTypes = widgetButtonTypes;

  widgetButtonTypeTranslationMap = widgetButtonTypeTranslations;
  widgetButtonTypeImageMap = widgetButtonTypeImages;

  widgetButtonStates = widgetButtonStates;
  widgetButtonStateTranslationMap = widgetButtonStatesTranslations;

  modelValue: WidgetButtonAppearance;

  appearanceFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.appearanceFormGroup = this.fb.group({
      type: [null, []],
      showLabel: [null, []],
      label: [null, []],
      showIcon: [null, []],
      icon: [null, []],
      iconSize: [null, []],
      iconSizeUnit: [null, []],
      mainColor: [null, []],
      backgroundColor: [null, []]
    });
    if (this.withAutoScale) {
      this.appearanceFormGroup.addControl('autoScale', this.fb.control(null, []));
    }
    if (this.withBorderRadius) {
      this.appearanceFormGroup.addControl('borderRadius', this.fb.control(null, []));
    }
    const customStyle = this.fb.group({});
    for (const state of widgetButtonStates) {
      customStyle.addControl(state, this.fb.control(null, []));
    }
    this.appearanceFormGroup.addControl('customStyle', customStyle);
    this.appearanceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.appearanceFormGroup.get('showLabel').valueChanges,
          this.appearanceFormGroup.get('showIcon').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.appearanceFormGroup.disable({emitEvent: false});
    } else {
      this.appearanceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: WidgetButtonAppearance): void {
    this.modelValue = value;
    this.appearanceFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateModel() {
    this.modelValue = this.appearanceFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private updateValidators(): void {
    const showLabel: boolean = this.appearanceFormGroup.get('showLabel').value;
    const showIcon: boolean = this.appearanceFormGroup.get('showIcon').value;
    if (showLabel) {
      this.appearanceFormGroup.get('label').enable();
    } else {
      this.appearanceFormGroup.get('label').disable();
    }
    if (showIcon) {
      this.appearanceFormGroup.get('icon').enable();
      this.appearanceFormGroup.get('iconSize').enable();
      this.appearanceFormGroup.get('iconSizeUnit').enable();
    } else {
      this.appearanceFormGroup.get('icon').disable();
      this.appearanceFormGroup.get('iconSize').disable();
      this.appearanceFormGroup.get('iconSizeUnit').disable();
    }
  }
}
