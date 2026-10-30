// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { baseUrl, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import {
  IntegrationCredentialType,
  IntegrationType,
  KpnIntegration,
} from '@shared/models/integration.models';
import { integrationEndPointUrl, privateNetworkAddressValidator } from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';

interface KpnIntegrationFormValue extends Omit<KpnIntegration, 'enableSecurity' | 'headersFilter'> {
  securityHeader: string;
  additionalHeadersFilter: {[key: string]: string};
}

@Component({
    selector: 'tb-kpn-integration-form',
    templateUrl: './kpn-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => KpnIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => KpnIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class KpnIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  kpnIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  httpsDestinationSettingsExpanded = false;

  noSecurity = true;

  @Input()
  routingKey: string;

  @Input()
  set isSetDownlink(value: boolean) {
    if (this.isSetDownlinkValue !== value) {
      this.isSetDownlinkValue = value;
      this.updateCredentialsValidators();
    }
  }

  get isSetDownlink(): boolean {
    return this.isSetDownlinkValue;
  }

  private integrationType = IntegrationType.KPN;
  private securityHeaderName = 'X-Things-Secret';
  private isSetDownlinkValue = false;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };
  private validatorChange = () => { };

  constructor(private fb: UntypedFormBuilder,
              private store: Store<AppState>,
              private translate: TranslateService) {
    super();
  }

  ngOnInit() {
    const baseURLValidators = [Validators.required];
    if (!this.allowLocalNetwork) {
      baseURLValidators.push(privateNetworkAddressValidator);
    }
    this.kpnIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{
        value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey),
        disabled: true
      }],
      securityHeader: [''],
      destinationSharedSecret: [''],
      additionalHeadersFilter: [{}, (control: AbstractControl) => this.additionalHeadersFilterValidator(control)],
      apiId: [],
      apiKey: []
    });
    this.kpnIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.kpnIntegrationConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
    this.kpnIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const value = this.kpnIntegrationConfigForm.getRawValue();
      this.updateNoSecurity(value);
      this.updateModels(value);
    });
    this.kpnIntegrationConfigForm.removeControl('replaceNoContentToOk', {emitEvent: false});
    this.updateCredentialsValidators();
  }

  writeValue(value: KpnIntegration) {
    if (isDefinedAndNotNull(value)) {
      const formValue = this.configToFormValue(value);
      this.httpsDestinationSettingsExpanded = isNotEmptyStr(formValue.destinationSharedSecret) ||
        Object.keys(formValue.additionalHeadersFilter).length > 0;
      this.kpnIntegrationConfigForm.reset(formValue, {emitEvent: false});
      this.updateNoSecurity(this.kpnIntegrationConfigForm.getRawValue());
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.kpnIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  registerOnValidatorChange(fn: () => void) {
    this.validatorChange = fn;
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.kpnIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.kpnIntegrationConfigForm.enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.kpnIntegrationConfigForm.valid ? null : {
      baseHttpIntegrationConfigForm: {valid: false}
    };
  }

  onHttpEndpointCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.http-endpoint-url-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left',
        target: 'integrationRoot'
      }));
  }

  private updateNoSecurity({securityHeader, destinationSharedSecret, additionalHeadersFilter}: KpnIntegrationFormValue) {
    this.noSecurity = !isNotEmptyStr(securityHeader) && !isNotEmptyStr(destinationSharedSecret) &&
      !Object.keys(additionalHeadersFilter).length;
  }

  private updateModels(value: KpnIntegrationFormValue) {
    this.propagateChange(this.formValueToConfig(value));
  }

  private configToFormValue({enableSecurity, headersFilter, ...config}: KpnIntegration): KpnIntegrationFormValue {
    const additionalHeadersFilter = enableSecurity ? {...headersFilter} : {};
    const securityHeaderKey = Object.keys(additionalHeadersFilter).find(key => this.isSecurityHeader(key));
    let securityHeader = '';
    if (securityHeaderKey) {
      securityHeader = additionalHeadersFilter[securityHeaderKey];
      delete additionalHeadersFilter[securityHeaderKey];
    }
    return {...config, securityHeader, additionalHeadersFilter};
  }

  private formValueToConfig({securityHeader, additionalHeadersFilter, ...config}: KpnIntegrationFormValue): KpnIntegration {
    const headersFilter = {...additionalHeadersFilter};
    if (isNotEmptyStr(securityHeader)) {
      headersFilter[this.securityHeaderName] = securityHeader.trim();
    }
    const enableSecurity = Object.keys(headersFilter).length > 0;
    return {...config, enableSecurity, headersFilter};
  }

  private additionalHeadersFilterValidator(control: AbstractControl): ValidationErrors | null {
    return Object.keys(control.value ?? {}).some(key => this.isSecurityHeader(key)) ? {securityHeaderDuplicate: true} : null;
  }

  private isSecurityHeader(key: string): boolean {
    return key.toLowerCase() === this.securityHeaderName.toLowerCase();
  }

  private updateCredentialsValidators() {
    if (this.kpnIntegrationConfigForm) {
      const validators = this.isSetDownlink ? [Validators.required] : [];
      this.kpnIntegrationConfigForm.get('apiId').setValidators(validators);
      this.kpnIntegrationConfigForm.get('apiKey').setValidators(validators);
      this.kpnIntegrationConfigForm.get('apiId').updateValueAndValidity({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiKey').updateValueAndValidity({emitEvent: false});
      this.validatorChange();
    }
  }
}
