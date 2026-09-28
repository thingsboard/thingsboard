// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
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
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageComponent } from '@shared/components/page.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { defaultEmailConfig, EmailConfig } from '@home/components/scheduler/config/config.models';

@Component({
    selector: 'tb-email-config',
    templateUrl: './email-config.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EmailConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => EmailConfigComponent),
            multi: true
        }],
    standalone: false
})
export class EmailConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  modelValue: EmailConfig | null;

  emailConfigFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  authUser = getCurrentAuthUser(this.store);

  private destroy$ = new Subject<void>();

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
    this.emailConfigFormGroup = this.fb.group({
      from: [null, [Validators.required]],
      to: [null, [Validators.required]],
      cc: [null, []],
      bcc: [null, []],
      subject: [null, [Validators.required]],
      body: [null, [Validators.required]]
    });

    this.emailConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    if (!this.emailConfigFormGroup.valid && !this.disabled) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.emailConfigFormGroup.disable({emitEvent: false});
    } else {
      this.emailConfigFormGroup.enable({emitEvent: false});
    }
    this.checkModel();
  }

  private checkModel() {
    if (!this.disabled && !this.modelValue) {
      this.modelValue = this.createDefaultEmailConfig();
      this.emailConfigFormGroup.reset(this.modelValue,{emitEvent: false});
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  writeValue(value: EmailConfig | null): void {
    this.modelValue = value;
    this.emailConfigFormGroup.reset(this.modelValue || undefined,{emitEvent: false});
  }

  validate(): ValidationErrors | null {
    if (!this.emailConfigFormGroup.valid && !this.disabled) {
      return {
        emailConfigForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private createDefaultEmailConfig(): EmailConfig {
    return {...defaultEmailConfig, from: this.authUser.sub};
  }

  private updateModel() {
    if (this.emailConfigFormGroup.valid) {
      const value = this.emailConfigFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
