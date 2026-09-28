// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Insets } from '@shared/models/report.models';

@Component({
    selector: 'tb-report-insets',
    templateUrl: './report-insets.component.html',
    styleUrls: ['./report-insets.component.scss'],
    encapsulation: ViewEncapsulation.None,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportInsetsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportInsetsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  private modelValue: Insets;

  private propagateChange = null;

  public insetsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.insetsFormGroup = this.fb.group(
      {
        left: [null, [Validators.min(0)]],
        right: [null, [Validators.min(0)]],
        top: [null, [Validators.min(0)]],
        bottom: [null, [Validators.min(0)]]
      }
    )
    this.insetsFormGroup.valueChanges.pipe(
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
      this.insetsFormGroup.disable({emitEvent: false});
    } else {
      this.insetsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Insets): void {
    this.modelValue = value;
    this.insetsFormGroup.patchValue(
      {
        left: value?.left,
        right: value?.right,
        top: value?.top,
        bottom: value?.bottom
      }, {emitEvent: false}
    );
  }

  private updateModel() {
    this.modelValue = this.insetsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
