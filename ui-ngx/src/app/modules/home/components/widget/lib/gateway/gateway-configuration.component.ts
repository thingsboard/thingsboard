///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  GatewayRemoteConfigurationDialogComponent,
  GatewayRemoteConfigurationDialogData
} from '@home/components/widget/lib/gateway/gateway-remote-configuration-dialog';
import { DeviceService } from '@core/http/device.service';
import { Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { DeviceCredentials, DeviceCredentialsType } from '@shared/models/device.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import {
  GatewayLogLevel,
  GecurityTypesTranslationsMap,
  LocalLogsConfigTranslateMap,
  LocalLogsConfigs,
  LogSavingPeriod,
  LogSavingPeriodTranslations,
  SecurityTypes,
  StorageTypes,
  StorageTypesTranslationMap
} from './gateway-widget.models';
import { deepTrim } from '@core/utils';

@Component({
  selector: 'tb-gateway-configuration',
  templateUrl: './gateway-configuration.component.html',
  styleUrls: ['./gateway-configuration.component.scss']
})
export class GatewayConfigurationComponent implements OnInit {

  gatewayConfigGroup: FormGroup;

  StorageTypes = StorageTypes;
  storageTypes = Object.values(StorageTypes) as StorageTypes[];
  storageTypesTranslationMap = StorageTypesTranslationMap;

  logSavingPeriods = LogSavingPeriodTranslations;

  localLogsConfigs = Object.keys(LocalLogsConfigs) as LocalLogsConfigs[];
  localLogsConfigTranslateMap = LocalLogsConfigTranslateMap;

  securityTypes = GecurityTypesTranslationsMap;

  gatewayLogLevel = Object.values(GatewayLogLevel);

  @Input()
  device: EntityId;

  @Input()
  dialogRef: MatDialogRef<any>;

  logSelector: FormControl;

  private initialCredentials: DeviceCredentials;

  constructor(private fb: FormBuilder,
              private attributeService: AttributeService,
              private deviceService: DeviceService,
              private cd: ChangeDetectorRef,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.gatewayConfigGroup = this.fb.group({
      thingsboard: this.fb.group({
        host: [window.location.hostname, [Validators.required, Validators.pattern(/^[^\s]+$/)]],
        port: [1883, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(/^-?[0-9]+$/)]],
        remoteShell: [false, []],
        remoteConfiguration: [true, []],
        checkConnectorsConfigurationInSeconds: [60, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        statistics: this.fb.group({
          enable: [true, []],
          statsSendPeriodInSeconds: [3600, [Validators.required, Validators.min(60), Validators.pattern(/^-?[0-9]+$/)]],
          commands: this.fb.array([], [])
        }),
        maxPayloadSizeBytes: [1024, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        minPackSendDelayMS: [200, [Validators.required, Validators.min(0), Validators.pattern(/^-?[0-9]+$/)]],
        minPackSizeToSend: [500, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        handleDeviceRenaming: [true, []],
        checkingDeviceActivity: this.fb.group({
          checkDeviceInactivity: [false, []],
          inactivityTimeoutSeconds: [200, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
          inactivityCheckPeriodSeconds: [500, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]]
        }),
        security: this.fb.group({
          type: [SecurityTypes.ACCESS_TOKEN, [Validators.required]],
          accessToken: [null, [Validators.required, Validators.pattern(/^[^.\s]+$/)]],
          clientId: [null, [Validators.pattern(/^[^.\s]+$/)]],
          username: [null, [Validators.pattern(/^[^.\s]+$/)]],
          password: [null, [Validators.pattern(/^[^.\s]+$/)]],
          caCert: [null, []],
          cert: [null, []],
          privateKey: [null, []],
        }),
        qos: [1, [Validators.min(0), Validators.max(1), Validators.required, Validators.pattern(/^[^.\s]+$/)]]
      }),
      storage: this.fb.group({
        type: [StorageTypes.MEMORY, [Validators.required]],
        read_records_count: [100, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required, Validators.pattern(/^[^.\s]+$/)]],
        max_records_count: [100000, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required, Validators.pattern(/^[^.\s]+$/)]],
        data_folder_path: ['./data/', [Validators.pattern(/^[^\s]+$/)]],
        max_file_count: [10, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        max_read_records_count: [10, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        max_records_per_file: [10000, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        data_file_path: ['./data/data.db', [Validators.pattern(/^[^\s]+$/)]],
        messages_ttl_check_in_hours: [1, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        messages_ttl_in_days: [7, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],

      }),
      grpc: this.fb.group({
        enabled: [false, []],
        serverPort: [9595, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(/^-?[0-9]+$/)]],
        keepAliveTimeMs: [10000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        keepAliveTimeoutMs: [5000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        keepalivePermitWithoutCalls: [true, []],
        maxPingsWithoutData: [0, [Validators.required, Validators.min(0), Validators.pattern(/^-?[0-9]+$/)]],
        minTimeBetweenPingsMs: [10000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        minPingIntervalWithoutDataMs: [5000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      }),
      connectors: this.fb.array([]),
      logs: this.fb.group({
        dateFormat: ['%Y-%m-%d %H:%M:%S', [Validators.required, Validators.pattern(/^[^\s].*[^\s]$/)]],
        logFormat: ['%(asctime)s - |%(levelname)s| - [%(filename)s] - %(module)s - %(funcName)s - %(lineno)d - %(message)s',
          [Validators.required, Validators.pattern(/^[^\s].*[^\s]$/)]],
        type: ['remote', [Validators.required]],
        remote: this.fb.group({
          enabled: [false],
          logLevel: [GatewayLogLevel.INFO, [Validators.required]],
        }),
        local: this.fb.group({})
      })
    });

    this.gatewayConfigGroup.get('thingsboard.security.password').valueChanges.subscribe(password => {
      if (password && password !== '') {
        this.gatewayConfigGroup.get('thingsboard.security.username').setValidators([Validators.required]);
      } else {
        this.gatewayConfigGroup.get('thingsboard.security.username').clearValidators();
      }
      this.gatewayConfigGroup.get('thingsboard.security.username').updateValueAndValidity({emitEvent: false});
    });

    this.toggleRpcFields(false);

    this.gatewayConfigGroup.get('thingsboard.remoteConfiguration').valueChanges.subscribe(enabled => {
      if (!enabled) {
        this.openConfigurationConfirmDialog();
      }
    });

    this.logSelector = this.fb.control(LocalLogsConfigs.service);

    for (const localLogsConfigsKey of Object.keys(LocalLogsConfigs)) {
      this.addLocalLogConfig(localLogsConfigsKey, {});
    }

    const checkingDeviceActivityGroup = this.gatewayConfigGroup.get('thingsboard.checkingDeviceActivity') as FormGroup;
    checkingDeviceActivityGroup.get('checkDeviceInactivity').valueChanges.subscribe(enabled => {
      checkingDeviceActivityGroup.updateValueAndValidity();
      if (enabled) {
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').setValidators([Validators.min(1), Validators.required, Validators.pattern(/^-?[0-9]+$/)]);
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').setValidators([Validators.min(1), Validators.required, Validators.pattern(/^-?[0-9]+$/)]);
      } else {
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').clearValidators();
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').clearValidators();
      }
      checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').updateValueAndValidity({emitEvent: false});
      checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').updateValueAndValidity({emitEvent: false});
    });

    this.gatewayConfigGroup.get('grpc.enabled').valueChanges.subscribe(value => {
      this.toggleRpcFields(value);
    });

    const securityGroup = this.gatewayConfigGroup.get('thingsboard.security') as FormGroup;
    securityGroup.get('type').valueChanges.subscribe(type => {
      this.removeAllSecurityValidators();
      if (type === SecurityTypes.ACCESS_TOKEN) {
        securityGroup.get('accessToken').addValidators([Validators.required, Validators.pattern(/^[^.\s]+$/)]);
        securityGroup.get('accessToken').updateValueAndValidity();
      } else if (type === SecurityTypes.TLS_PRIVATE_KEY) {
        securityGroup.get('caCert').addValidators([Validators.required]);
        securityGroup.get('caCert').updateValueAndValidity();
        securityGroup.get('privateKey').addValidators([Validators.required]);
        securityGroup.get('privateKey').updateValueAndValidity();
        securityGroup.get('cert').addValidators([Validators.required]);
        securityGroup.get('cert').updateValueAndValidity();
      } else if (type === SecurityTypes.TLS_ACCESS_TOKEN) {
        securityGroup.get('accessToken').addValidators([Validators.required, Validators.pattern(/^[^.\s]+$/)]);
        securityGroup.get('accessToken').updateValueAndValidity();
        securityGroup.get('caCert').addValidators([Validators.required]);
        securityGroup.get('caCert').updateValueAndValidity();
      } else if (type === SecurityTypes.USERNAME_PASSWORD) {
        securityGroup.addValidators([this.atLeastOneRequired(Validators.required, ['clientId', 'username'])]);
      }
      securityGroup.updateValueAndValidity();
    });

    securityGroup.get('caCert').valueChanges.subscribe(() => this.cd.detectChanges());
    securityGroup.get('privateKey').valueChanges.subscribe(() => this.cd.detectChanges());
    securityGroup.get('cert').valueChanges.subscribe(() => this.cd.detectChanges());

    const storageGroup = this.gatewayConfigGroup.get('storage') as FormGroup;
    storageGroup.get('type').valueChanges.subscribe(type => {
      this.removeAllStorageValidators();
      if (type === StorageTypes.MEMORY) {
        storageGroup.get('read_records_count').addValidators(
          [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]);
        storageGroup.get('max_records_count').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('read_records_count').updateValueAndValidity({emitEvent: false});
        storageGroup.get('max_records_count').updateValueAndValidity({emitEvent: false});
      } else if (type === StorageTypes.FILE) {
        storageGroup.get('data_folder_path').addValidators([Validators.required, Validators.pattern(/^[^.\s]+$/)]);
        storageGroup.get('max_file_count').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('max_read_records_count').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('max_records_per_file').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('data_folder_path').updateValueAndValidity({emitEvent: false});
        storageGroup.get('max_file_count').updateValueAndValidity({emitEvent: false});
        storageGroup.get('max_read_records_count').updateValueAndValidity({emitEvent: false});
        storageGroup.get('max_records_per_file').updateValueAndValidity({emitEvent: false});
      } else if (type === StorageTypes.SQLITE) {
        storageGroup.get('data_file_path').addValidators([Validators.required, Validators.pattern(/^[^.\s]+$/)]);
        storageGroup.get('messages_ttl_check_in_hours').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('messages_ttl_in_days').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('data_file_path').updateValueAndValidity({emitEvent: false});
        storageGroup.get('messages_ttl_check_in_hours').updateValueAndValidity({emitEvent: false});
        storageGroup.get('messages_ttl_in_days').updateValueAndValidity({emitEvent: false});
      }
    });

    this.fetchConfigAttribute(this.device);
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

  private fetchConfigAttribute(entityId: EntityId) {
    if (entityId.id === NULL_UUID) {
      return;
    }
    this.attributeService.getEntityAttributes(entityId, AttributeScope.CLIENT_SCOPE,
      ['general_configuration', 'grpc_configuration', 'logs_configuration', 'storage_configuration', 'RemoteLoggingLevel']).pipe(
      mergeMap(attributes => attributes.length ? of(attributes) : this.attributeService.getEntityAttributes(
        entityId, AttributeScope.SHARED_SCOPE, ['general_configuration', 'grpc_configuration',
          'logs_configuration', 'storage_configuration', 'RemoteLoggingLevel']))
    ).subscribe(attributes => {
      if (attributes.length) {
        const general_configuration = attributes.find(attribute => attribute.key === 'general_configuration')?.value;
        const grpc_configuration = attributes.find(attribute => attribute.key === 'grpc_configuration')?.value;
        const logs_configuration = attributes.find(attribute => attribute.key === 'logs_configuration')?.value;
        const storage_configuration = attributes.find(attribute => attribute.key === 'storage_configuration')?.value;
        const remoteLoggingLevel = attributes.find(attribute => attribute.key === 'RemoteLoggingLevel')?.value;
        if (general_configuration) {
          const configObj = {thingsboard: general_configuration};
          if (configObj.thingsboard.statistics && configObj.thingsboard.statistics.commands) {
            for (const command of configObj.thingsboard.statistics.commands) {
              this.addCommand(command);
            }
            delete configObj.thingsboard.statistics.commands;
          }
          this.gatewayConfigGroup.patchValue(configObj, {emitEvent: false});
          this.gatewayConfigGroup.markAsPristine();
          if (!configObj.thingsboard.remoteConfiguration) {
            this.gatewayConfigGroup.disable({emitEvent: false});
          }
          this.checkAndFetchCredentials(configObj.thingsboard.security);
        }
        if (grpc_configuration) {
          const configObj = {grpc: grpc_configuration};
          this.gatewayConfigGroup.patchValue(configObj, {emitEvent: false});
          this.toggleRpcFields(grpc_configuration.enabled);
        }
        if (logs_configuration) {
          const configObj = {logs: this.logsToObj(logs_configuration)};
          this.gatewayConfigGroup.patchValue(configObj, {emitEvent: false});
          this.cd.detectChanges();
        }
        if (storage_configuration) {
          const configObj = {storage: storage_configuration};
          this.gatewayConfigGroup.patchValue(configObj, {emitEvent: false});
        }
        if (remoteLoggingLevel) {
          const remoteLogsFormGroup = this.gatewayConfigGroup.get('logs.remote');
          remoteLogsFormGroup.patchValue({
            enabled: remoteLoggingLevel !== GatewayLogLevel.NONE,
            logLevel: remoteLoggingLevel
          }, {emitEvent: false});
          remoteLogsFormGroup.markAsPristine();
        }
        this.cd.detectChanges();
      } else {
        this.checkAndFetchCredentials();
      }
    });
  }

  private checkAndFetchCredentials(security: any = {}): void {
    if (security.type !== SecurityTypes.TLS_PRIVATE_KEY) {
      this.deviceService.getDeviceCredentials(this.device.id).subscribe(credentials => {
        this.initialCredentials = credentials;
        if (credentials.credentialsType === DeviceCredentialsType.ACCESS_TOKEN || security.type === SecurityTypes.TLS_ACCESS_TOKEN) {
          this.gatewayConfigGroup.get('thingsboard.security.type').setValue(security.type === SecurityTypes.TLS_ACCESS_TOKEN
            ? SecurityTypes.TLS_ACCESS_TOKEN
            : SecurityTypes.ACCESS_TOKEN);
          this.gatewayConfigGroup.get('thingsboard.security.accessToken').setValue(credentials.credentialsId);
          if(security.type === SecurityTypes.TLS_ACCESS_TOKEN) {
            this.gatewayConfigGroup.get('thingsboard.security.caCert').setValue(security.caCert);
          }
        } else if (credentials.credentialsType === DeviceCredentialsType.MQTT_BASIC) {
          const parsedValue = JSON.parse(credentials.credentialsValue);
          this.gatewayConfigGroup.get('thingsboard.security.type').setValue(SecurityTypes.USERNAME_PASSWORD);
          this.gatewayConfigGroup.get('thingsboard.security.clientId').setValue(parsedValue.clientId);
          this.gatewayConfigGroup.get('thingsboard.security.username').setValue(parsedValue.userName);
          this.gatewayConfigGroup.get('thingsboard.security.password').setValue(parsedValue.password, {emitEvent: false});
        } else if (credentials.credentialsType === DeviceCredentialsType.X509_CERTIFICATE) {
          //if sertificate is present set sertificate as present
        }
      });
    }
  }

  private logsToObj(logsConfig: any) {
    const logsObject = {
      local: {}
    };
    const logFormat = logsConfig.formatters.LogFormatter.format;
    const dateFormat = logsConfig.formatters.LogFormatter.datefmt;
    for (const localLogsConfigsKey of Object.keys(LocalLogsConfigs)) {
      const handlerKey = localLogsConfigsKey + 'Handler';
      logsObject[localLogsConfigsKey] = {
        logLevel: logsConfig.loggers[localLogsConfigsKey].level,
        filePath: logsConfig.handlers[handlerKey].filename.split('/' + localLogsConfigsKey)[0],
        backupCount: logsConfig.handlers[handlerKey].backupCount,
        savingTime: logsConfig.handlers[handlerKey].interval,
        savingPeriod: logsConfig.handlers[handlerKey].when,
      };
    }


    return {local: logsObject, logFormat, dateFormat};
  }

  private toggleRpcFields(enable: boolean) {
    const grpcGroup = this.gatewayConfigGroup.get('grpc') as FormGroup;
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


  addCommand(command: any = {}): void {
    const commandsFormArray = this.commandFormArray();
    const commandFormGroup = this.fb.group({
      attributeOnGateway: [command.attributeOnGateway || null, [Validators.required, Validators.pattern(/^[^.\s]+$/)]],
      command: [command.command || null, [Validators.required, Validators.pattern(/^[^.\s]+$/)]],
      timeout: [command.timeout || null, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.pattern(/^[^.\s]+$/)]],
    });
    commandsFormArray.push(commandFormGroup);
  }

  private addLocalLogConfig(name: string, config: any): void {
    const localLogsFormGroup = this.gatewayConfigGroup.get('logs.local') as FormGroup;
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
    return this.gatewayConfigGroup.get(`logs.local.${value}`) as FormGroup;
  }

  commandFormArray(): FormArray {
    return this.gatewayConfigGroup.get('thingsboard.statistics.commands') as FormArray;
  }

  removeCommandControl(index: number, event: any): void {
    if (event.pointerType === '') {
      return;
    }
    this.commandFormArray().removeAt(index);
    this.gatewayConfigGroup.markAsDirty();
  }

  private removeAllSecurityValidators(): void {
    const securityGroup = this.gatewayConfigGroup.get('thingsboard.security') as FormGroup;
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
    const storageGroup = this.gatewayConfigGroup.get('storage') as FormGroup;
    for (const storageKey in storageGroup.controls) {
      if (storageKey !== 'type') {
        storageGroup.controls[storageKey].clearValidators();
        storageGroup.controls[storageKey].setErrors(null);
        storageGroup.controls[storageKey].updateValueAndValidity();
      }
    }
  }

  private removeEmpty(obj: any) {
    return Object.fromEntries(
      Object.entries(obj)
        .filter(([_, v]) => v != null)
        .map(([k, v]) => [k, v === Object(v) ? this.removeEmpty(v) : v])
    );
  }

  private generateLogsFile(logsObj: any) {
    const logAttrObj = {
      version: 1,
      disable_existing_loggers: false,
      formatters: {
        LogFormatter: {
          class: 'logging.Formatter',
          format: logsObj.logFormat,
          datefmt: logsObj.dateFormat,
        }
      },
      handlers: {
        consoleHandler: {
          class: 'logging.StreamHandler',
          formatter: 'LogFormatter',
          level: 'DEBUG',
          stream: 'ext://sys.stdout'
        },
        databaseHandler: {
          class: 'thingsboard_gateway.tb_utility.tb_handler.TimedRotatingFileHandler',
          formatter: 'LogFormatter',
          filename: './logs/database.log',
          backupCount: 1,
          encoding: 'utf-8'
        }
      },
      loggers: {
        database: {
          handlers: ['databaseHandler', 'consoleHandler'],
          level: 'DEBUG',
          propagate: false
        }
      },
      root: {
        level: 'ERROR',
        handlers: [
          'consoleHandler'
        ]
      },
      ts: new Date().getTime()
    };
    for (const key of Object.keys(logsObj.local)) {
      logAttrObj.handlers[key + 'Handler'] = this.createHandlerObj(logsObj.local[key], key);
      logAttrObj.loggers[key] = this.createLoggerObj(logsObj.local[key], key);
    }
    return logAttrObj;
  }

  private createHandlerObj(logObj: any, key: string) {
    return {
      class: 'thingsboard_gateway.tb_utility.tb_handler.TimedRotatingFileHandler',
      formatter: 'LogFormatter',
      filename: `${logObj.filePath}/${key}.log`,
      backupCount: logObj.backupCount,
      interval: logObj.savingTime,
      when: logObj.savingPeriod,
      encoding: 'utf-8'
    };
  }

  private createLoggerObj(logObj: any, key: string) {
    return {
      handlers: [`${key}Handler`, 'consoleHandler'],
      level: logObj.logLevel,
      propagate: false
    };
  }

  saveConfig(): void {
    const value = deepTrim(this.removeEmpty(this.gatewayConfigGroup.value));
    value.thingsboard.statistics.commands = Object.values(value.thingsboard.statistics.commands);
    const attributes = [];
    attributes.push({
      key: 'RemoteLoggingLevel',
      value: value.logs.remote.enabled ? value.logs.remote.logLevel : GatewayLogLevel.NONE
    });
    delete value.connectors;
    attributes.push({
      key: 'logs_configuration',
      value: this.generateLogsFile(value.logs)
    });
    value.grpc.ts = new Date().getTime();
    attributes.push({
      key: 'grpc_configuration',
      value: value.grpc
    });
    value.storage.ts = new Date().getTime();
    attributes.push({
      key: 'storage_configuration',
      value: value.storage
    });
    value.thingsboard.ts = new Date().getTime();
    attributes.push({
      key: 'general_configuration',
      value: value.thingsboard
    });


    this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, attributes).subscribe(_ => {
      this.updateCredentials(value.thingsboard.security).subscribe(() => {
        if (this.dialogRef) {
          this.dialogRef.close();
        } else {
          this.gatewayConfigGroup.markAsPristine();
          this.cd.detectChanges();
        }
      });
    });
  }

  private updateCredentials(securityConfig: any): Observable<any> {
    let updateCredentials = false;
    let newCredentials = {};
    if (securityConfig.type === SecurityTypes.USERNAME_PASSWORD) {
      if (this.initialCredentials.credentialsType !== DeviceCredentialsType.MQTT_BASIC) {
        updateCredentials = true;
      } else {
        const parsedCredentials = JSON.parse(this.initialCredentials.credentialsValue);
        updateCredentials = !(
          parsedCredentials.clientId === securityConfig.clientId &&
          parsedCredentials.userName === securityConfig.username &&
          parsedCredentials.password === securityConfig.password);
      }
      if (updateCredentials) {
        const credentialsValue: { clientId?: string; userName?: string; password?: string } = {};
        const credentialsType = DeviceCredentialsType.MQTT_BASIC;
        if (securityConfig.clientId) {
          credentialsValue.clientId = securityConfig.clientId;
        }
        if (securityConfig.username) {
          credentialsValue.userName = securityConfig.username;
        }
        if (securityConfig.password) {
          credentialsValue.password = securityConfig.password;
        }
        newCredentials = {
          credentialsType,
          credentialsValue: JSON.stringify(credentialsValue)
        };
      }
    } else if (securityConfig.type === SecurityTypes.ACCESS_TOKEN || securityConfig.type === SecurityTypes.TLS_ACCESS_TOKEN) {
      if (this.initialCredentials.credentialsType !== DeviceCredentialsType.ACCESS_TOKEN) {
        updateCredentials = true;
      } else {
        updateCredentials = this.initialCredentials.credentialsId !== securityConfig.accessToken;
      }
      if (updateCredentials) {
        newCredentials = {
          credentialsType: DeviceCredentialsType.ACCESS_TOKEN,
          credentialsId: securityConfig.accessToken
        };
      }
    }

    if (updateCredentials) {
      return this.deviceService.saveDeviceCredentials({...this.initialCredentials,...newCredentials});
    }
    return of(null);
  }

  cancel(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }

  private openConfigurationConfirmDialog(): void {
    this.deviceService.getDevice(this.device.id).subscribe(gateway => {
      this.dialog.open<GatewayRemoteConfigurationDialogComponent, GatewayRemoteConfigurationDialogData>
      (GatewayRemoteConfigurationDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          gatewayName: gateway.name
        }
      }).afterClosed().subscribe(
        (res) => {
          if (!res) {
            this.gatewayConfigGroup.get('thingsboard.remoteConfiguration').setValue(true, {emitEvent: false});
          }
        }
      );
    });
  }
}
