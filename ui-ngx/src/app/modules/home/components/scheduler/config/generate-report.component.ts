// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ReportConfig } from '@shared/models/report.models';

@Component({
    selector: 'tb-generate-report-event-config',
    templateUrl: './generate-report.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => GenerateReportComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => GenerateReportComponent),
            multi: true
        }],
    standalone: false
})
export class GenerateReportComponent implements ControlValueAccessor, AfterViewInit, OnDestroy, Validator {

  modelValue: ReportConfig | null;

  generateReportFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.generateReportFormGroup = this.fb.group({
      reportConfig: [null, [Validators.required]]
    });

    this.generateReportFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.generateReportFormGroup.disable({emitEvent: false});
    } else {
      this.generateReportFormGroup.enable({emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngAfterViewInit(): void {
    if (!this.generateReportFormGroup.valid) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  writeValue(value: ReportConfig | null): void {
    this.modelValue = value;
    this.generateReportFormGroup.reset({
      reportConfig: this.modelValue
    },{emitEvent: false});
    this.updateEnabledState();
  }

  validate(): ValidationErrors | null {
    if (!this.generateReportFormGroup.valid) {
      return {
        generateReportForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private updateModel() {
    if (this.generateReportFormGroup.valid) {
      this.modelValue = this.generateReportFormGroup.value?.reportConfig || {};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
