// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { IntegrationCredentialType, IntegrationCredentialTypeTranslation } from '@shared/models/integration.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
    selector: 'tb-integration-credentials',
    templateUrl: 'integration-credentials.component.html',
    styleUrls: ['integration-credentials.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => IntegrationCredentialsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => IntegrationCredentialsComponent),
            multi: true,
        }],
    standalone: false
})
export class IntegrationCredentialsComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  integrationCredentialForm: UntypedFormGroup;
  hideSelectType = false;

  private allowCredentialTypesValue: IntegrationCredentialType[] = [];
  @Input()
  set allowCredentialTypes(types: IntegrationCredentialType[]) {
    this.allowCredentialTypesValue = types;
    this.hideSelectType = types.length === 1;
  }

  get allowCredentialTypes(): IntegrationCredentialType[] {
    return this.allowCredentialTypesValue;
  }

  private ignoreCaCertValue = false;
  @Input()
  set ignoreCaCert(value: boolean) {
    this.ignoreCaCertValue = coerceBooleanProperty(value);
    if (this.integrationCredentialForm) {
      if (this.ignoreCaCertValue) {
        this.integrationCredentialForm.get('caCert').clearValidators();
      } else {
        this.integrationCredentialForm.get('caCert').setValidators(Validators.required);
      }
      this.integrationCredentialForm.get('caCert').updateValueAndValidity({emitEvent: false});
    }
  }

  get ignoreCaCert(): boolean {
    return this.ignoreCaCertValue;
  }

  @Input() userNameLabel = 'integration.username';
  @Input() userNameRequired = 'integration.username-required';
  @Input() passwordLabel = 'integration.password';
  @Input() passwordRequired = 'integration.password-required';
  private passwordOptionalValue = false;
  get passwordOptional(): boolean {
    return this.passwordOptionalValue;
  }
  @Input()
  set passwordOptional(value: boolean) {
    this.passwordOptionalValue = coerceBooleanProperty(value);
  }

  IntegrationCredentialTypeTranslation = IntegrationCredentialTypeTranslation;
  IntegrationCredentialType = IntegrationCredentialType;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    this.integrationCredentialForm = this.fb.group({
      type: ['', Validators.required],
      username: [{value: '', disabled: true}, Validators.required],
      password: [{value: '', disabled: true}, this.passwordOptional ? null : Validators.required],
      caCertFileName: [{value: '', disabled: true}],
      caCert: [{value: '', disabled: true},  this.ignoreCaCert ? null : Validators.required],
      certFileName: [{value: '', disabled: true}],
      cert: [{value: '', disabled: true}, Validators.required],
      privateKeyFileName: [{value: '', disabled: true}],
      privateKey: [{value: '', disabled: true}, Validators.required],
      privateKeyPassword: [{value: '', disabled: true}],
      token: [{value: '', disabled: true}, Validators.required],
      sasKey: [{value: '', disabled: true}, Validators.required],
    });
    this.integrationCredentialForm.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(type => this.updatedValidation(type));
    this.integrationCredentialForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateModel(value));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.integrationCredentialForm.disable({emitEvent: false});
    } else {
      this.integrationCredentialForm.enable({emitEvent: false});
      this.integrationCredentialForm.get('type').updateValueAndValidity({onlySelf: true});
      if (!this.integrationCredentialForm.valid) {
        this.integrationCredentialForm.updateValueAndValidity();
      }
    }
  }

  writeValue(value) {
    this.integrationCredentialForm.patchValue(value, {emitEvent: false});
    if (!this.disabled) {
      this.integrationCredentialForm.get('type').updateValueAndValidity({onlySelf: true});
    }
  }

  private updatedValidation(type: IntegrationCredentialType) {
    this.integrationCredentialForm.disable({emitEvent: false});
    switch (type) {
      case IntegrationCredentialType.Anonymous:
        break;
      case IntegrationCredentialType.Basic:
        this.integrationCredentialForm.get('username').enable({emitEvent: false});
        this.integrationCredentialForm.get('password').enable({emitEvent: false});
        break;
      case IntegrationCredentialType.CertPEM:
        this.integrationCredentialForm.get('caCertFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCert').enable({emitEvent: false});
        this.integrationCredentialForm.get('certFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('cert').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKeyFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKey').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKeyPassword').enable({emitEvent: false});
        break;
      case IntegrationCredentialType.Token:
        this.integrationCredentialForm.get('token').enable({emitEvent: false});
        break;
      case IntegrationCredentialType.SAS:
        this.integrationCredentialForm.get('sasKey').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCertFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCert').enable({emitEvent: false});
        break;
    }
    this.integrationCredentialForm.get('type').enable({emitEvent: false});
  }

  private updateModel(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    if (this.integrationCredentialForm.status !== 'DISABLED' && !this.integrationCredentialForm.valid) {
      return {
        integrationCredential: {valid: false}
      }
    }
    return null;
  }
}
