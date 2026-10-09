// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Heading } from '@shared/models/report-component.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-report-heading',
    templateUrl: './report-heading.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportHeadingComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportHeadingComponent implements OnInit, ControlValueAccessor {

  @Input()
  variableNames: string[] = [];

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  withLayout = true;

  private modelValue: Heading;

  private propagateChange = null;

  public headingFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.headingFormGroup = this.fb.group(
      {
        text: [null],
      }
    )
    if (this.withLayout) {
      this.headingFormGroup.addControl('font', this.fb.control(null));
      this.headingFormGroup.addControl('color', this.fb.control(null));
      this.headingFormGroup.addControl('textAlignment', this.fb.control(null));
      this.headingFormGroup.addControl('verticalAlignment', this.fb.control(null));
      this.headingFormGroup.addControl('height', this.fb.control(null));
    }
    this.headingFormGroup.valueChanges.pipe(
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
      this.headingFormGroup.disable({emitEvent: false});
    } else {
      this.headingFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Heading): void {
    this.modelValue = value;
    this.headingFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    this.modelValue = this.headingFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
