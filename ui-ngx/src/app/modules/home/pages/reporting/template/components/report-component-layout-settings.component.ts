// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReportComponentLayoutSettings } from '@shared/models/report-component.models';

@Component({
    selector: 'tb-report-component-layout-settings',
    templateUrl: './report-component-layout-settings.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportComponentLayoutSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportComponentLayoutSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  private modelValue: ReportComponentLayoutSettings;

  private propagateChange = null;

  public settingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.settingsFormGroup = this.fb.group({
      paddings: [null],
      margins: [null],
      background: [null],
      borderWidth: [null, [Validators.min(0)]],
      borderRadius: [null, [Validators.min(0)]],
      borderColor: [null]
    });
    this.settingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.settingsFormGroup.disable({emitEvent: false});
    } else {
      this.settingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ReportComponentLayoutSettings): void {
    this.modelValue = value;
    this.settingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    this.modelValue = this.settingsFormGroup.value;
    this.propagateChange(this.modelValue);
  }
}
