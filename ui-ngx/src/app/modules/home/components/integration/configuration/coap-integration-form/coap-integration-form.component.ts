// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnInit } from '@angular/core';
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
import { coapBaseUrl, isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { CoapIntegration, CoapSecurityMode, coapSecurityModeTranslationsMap } from '@shared/models/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-coap-integration-form',
    templateUrl: './coap-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CoapIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CoapIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class CoapIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, OnInit, Validator {

  @Input() routingKey;

  coapSecurityModes = Object.keys(CoapSecurityMode);
  coapSecurityModeTranslations = coapSecurityModeTranslationsMap;

  coapIntegrationConfigForm: UntypedFormGroup;

  private propagateChangePending = false;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              private store: Store,
              private translate: TranslateService) {
    super();
  }

  ngOnInit() {
    this.coapIntegrationConfigForm = this.fb.group({
      baseUrl: [coapBaseUrl(false), [Validators.required]],
      dtlsBaseUrl: [coapBaseUrl(true), [Validators.required]],
      securityMode: [CoapSecurityMode.NO_SECURE, [Validators.required]],
      coapEndpoint: [{value: this.coapEndPointUrl(coapBaseUrl(false), this.routingKey), disabled: true}],
      dtlsCoapEndpoint: [{value: this.coapEndPointUrl(coapBaseUrl(true), this.routingKey), disabled: true}]
    });
    this.coapIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.coapIntegrationConfigForm.getRawValue());
    });
    this.coapIntegrationConfigForm.get('securityMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.integrationTypeChanged(value));
    this.coapIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.coapIntegrationConfigForm.get('coapEndpoint').patchValue(this.coapEndPointUrl(value, this.routingKey));
    });
    this.coapIntegrationConfigForm.get('dtlsBaseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.coapIntegrationConfigForm.get('dtlsCoapEndpoint').patchValue(this.coapEndPointUrl(value, this.routingKey));
    });
  }

  writeValue(value: CoapIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.coapIntegrationConfigForm.patchValue(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.coapIntegrationConfigForm.get('securityMode').updateValueAndValidity({onlySelf: true});
      }
    } else {
      this.propagateChangePending = true;
    }
  }

  onCoapEndpointCopied() {
    this.onEndpointCopied('integration.coap-endpoint-url-copied-message');
  }

  onDtlsCoapEndpointCopied() {
    this.onEndpointCopied('integration.coap-dtls-endpoint-url-copied-message');
  }

  get noSecureMode(): boolean {
    return this.checkSecurityMode(CoapSecurityMode.NO_SECURE);
  }

  get dtlsMode(): boolean {
    return this.checkSecurityMode(CoapSecurityMode.DTLS);
  }

  get mixedMode(): boolean {
    return this.checkSecurityMode(CoapSecurityMode.MIXED);
  }

  private checkSecurityMode(securityMode: CoapSecurityMode) {
    const coapSecurityMode = this.coapIntegrationConfigForm.get('securityMode').value;
    return coapSecurityMode === securityMode;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.coapIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.coapIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.coapIntegrationConfigForm.enable({emitEvent: false});
      this.coapIntegrationConfigForm.get('coapEndpoint').disable({emitEvent: false});
      this.coapIntegrationConfigForm.get('dtlsCoapEndpoint').disable({emitEvent: false});
      this.coapIntegrationConfigForm.get('securityMode').updateValueAndValidity({onlySelf: true});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.coapIntegrationConfigForm.valid ? null : {
      coapIntegrationConfigForm: {valid: false}
    };
  }

  private integrationTypeChanged(value: CoapSecurityMode) {
    switch (value) {
      case CoapSecurityMode.NO_SECURE:
        this.coapIntegrationConfigForm.get('dtlsBaseUrl').disable({emitEvent: false});
        this.coapIntegrationConfigForm.get('baseUrl').enable({emitEvent: false});
        break;
      case CoapSecurityMode.DTLS:
        this.coapIntegrationConfigForm.get('dtlsBaseUrl').enable({emitEvent: false});
        this.coapIntegrationConfigForm.get('baseUrl').disable({emitEvent: false});
        break;
      case CoapSecurityMode.MIXED:
        this.coapIntegrationConfigForm.get('dtlsBaseUrl').enable({emitEvent: false});
        this.coapIntegrationConfigForm.get('baseUrl').enable({emitEvent: false});
        break;
    }
  }

  private coapEndPointUrl(baseUrl: string, key = ''): string {
    return `${baseUrl}/i/${key}`;
  }

  private onEndpointCopied(key: string) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(key),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left',
        target: 'integrationRoot'
      }));
  }

  getCoapSecurityModeTranslations(type: string) {
    return this.coapSecurityModeTranslations.get(type as CoapSecurityMode);
  }
}
