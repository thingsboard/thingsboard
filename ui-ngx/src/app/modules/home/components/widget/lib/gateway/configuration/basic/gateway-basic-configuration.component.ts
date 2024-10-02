///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  Output
} from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { MatDialog } from '@angular/material/dialog';
import {
  GatewayRemoteConfigurationDialogComponent,
  GatewayRemoteConfigurationDialogData
} from '@home/components/widget/lib/gateway/gateway-remote-configuration-dialog';
import { DeviceService } from '@core/http/device.service';
import { Subject } from 'rxjs';
import { take, takeUntil } from 'rxjs/operators';
import { DeviceCredentials, DeviceCredentialsType } from '@shared/models/device.models';
import {
  GatewayLogLevel,
  GecurityTypesTranslationsMap,
  LocalLogsConfigTranslateMap,
  LocalLogsConfigs,
  LogSavingPeriod,
  LogSavingPeriodTranslations,
  SecurityTypes,
  StorageTypes,
  StorageTypesTranslationMap,
} from '../../gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  GatewayConfigCommand,
  GatewayConfigSecurity,
  GatewayConfigValue,
  LogConfig
} from '@home/components/widget/lib/gateway/configuration/models/gateway-configuration.models';

@Component({
  selector: 'tb-gateway-basic-configuration',
  templateUrl: './gateway-basic-configuration.component.html',
  styleUrls: ['./gateway-basic-configuration.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GatewayBasicConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => GatewayBasicConfigurationComponent),
      multi: true
    }
  ],
})
export class GatewayBasicConfigurationComponent implements OnDestroy, ControlValueAccessor, Validators {

  @Input()
  device: EntityId;

  @coerceBoolean()
  @Input()
  dialogMode = false;

  @Output()
  initialCredentialsUpdated = new EventEmitter<DeviceCredentials>();

  StorageTypes = StorageTypes;
  storageTypes = Object.values(StorageTypes);
  storageTypesTranslationMap = StorageTypesTranslationMap;
  logSavingPeriods = LogSavingPeriodTranslations;
  localLogsConfigs = Object.keys(LocalLogsConfigs) as LocalLogsConfigs[];
  localLogsConfigTranslateMap = LocalLogsConfigTranslateMap;
  securityTypes = GecurityTypesTranslationsMap;
  gatewayLogLevel = Object.values(GatewayLogLevel);

  logSelector: FormControl;
  basicFormGroup: FormGroup;

  private onChange: (value: GatewayConfigValue) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private deviceService: DeviceService,
              private cd: ChangeDetectorRef,
              private dialog: MatDialog) {
    this.initBasicFormGroup();
    this.observeFormChanges();
    this.basicFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(value);
        this.onTouched();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: GatewayConfigValue) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(basicConfig: GatewayConfigValue): void {
    this.basicFormGroup.patchValue(basicConfig, {emitEvent: false});
    this.checkAndFetchCredentials(basicConfig?.thingsboard?.security ?? {} as GatewayConfigSecurity);
    if (basicConfig?.grpc) {
      this.toggleRpcFields(basicConfig.grpc.enabled);
    }
    const commands = basicConfig?.thingsboard?.statistics?.commands ?? [];
    commands.forEach((command: GatewayConfigCommand) => this.addCommand(command, false));
  }

  validate(): ValidationErrors | null {
    return this.basicFormGroup.valid ? null : {
      basicFormGroup: {valid: false}
    };
  }

  private atLeastOneRequired(validator: ValidatorFn, controls: string[] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!controls) {
        controls = Object.keys(group.controls);
      }
      const hasAtLeastOne = group?.controls && controls.some(k => !validator(group.controls[k]));

      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  private toggleRpcFields(enable: boolean): void {
    const grpcGroup = this.basicFormGroup.get('grpc') as FormGroup;
    if (enable) {
      grpcGroup.get('serverPort').enable({emitEvent: false});
      grpcGroup.get('keepAliveTimeMs').enable({emitEvent: false});
      grpcGroup.get('keepAliveTimeoutMs').enable({emitEvent: false});
      grpcGroup.get('keepalivePermitWithoutCalls').enable({emitEvent: false});
      grpcGroup.get('maxPingsWithoutData').enable({emitEvent: false});
      grpcGroup.get('minTimeBetweenPingsMs').enable({emitEvent: false});
      grpcGroup.get('minPingIntervalWithoutDataMs').enable({emitEvent: false});
    } else {
      grpcGroup.get('serverPort').disable({emitEvent: false});
      grpcGroup.get('keepAliveTimeMs').disable({emitEvent: false});
      grpcGroup.get('keepAliveTimeoutMs').disable({emitEvent: false});
      grpcGroup.get('keepalivePermitWithoutCalls').disable({emitEvent: false});
      grpcGroup.get('maxPingsWithoutData').disable({emitEvent: false});
      grpcGroup.get('minTimeBetweenPingsMs').disable({emitEvent: false});
      grpcGroup.get('minPingIntervalWithoutDataMs').disable({emitEvent: false});
    }
  }

  private addLocalLogConfig(name: string, config: LogConfig): void {
    const localLogsFormGroup = this.basicFormGroup.get('logs.local') as FormGroup;
    const configGroup = this.fb.group({
      logLevel: [config.logLevel || GatewayLogLevel.INFO, [Validators.required]],
      filePath: [config.filePath || './logs', [Validators.required]],
      backupCount: [config.backupCount || 7, [Validators.required, Validators.min(0)]],
      savingTime: [config.savingTime || 3, [Validators.required, Validators.min(0)]],
      savingPeriod: [config.savingPeriod || LogSavingPeriod.days, [Validators.required]]
    });
    localLogsFormGroup.addControl(name, configGroup);
  }

  getLogFormGroup(value: string): FormGroup {
    return this.basicFormGroup.get(`logs.local.${value}`) as FormGroup;
  }

  commandFormArray(): FormArray {
    return this.basicFormGroup.get('thingsboard.statistics.commands') as FormArray;
  }

  removeCommandControl(index: number, event: PointerEvent): void {
    if (event.pointerType === '') {
      return;
    }
    this.commandFormArray().removeAt(index);
    this.basicFormGroup.markAsDirty();
  }

  private removeAllSecurityValidators(): void {
    const securityGroup = this.basicFormGroup.get('thingsboard.security') as FormGroup;
    securityGroup.clearValidators();
    for (const controlsKey in securityGroup.controls) {
      if (controlsKey !== 'type') {
        securityGroup.controls[controlsKey].clearValidators();
        securityGroup.controls[controlsKey].setErrors(null);
        securityGroup.controls[controlsKey].updateValueAndValidity();
      }
    }
  }

  private removeAllStorageValidators(): void {
    const storageGroup = this.basicFormGroup.get('storage') as FormGroup;
    for (const storageKey in storageGroup.controls) {
      if (storageKey !== 'type') {
        storageGroup.controls[storageKey].clearValidators();
        storageGroup.controls[storageKey].setErrors(null);
        storageGroup.controls[storageKey].updateValueAndValidity();
      }
    }
  }


  private openConfigurationConfirmDialog(): void {
    this.deviceService.getDevice(this.device.id).pipe(takeUntil(this.destroy$)).subscribe(gateway => {
      this.dialog.open<GatewayRemoteConfigurationDialogComponent, GatewayRemoteConfigurationDialogData>
      (GatewayRemoteConfigurationDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          gatewayName: gateway.name
        }
      }).afterClosed().pipe(take(1)).subscribe(
        (res) => {
          if (!res) {
            this.basicFormGroup.get('thingsboard.remoteConfiguration').setValue(true, {emitEvent: false});
          }
        }
      );
    });
  }

  addCommand(command?: GatewayConfigCommand, emitEvent: boolean = true): void {
    const { attributeOnGateway = null, command: cmd = null, timeout = null } = command || {};

    const commandFormGroup = this.fb.group({
      attributeOnGateway: [attributeOnGateway, [Validators.required, Validators.pattern(/^[^.\s]+$/)]],
      command: [cmd, [Validators.required, Validators.pattern(/^(?=\S).*\S$/)]],
      timeout: [timeout, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.pattern(/^[^.\s]+$/)]]
    });

    this.commandFormArray().push(commandFormGroup, { emitEvent });
  }

  private initBasicFormGroup(): void {
    this.basicFormGroup = this.fb.group({
      thingsboard: this.initThingsboardFormGroup(),
      storage: this.initStorageFormGroup(),
      grpc: this.initGrpcFormGroup(),
      connectors: this.fb.array([]),
      logs: this.initLogsFormGroup(),
    });
  }

  private initThingsboardFormGroup(): FormGroup {
    return this.fb.group({
      host: [window.location.hostname, [Validators.required, Validators.pattern(/^[^\s]+$/)]],
      port: [1883, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(/^-?[0-9]+$/)]],
      remoteShell: [false],
      remoteConfiguration: [true],
      checkConnectorsConfigurationInSeconds: [60, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      statistics: this.fb.group({
        enable: [true],
        statsSendPeriodInSeconds: [3600, [Validators.required, Validators.min(60), Validators.pattern(/^-?[0-9]+$/)]],
        commands: this.fb.array([])
      }),
      maxPayloadSizeBytes: [8196, [Validators.required, Validators.min(100), Validators.pattern(/^-?[0-9]+$/)]],
      minPackSendDelayMS: [50, [Validators.required, Validators.min(10), Validators.pattern(/^-?[0-9]+$/)]],
      minPackSizeToSend: [500, [Validators.required, Validators.min(100), Validators.pattern(/^-?[0-9]+$/)]],
      handleDeviceRenaming: [true],
      checkingDeviceActivity: this.initCheckingDeviceActivityFormGroup(),
      security: this.initSecurityFormGroup(),
      qos: [1, [Validators.required, Validators.min(0), Validators.max(1), Validators.pattern(/^[^.\s]+$/)]]
    });
  }

  private initStorageFormGroup(): FormGroup {
    return this.fb.group({
      type: [StorageTypes.MEMORY, [Validators.required]],
      read_records_count: [100, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      max_records_count: [100000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      data_folder_path: ['./data/', [Validators.required]],
      max_file_count: [10, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      max_read_records_count: [10, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      max_records_per_file: [10000, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      data_file_path: ['./data/data.db', [Validators.required]],
      messages_ttl_check_in_hours: [1, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      messages_ttl_in_days: [7, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]]
    });
  }

  private initGrpcFormGroup(): FormGroup {
    return this.fb.group({
      enabled: [false],
      serverPort: [9595, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(/^-?[0-9]+$/)]],
      keepAliveTimeMs: [10000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      keepAliveTimeoutMs: [5000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      keepalivePermitWithoutCalls: [true],
      maxPingsWithoutData: [0, [Validators.required, Validators.min(0), Validators.pattern(/^-?[0-9]+$/)]],
      minTimeBetweenPingsMs: [10000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      minPingIntervalWithoutDataMs: [5000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]]
    });
  }

  private initLogsFormGroup(): FormGroup {
    return this.fb.group({
      dateFormat: ['%Y-%m-%d %H:%M:%S', [Validators.required, Validators.pattern(/^[^\s].*[^\s]$/)]],
      logFormat: [
        '%(asctime)s - |%(levelname)s| - [%(filename)s] - %(module)s - %(funcName)s - %(lineno)d - %(message)s',
        [Validators.required, Validators.pattern(/^[^\s].*[^\s]$/)]
      ],
      type: ['remote', [Validators.required]],
      remote: this.fb.group({
        enabled: [false],
        logLevel: [GatewayLogLevel.INFO, [Validators.required]]
      }),
      local: this.fb.group({})
    });
  }

  private initCheckingDeviceActivityFormGroup(): FormGroup {
    return this.fb.group({
      checkDeviceInactivity: [false],
      inactivityTimeoutSeconds: [200, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      inactivityCheckPeriodSeconds: [500, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]]
    });
  }

  private initSecurityFormGroup(): FormGroup {
    return this.fb.group({
      type: [SecurityTypes.ACCESS_TOKEN, [Validators.required]],
      accessToken: [null, [Validators.required, Validators.pattern(/^[^.\s]+$/)]],
      clientId: [null, [Validators.pattern(/^[^.\s]+$/)]],
      username: [null, [Validators.pattern(/^[^.\s]+$/)]],
      password: [null, [Validators.pattern(/^[^.\s]+$/)]],
      caCert: [null],
      cert: [null],
      privateKey: [null]
    });
  }

  private observeFormChanges(): void {
    this.observeSecurityPasswordChanges();
    this.observeRemoteConfigurationChanges();
    this.observeDeviceActivityChanges();
    this.observeSecurityTypeChanges();
    this.observeStorageTypeChanges();
  }

  private observeSecurityPasswordChanges(): void {
    const securityUsername = this.basicFormGroup.get('thingsboard.security.username');
    this.basicFormGroup.get('thingsboard.security.password').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(password => {
      if (password && password !== '') {
        securityUsername.setValidators([Validators.required]);
      } else {
        securityUsername.clearValidators();
      }
      securityUsername.updateValueAndValidity({ emitEvent: false });
    });
  }

  private observeRemoteConfigurationChanges(): void {
    this.basicFormGroup.get('thingsboard.remoteConfiguration').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(enabled => {
      if (!enabled) {
        this.openConfigurationConfirmDialog();
      }
    });

    this.logSelector = this.fb.control(LocalLogsConfigs.service);
    for (const key of Object.keys(LocalLogsConfigs)) {
      this.addLocalLogConfig(key, {} as LogConfig);
    }
  }

  private observeDeviceActivityChanges(): void {
    const checkingDeviceActivityGroup = this.basicFormGroup.get('thingsboard.checkingDeviceActivity') as FormGroup;
    checkingDeviceActivityGroup.get('checkDeviceInactivity').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(enabled => {
      checkingDeviceActivityGroup.updateValueAndValidity();
      const validators = [Validators.min(1), Validators.required, Validators.pattern(/^-?[0-9]+$/)];

      if (enabled) {
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').setValidators(validators);
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').setValidators(validators);
      } else {
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').clearValidators();
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').clearValidators();
      }
      checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').updateValueAndValidity({ emitEvent: false });
      checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').updateValueAndValidity({ emitEvent: false });
    });

    this.basicFormGroup.get('grpc.enabled').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(value => {
      this.toggleRpcFields(value);
    });
  }

  private observeSecurityTypeChanges(): void {
    const securityGroup = this.basicFormGroup.get('thingsboard.security') as FormGroup;

    securityGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(type => {
      this.removeAllSecurityValidators();

      switch (type) {
        case SecurityTypes.ACCESS_TOKEN:
          this.addAccessTokenValidators(securityGroup);
          break;
        case SecurityTypes.TLS_PRIVATE_KEY:
          this.addTlsPrivateKeyValidators(securityGroup);
          break;
        case SecurityTypes.TLS_ACCESS_TOKEN:
          this.addTlsAccessTokenValidators(securityGroup);
          break;
        case SecurityTypes.USERNAME_PASSWORD:
          securityGroup.addValidators([this.atLeastOneRequired(Validators.required, ['clientId', 'username'])]);
          break;
      }

      securityGroup.updateValueAndValidity();
    });

    ['caCert', 'privateKey', 'cert'].forEach(field => {
      securityGroup.get(field).valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.cd.detectChanges());
    });
  }

  private observeStorageTypeChanges(): void {
    const storageGroup = this.basicFormGroup.get('storage') as FormGroup;

    storageGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(type => {
      this.removeAllStorageValidators();

      switch (type) {
        case StorageTypes.MEMORY:
          this.addMemoryStorageValidators(storageGroup);
          break;
        case StorageTypes.FILE:
          this.addFileStorageValidators(storageGroup);
          break;
        case StorageTypes.SQLITE:
          this.addSqliteStorageValidators(storageGroup);
          break;
      }
    });
  }

  private addAccessTokenValidators(group: FormGroup): void {
    group.get('accessToken').addValidators([Validators.required, Validators.pattern(/^[^.\s]+$/)]);
    group.get('accessToken').updateValueAndValidity();
  }

  private addTlsPrivateKeyValidators(group: FormGroup): void {
    ['caCert', 'privateKey', 'cert'].forEach(field => {
      group.get(field).addValidators([Validators.required]);
      group.get(field).updateValueAndValidity();
    });
  }

  private addTlsAccessTokenValidators(group: FormGroup): void {
    this.addAccessTokenValidators(group);
    group.get('caCert').addValidators([Validators.required]);
    group.get('caCert').updateValueAndValidity();
  }

  private addMemoryStorageValidators(group: FormGroup): void {
    group.get('read_records_count').addValidators([Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]);
    group.get('max_records_count').addValidators([Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]);
    group.get('read_records_count').updateValueAndValidity({ emitEvent: false });
    group.get('max_records_count').updateValueAndValidity({ emitEvent: false });
  }

  private addFileStorageValidators(group: FormGroup): void {
    ['max_file_count', 'max_read_records_count', 'max_records_per_file'].forEach(field => {
      group.get(field).addValidators([Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]);
      group.get(field).updateValueAndValidity({ emitEvent: false });
    });
  }

  private addSqliteStorageValidators(group: FormGroup): void {
    ['messages_ttl_check_in_hours', 'messages_ttl_in_days'].forEach(field => {
      group.get(field).addValidators([Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]);
      group.get(field).updateValueAndValidity({ emitEvent: false });
    });
  }

  private checkAndFetchCredentials(security: GatewayConfigSecurity): void {
    if (security.type === SecurityTypes.TLS_PRIVATE_KEY) {
      return;
    }

    this.deviceService.getDeviceCredentials(this.device.id).pipe(takeUntil(this.destroy$)).subscribe(credentials => {
      this.initialCredentialsUpdated.emit(credentials);
      this.updateSecurityType(security, credentials);
      this.updateCredentials(credentials, security);
    });
  }

  private updateSecurityType(security, credentials: DeviceCredentials): void {
    const isAccessToken = credentials.credentialsType === DeviceCredentialsType.ACCESS_TOKEN
      || security.type === SecurityTypes.TLS_ACCESS_TOKEN;
    const securityType = isAccessToken
      ? (security.type === SecurityTypes.TLS_ACCESS_TOKEN ? SecurityTypes.TLS_ACCESS_TOKEN : SecurityTypes.ACCESS_TOKEN)
      : (credentials.credentialsType === DeviceCredentialsType.MQTT_BASIC ? SecurityTypes.USERNAME_PASSWORD : null);

    if (securityType) {
      this.basicFormGroup.get('thingsboard.security.type').setValue(securityType, { emitEvent: false });
    }
  }

  private updateCredentials(credentials: DeviceCredentials, security: GatewayConfigSecurity): void {
    switch (credentials.credentialsType) {
      case DeviceCredentialsType.ACCESS_TOKEN:
        this.updateAccessTokenCredentials(credentials, security);
        break;
      case DeviceCredentialsType.MQTT_BASIC:
        this.updateMqttBasicCredentials(credentials);
        break;
      case DeviceCredentialsType.X509_CERTIFICATE:
        break;
    }
  }

  private updateAccessTokenCredentials(credentials: DeviceCredentials, security: GatewayConfigSecurity): void {
    this.basicFormGroup.get('thingsboard.security.accessToken').setValue(credentials.credentialsId, { emitEvent: false });
    if (security.type === SecurityTypes.TLS_ACCESS_TOKEN) {
      this.basicFormGroup.get('thingsboard.security.caCert').setValue(security.caCert, { emitEvent: false });
    }
  }

  private updateMqttBasicCredentials(credentials: DeviceCredentials): void {
    const parsedValue = JSON.parse(credentials.credentialsValue);
    this.basicFormGroup.get('thingsboard.security.clientId').setValue(parsedValue.clientId, { emitEvent: false });
    this.basicFormGroup.get('thingsboard.security.username').setValue(parsedValue.userName, { emitEvent: false });
    this.basicFormGroup.get('thingsboard.security.password').setValue(parsedValue.password, { emitEvent: false });
  }
}
