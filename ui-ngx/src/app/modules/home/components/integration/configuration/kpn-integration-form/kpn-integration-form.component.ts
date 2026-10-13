// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnInit } from '@angular/core';
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
import { baseUrl, isDefinedAndNotNull } from '@core/utils';
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

@Component({
    selector: 'tb-kpn-integration-form',
    templateUrl: './kpn-integration-form.component.html',
    styleUrls: ['./kpn-integration-form.component.scss'],
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

  @Input()
  routingKey: string;

  private integrationType = IntegrationType.KPN;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

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
      destinationSharedSecret: [""],
      httpEndpoint: [{
        value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey),
        disabled: true
      }],
      enableSecurity: [false],
      headersFilter: [{}],
      allowDownlink: [false],
      apiId:[],
      apiKey:[]
    });
    this.kpnIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.kpnIntegrationConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
      this.kpnIntegrationConfigForm.get('allowDownlink').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnableFields();
    });
    this.kpnIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.kpnIntegrationConfigForm.getRawValue());
    });
    this.kpnIntegrationConfigForm.removeControl('replaceNoContentToOk', {emitEvent: false});
  }

  writeValue(value: KpnIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.kpnIntegrationConfigForm.reset(value, {emitEvent: false});
      if (!this.disabled) {
        this.updateEnableFields();
      }
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

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.kpnIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.kpnIntegrationConfigForm.enable({emitEvent: false});
      this.updateEnableFields();
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

  private updateModels(value) {
    this.propagateChange(value);
  }

  private updateEnableFields() {
    const allowDownlink = this.kpnIntegrationConfigForm.get('allowDownlink').value;
    if (allowDownlink) {
      this.kpnIntegrationConfigForm.get('apiId').enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiKey').enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiId').setValidators(Validators.required);
      this.kpnIntegrationConfigForm.get('apiKey').setValidators(Validators.required);
    } else {
      this.kpnIntegrationConfigForm.get('apiId').disable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiKey').disable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiId').clearValidators();
      this.kpnIntegrationConfigForm.get('apiKey').clearValidators();

    }
    this.kpnIntegrationConfigForm.get('apiId').updateValueAndValidity({emitEvent: false});
    this.kpnIntegrationConfigForm.get('apiKey').updateValueAndValidity({emitEvent: false});
    this.kpnIntegrationConfigForm.get('destinationSharedSecret').updateValueAndValidity({emitEvent: false});
  }
}
