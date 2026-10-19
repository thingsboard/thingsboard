// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators,
  ValidationErrors,
  NG_VALIDATORS,
  Validator
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-generate-dashboard-report-event-config',
    templateUrl: './generate-dashboard-report.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => GenerateDashboardReportComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => GenerateDashboardReportComponent),
            multi: true
        }],
    standalone: false
})
export class GenerateDashboardReportComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  modelValue: SchedulerEventConfiguration | null;

  generateReportFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.generateReportFormGroup = this.fb.group({
      msgBody: this.fb.group(
        {
          reportConfig: [null, []],
          sendEmail: [false, []],
          emailConfig: [null, [Validators.required]]
        }
      )
    });

    this.generateReportFormGroup.get('msgBody.sendEmail').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnabledState();
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
      const sendEmail: boolean = this.generateReportFormGroup.get('msgBody.sendEmail').value;
      if (sendEmail) {
        this.generateReportFormGroup.get('msgBody.emailConfig').enable({emitEvent: false});
      } else {
        this.generateReportFormGroup.get('msgBody.emailConfig').disable({emitEvent: false});
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
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

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = value;
    this.generateReportFormGroup.reset(this.modelValue || undefined,{emitEvent: false});
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
      const value = this.generateReportFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
