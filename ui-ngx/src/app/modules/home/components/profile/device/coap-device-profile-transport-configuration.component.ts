// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import {
  CoapDeviceProfileTransportConfiguration,
  coapDeviceTypeTranslationMap,
  CoapTransportDeviceType,
  defaultAttributesSchema,
  defaultRpcRequestSchema,
  defaultRpcResponseSchema,
  defaultTelemetrySchema,
  DeviceTransportType,
  TransportPayloadType,
  transportPayloadTypeTranslationMap,
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PowerMode } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';

@Component({
    selector: 'tb-coap-device-profile-transport-configuration',
    templateUrl: './coap-device-profile-transport-configuration.component.html',
    styleUrls: ['./coap-device-profile-transport-configuration.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CoapDeviceProfileTransportConfigurationComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CoapDeviceProfileTransportConfigurationComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CoapDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  coapTransportDeviceTypes = Object.values(CoapTransportDeviceType);
  coapTransportDeviceTypeTranslations = coapDeviceTypeTranslationMap;

  transportPayloadTypes = Object.values(TransportPayloadType);
  transportPayloadTypeTranslations = transportPayloadTypeTranslationMap;

  coapTransportConfigurationFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  private transportPayloadTypeConfiguration = this.fb.group({
    transportPayloadType: [TransportPayloadType.JSON, Validators.required],
    deviceTelemetryProtoSchema: [defaultTelemetrySchema, Validators.required],
    deviceAttributesProtoSchema: [defaultAttributesSchema, Validators.required],
    deviceRpcRequestProtoSchema: [defaultRpcRequestSchema, Validators.required],
    deviceRpcResponseProtoSchema: [defaultRpcResponseSchema, Validators.required]
  });

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.coapTransportConfigurationFormGroup = this.fb.group({
      coapDeviceTypeConfiguration: this.fb.group({
        coapDeviceType: [CoapTransportDeviceType.DEFAULT, Validators.required],
        transportPayloadTypeConfiguration: this.transportPayloadTypeConfiguration,
      }),
      clientSettings: this.fb.group({
        powerMode: [PowerMode.DRX, Validators.required],
        edrxCycle: [{disabled: true, value: 0}, Validators.required],
        psmActivityTimer: [{disabled: true, value: 0}, Validators.required],
        pagingTransmissionWindow: [{disabled: true, value: 0}, Validators.required]
      })}
    );
    this.coapTransportConfigurationFormGroup.get('coapDeviceTypeConfiguration.coapDeviceType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(coapDeviceType => {
      this.updateCoapDeviceTypeBasedControls(coapDeviceType, true);
    });
    this.coapTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get coapDeviceTypeDefault(): boolean {
    const coapDeviceType = this.coapTransportConfigurationFormGroup.get('coapDeviceTypeConfiguration.coapDeviceType').value;
    return coapDeviceType === CoapTransportDeviceType.DEFAULT;
  }

  get protoPayloadType(): boolean {
    const transportPayloadTypePath = 'coapDeviceTypeConfiguration.transportPayloadTypeConfiguration.transportPayloadType';
    const transportPayloadType = this.coapTransportConfigurationFormGroup.get(transportPayloadTypePath).value;
    return transportPayloadType === TransportPayloadType.PROTOBUF;
  }

  get clientSettingsFormGroup(): UntypedFormGroup {
    return this.coapTransportConfigurationFormGroup.get('clientSettings') as UntypedFormGroup;
  }

  private updateCoapDeviceTypeBasedControls(type: CoapTransportDeviceType, forceUpdated = false) {
    const coapDeviceTypeConfigurationFormGroup = this.coapTransportConfigurationFormGroup
      .get('coapDeviceTypeConfiguration') as UntypedFormGroup;
    if (forceUpdated) {
      coapDeviceTypeConfigurationFormGroup.patchValue({
        transportPayloadTypeConfiguration: this.transportPayloadTypeConfiguration
      }, {emitEvent: false});
    }
    if (type === CoapTransportDeviceType.DEFAULT && !this.disabled) {
      coapDeviceTypeConfigurationFormGroup.get('transportPayloadTypeConfiguration').enable({emitEvent: false});
    } else {
      coapDeviceTypeConfigurationFormGroup.get('transportPayloadTypeConfiguration').disable({emitEvent: false});
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.coapTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.coapTransportConfigurationFormGroup.enable({emitEvent: false});
      this.coapTransportConfigurationFormGroup.get('clientSettings.powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(value: CoapDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      if (!value.clientSettings) {
        value.clientSettings = {
          powerMode: PowerMode.DRX
        };
      }
      this.coapTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
      if (!this.disabled) {
        this.coapTransportConfigurationFormGroup.get('clientSettings.powerMode').updateValueAndValidity({onlySelf: true});
      }
      this.updateCoapDeviceTypeBasedControls(value.coapDeviceTypeConfiguration?.coapDeviceType);
    }
  }

  public validate(c: UntypedFormControl): ValidationErrors | null {
    return (this.coapTransportConfigurationFormGroup.valid) ? null : {
      valid: false,
    };
  }

  private updateModel() {
    const configuration = this.coapTransportConfigurationFormGroup.value;
    configuration.type = DeviceTransportType.COAP;
    this.propagateChange(configuration);
  }
}
