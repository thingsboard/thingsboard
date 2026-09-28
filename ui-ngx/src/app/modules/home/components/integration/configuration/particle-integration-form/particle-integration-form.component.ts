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
import { IntegrationCredentialType, IntegrationType, ParticleIntegration } from '@shared/models/integration.models';
import { integrationEndPointUrl, privateNetworkAddressValidator } from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { merge } from 'rxjs';

@Component({
    selector: 'tb-particle-integration-form',
    templateUrl: './particle-integration-form.component.html',
    styleUrls: ['./particle-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ParticleIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ParticleIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class ParticleIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  particleIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  @Input()
  routingKey: string;

  private integrationType = IntegrationType.PARTICLE;

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
    this.particleIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{
        value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey),
        disabled: true
      }],
      enableSecurity: [false],
      headersFilter: [{}],
      allowDownlink: [false],
      credentials: [{value: {type: IntegrationCredentialType.Token}, disabled: true}],
    });
    this.particleIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.particleIntegrationConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
      this.particleIntegrationConfigForm.get('allowDownlink').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnableFields();
    });
    this.particleIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.particleIntegrationConfigForm.getRawValue());
    });
    this.particleIntegrationConfigForm.removeControl('replaceNoContentToOk', {emitEvent: false});
  }

  writeValue(value: ParticleIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.particleIntegrationConfigForm.patchValue(value, {emitEvent: false});
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
        this.updateModels(this.particleIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.particleIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.particleIntegrationConfigForm.enable({emitEvent: false});
      this.updateEnableFields();
      this.particleIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.particleIntegrationConfigForm.valid ? null : {
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
    const allowDownlink = this.particleIntegrationConfigForm.get('allowDownlink').value;
    if (allowDownlink) {
      this.particleIntegrationConfigForm.get('credentials').enable({emitEvent: false});
    } else {
      this.particleIntegrationConfigForm.get('credentials').disable({emitEvent: false});
    }
  }

  updatedValidationPrivateNetwork() {
    if (this.particleIntegrationConfigForm) {
      if (this.allowLocalNetwork) {
        this.particleIntegrationConfigForm.get('baseUrl').removeValidators(privateNetworkAddressValidator);
      } else {
        this.particleIntegrationConfigForm.get('baseUrl').addValidators(privateNetworkAddressValidator);
      }
      this.particleIntegrationConfigForm.get('baseUrl').updateValueAndValidity({emitEvent: false});
    }
  }
}
