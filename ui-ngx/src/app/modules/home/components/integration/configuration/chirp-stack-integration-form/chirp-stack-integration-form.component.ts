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
import { baseUrl, isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { ChipStackIntegration, IntegrationType } from '@shared/models/integration.models';
import {
  integrationEndPointUrl,
  privateNetworkAddressValidator
} from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';

@Component({
    selector: 'tb-chirp-stack-integration-form',
    templateUrl: './chirp-stack-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ChirpStackIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ChirpStackIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class ChirpStackIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  chirpStackIntegrationConfigForm: UntypedFormGroup;

  @Input()
  routingKey: string;

  private propagateChange = (v: any) => { };
  private propagateChangePending = false;

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
    this.chirpStackIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{value: this.endPointUrl(baseUrl()), disabled: true}],
      applicationServerUrl: [null],
      applicationServerAPIToken: [null],
      useAPI4Plus: [true],
      allowDownlink: [false]
    });
    this.chirpStackIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.chirpStackIntegrationConfigForm.get('httpEndpoint').patchValue(this.endPointUrl(value));
    });
    this.chirpStackIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.chirpStackIntegrationConfigForm.getRawValue());
    });

    this.chirpStackIntegrationConfigForm.get('allowDownlink').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateDownlinkControlsState());
  }

  writeValue(value: ChipStackIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      const config = value.clientConfiguration;
      this.chirpStackIntegrationConfigForm.reset(config, {emitEvent: false});
      if (!isDefinedAndNotNull(config.allowDownlink)
        && (isDefinedAndNotNull(config.applicationServerUrl) || isDefinedAndNotNull(config.applicationServerAPIToken))) {
        this.chirpStackIntegrationConfigForm.get('allowDownlink').setValue(true, {emitEvent: false});
      }
      if (!this.disabled) {
        this.updateDownlinkControlsState();
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
        this.updateModels(this.chirpStackIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.chirpStackIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.chirpStackIntegrationConfigForm.enable({emitEvent: false});
      this.chirpStackIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
      this.updateDownlinkControlsState();
    }
  }

  private endPointUrl(url: string): string {
    return integrationEndPointUrl(IntegrationType.CHIRPSTACK, url, this.routingKey);
  }

  private updateModels(value) {
    if (!value.allowDownlink) {
      value = {...value, applicationServerUrl: null, applicationServerAPIToken: null};
    }
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.chirpStackIntegrationConfigForm.valid ? null : {
      chirpStackIntegrationConfigForm: {valid: false}
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

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.chirpStackIntegrationConfigForm?.get('baseUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.chirpStackIntegrationConfigForm?.get('baseUrl').addValidators(privateNetworkAddressValidator);
    }
    this.chirpStackIntegrationConfigForm?.get('baseUrl').updateValueAndValidity({emitEvent: false});
  }

  updateDownlinkControlsState() {
    const allowDownlink = this.chirpStackIntegrationConfigForm.get('allowDownlink').value;
    if (allowDownlink) {
      this.chirpStackIntegrationConfigForm.get('applicationServerAPIToken').enable({emitEvent: false});
      this.chirpStackIntegrationConfigForm.get('applicationServerUrl').enable({emitEvent: false});
      this.chirpStackIntegrationConfigForm.get('applicationServerAPIToken').setValidators(Validators.required);
      this.chirpStackIntegrationConfigForm.get('applicationServerUrl').setValidators(Validators.required);
    } else {
      this.chirpStackIntegrationConfigForm.get('applicationServerAPIToken').disable({emitEvent: false});
      this.chirpStackIntegrationConfigForm.get('applicationServerUrl').disable({emitEvent: false});
      this.chirpStackIntegrationConfigForm.get('applicationServerAPIToken').clearValidators();
      this.chirpStackIntegrationConfigForm.get('applicationServerUrl').clearValidators();
    }
    this.chirpStackIntegrationConfigForm.get('applicationServerAPIToken').updateValueAndValidity({emitEvent: false});
    this.chirpStackIntegrationConfigForm.get('applicationServerUrl').updateValueAndValidity({emitEvent: false});
  }
}
