// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull, isNumber } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import {
  privateNetworkAddressValidator,
  ThingsStartHostType,
  ThingsStartHostTypeTranslation,
  ttnVersion,
  ttnVersionMap
} from '@home/components/integration/integration.models';
import { IntegrationCredentialType, TtnIntegration, } from '@shared/models/integration.models';
import { DEFAULT_MQTT_VERSION, MqttVersion } from '@shared/models/mqtt.models';

@Component({
    selector: 'tb-ttn-integration-form',
    templateUrl: './tts-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TtnIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TtnIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class TtnIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  ttnIntegrationConfigForm: FormGroup;

  hostEdit: FormControl<string>;
  apiVersion: FormControl<boolean>;

  ThingsStartHostType = ThingsStartHostType;
  ThingsStartHostTypes = Object.values(ThingsStartHostType).filter((v): v is ThingsStartHostType => isNumber(v));
  ThingsStartHostTypeTranslation = ThingsStartHostTypeTranslation;
  IntegrationCredentialType = IntegrationCredentialType;
  MqttVersion = MqttVersion;

  hostRegionSuffix = '.cloud.thethings.network';
  hideSelectVersion = false;

  private downlinkPattern = ttnVersionMap.get(ttnVersion.v3).downlinkPattern;
  private propagateChange = (v: any) => { };

  constructor(protected fb: FormBuilder) {
    super();
    this.hostEdit = this.fb.control('', Validators.required);
    this.apiVersion = this.fb.control(true);
    this.ttnIntegrationConfigForm = this.fb.group({
      clientConfiguration: this.fb.group({
        host: ['', Validators.required],
        customHost: [false],
        port: [8883],
        ssl: [true],
        maxBytesInMessage: [32368, [Validators.min(1), Validators.max(256000000)]],
        connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
        protocolVersion: [DEFAULT_MQTT_VERSION],
        credentials: [{
          type: 'basic',
          username: '',
          password: ''
        }]
      }),
      topicFilters: [{value: ttnVersionMap.get(ttnVersion.v3).uplinkTopic, disabled: true}, Validators.required],
      downlinkTopicPattern: [this.downlinkPattern, Validators.required]
    });

    this.hostEdit.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.ttnIntegrationConfigForm.get('clientConfiguration.host').patchValue(this.buildHostName(value));
      if (this.ttnIntegrationConfigForm.get('clientConfiguration.host').pristine) {
        this.ttnIntegrationConfigForm.get('clientConfiguration.host').markAsDirty();
      }
    });

    this.apiVersion.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateTtnVersionState(Number(value));
    });

    this.ttnIntegrationConfigForm.get('clientConfiguration.customHost').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.hostEdit.patchValue('');
      this.hostEdit.markAsUntouched();
      this.hostEdit.markAsPristine();
    });

    this.ttnIntegrationConfigForm.get('clientConfiguration.credentials').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(name => {
      this.updateDownlinkPattern(name.username);
    });

    this.ttnIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.ttnIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: TtnIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.ttnIntegrationConfigForm.reset(value, {emitEvent: false});
      if (isDefinedAndNotNull(value.clientConfiguration)) {
        if (value.clientConfiguration.customHost === !!ThingsStartHostType.Region &&
          value.clientConfiguration.host.endsWith(this.hostRegionSuffix)) {
          this.hostEdit.patchValue(value.clientConfiguration.host.slice(0, -this.hostRegionSuffix.length), {emitEvent: false});
        } else {
          this.hostEdit.patchValue(value.clientConfiguration.host, {emitEvent: false});
        }
      }
      let apiVersion = false;
      if (value.downlinkTopicPattern?.startsWith('v3')) {
        apiVersion = true;
      }
      this.apiVersion.patchValue(apiVersion, {emitEvent: false});
      this.downlinkPattern = ttnVersionMap.get(Number(apiVersion)).downlinkPattern;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ttnIntegrationConfigForm.disable({emitEvent: false});
      this.hostEdit.disable({emitEvent: false});
      this.apiVersion.disable({emitEvent: false});
    } else {
      this.ttnIntegrationConfigForm.enable({emitEvent: false});
      this.hostEdit.enable({emitEvent: false});
      this.apiVersion.enable({emitEvent: false});
      this.ttnIntegrationConfigForm.get('topicFilters').disable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.ttnIntegrationConfigForm.valid ? null : {
      ttnIntegrationConfigForm: {valid: false}
    };
  }

  private buildHostName(host: string): string {
    if (this.ttnIntegrationConfigForm.get('clientConfiguration.customHost').value === !!ThingsStartHostType.Region) {
      if (host.length) {
        return host + this.hostRegionSuffix;
      }
      return '';
    }
    return host;
  }

  private updateTtnVersionState(value: ttnVersion) {
    this.downlinkPattern = ttnVersionMap.get(value).downlinkPattern;
    this.ttnIntegrationConfigForm.get('topicFilters').patchValue(ttnVersionMap.get(value).uplinkTopic, {emitEvent: false});

    this.updateDownlinkPattern(this.ttnIntegrationConfigForm.get('clientConfiguration.credentials').value.username, true);
  }

  private updateDownlinkPattern(name: string, emitEvent = false) {
    let pattern = this.downlinkPattern;
    if (name.length) {
      pattern = this.downlinkPattern.replace('${applicationId}', name);
    }
    this.ttnIntegrationConfigForm.get('downlinkTopicPattern').patchValue(pattern, {emitEvent});
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.ttnIntegrationConfigForm.get('clientConfiguration.host').removeValidators(privateNetworkAddressValidator);
      this.hostEdit.removeValidators(privateNetworkAddressValidator);
    } else {
      this.ttnIntegrationConfigForm.get('clientConfiguration.host').addValidators(privateNetworkAddressValidator);
      this.hostEdit.addValidators(privateNetworkAddressValidator);
    }
    this.ttnIntegrationConfigForm.get('clientConfiguration.host').updateValueAndValidity({emitEvent: false});
    this.hostEdit.updateValueAndValidity({emitEvent: false});
  }

  getThingsStartHostTypeTranslation(type: number) {
    return this.ThingsStartHostTypeTranslation.get(type as ThingsStartHostType);
  }
}
