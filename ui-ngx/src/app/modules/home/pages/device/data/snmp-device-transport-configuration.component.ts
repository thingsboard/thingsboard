///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
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
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceTransportConfiguration,
  DeviceTransportType,
  SnmpAuthenticationProtocol,
  SnmpAuthenticationProtocolTranslationMap,
  SnmpDeviceProtocolVersion,
  SnmpDeviceTransportConfiguration,
  SnmpPrivacyProtocol,
  SnmpPrivacyProtocolTranslationMap
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-snmp-device-transport-configuration',
    templateUrl: './snmp-device-transport-configuration.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SnmpDeviceTransportConfigurationComponent),
            multi: true
        }, {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => SnmpDeviceTransportConfigurationComponent),
            multi: true
        }
    ],
    standalone: false
})
export class SnmpDeviceTransportConfigurationComponent implements ControlValueAccessor, OnInit, Validator {

  snmpDeviceTransportConfigurationFormGroup: UntypedFormGroup;

  snmpDeviceProtocolVersions = Object.values(SnmpDeviceProtocolVersion);
  snmpAuthenticationProtocols = Object.values(SnmpAuthenticationProtocol);
  snmpAuthenticationProtocolTranslation = SnmpAuthenticationProtocolTranslationMap;
  snmpPrivacyProtocols = Object.values(SnmpPrivacyProtocol);
  snmpPrivacyProtocolTranslation = SnmpPrivacyProtocolTranslationMap;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.snmpDeviceTransportConfigurationFormGroup = this.fb.group({
      host: ['', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      port: [null, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      protocolVersion: [SnmpDeviceProtocolVersion.V2C, Validators.required],
      community: ['public', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      username: ['', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      securityName: ['public', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      contextName: [null],
      authenticationProtocol: [SnmpAuthenticationProtocol.SHA_512, Validators.required],
      authenticationPassphrase: ['', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      privacyProtocol: [SnmpPrivacyProtocol.DES, Validators.required],
      privacyPassphrase: ['', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      engineId: ['']
    });
    this.snmpDeviceTransportConfigurationFormGroup.get('protocolVersion').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((protocol: SnmpDeviceProtocolVersion) => {
      this.updateDisabledFormValue(protocol);
    });
    this.snmpDeviceTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  validate(): ValidationErrors | null {
    return this.snmpDeviceTransportConfigurationFormGroup.valid ?  null : {
      snmpDeviceTransportConfiguration: false
    };
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.snmpDeviceTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.snmpDeviceTransportConfigurationFormGroup.enable({emitEvent: false});
      this.updateDisabledFormValue(
        this.snmpDeviceTransportConfigurationFormGroup.get('protocolVersion').value || SnmpDeviceProtocolVersion.V2C
      );
    }
  }

  writeValue(value: SnmpDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.snmpDeviceTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
      if (this.snmpDeviceTransportConfigurationFormGroup.enabled) {
        this.updateDisabledFormValue(value.protocolVersion || SnmpDeviceProtocolVersion.V2C);
      }
    }
  }

  isV3protocolVersion(): boolean {
    return this.snmpDeviceTransportConfigurationFormGroup.get('protocolVersion').value === SnmpDeviceProtocolVersion.V3;
  }

  private updateDisabledFormValue(protocol: SnmpDeviceProtocolVersion) {
    if (protocol === SnmpDeviceProtocolVersion.V3) {
      this.snmpDeviceTransportConfigurationFormGroup.get('community').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('username').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('securityName').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('contextName').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('authenticationProtocol').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('authenticationPassphrase').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('privacyProtocol').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('privacyPassphrase').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('engineId').enable({emitEvent: false});
    } else {
      this.snmpDeviceTransportConfigurationFormGroup.get('community').enable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('username').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('securityName').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('contextName').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('authenticationProtocol').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('authenticationPassphrase').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('privacyProtocol').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('privacyPassphrase').disable({emitEvent: false});
      this.snmpDeviceTransportConfigurationFormGroup.get('engineId').disable({emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceTransportConfiguration = null;
    if (this.snmpDeviceTransportConfigurationFormGroup.valid) {
      configuration = this.snmpDeviceTransportConfigurationFormGroup.value;
      configuration.type = DeviceTransportType.SNMP;
    }
    this.propagateChange(configuration);
  }
}
