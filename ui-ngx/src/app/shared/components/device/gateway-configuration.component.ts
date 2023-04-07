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

import { Component, Input, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { base64toObj, base64toString, objToBase64, stringToBase64 } from '@core/utils';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  GatewayRemoteConfigurationDialogComponent,
  GatewayRemoteConfigurationDialogData
} from '@shared/components/dialog/gateway-remote-configuration-dialog';
import { DeviceService } from '@core/http/device.service';

export enum StorageTypes {
  MEMORY = 'memory',
  FILE = 'file',
  SQLITE = 'sqlite'
}

export enum GatewayLogLevel {
  none = 'NONE',
  critical = 'CRITICAL',
  error = 'ERROR',
  warning = 'WARNING',
  info = 'INFO',
  debug = 'DEBUG'
}

export enum LogSavingPeriod {
  days = 'd',
  hours = 'h',
  minutes = 'm',
  seconds = 's'
}

export enum LocalLogsConfigs {
  service = 'service',
  connector = 'connector',
  converter = 'converter',
  tb_connection = 'tb_connection',
  storage = 'storage',
  extension = 'extension'
}

export const logSavingPeriodTranslations = new Map<LogSavingPeriod, string>(
  [
    [LogSavingPeriod.days, 'gateway.logs.days'],
    [LogSavingPeriod.hours, 'gateway.logs.hours'],
    [LogSavingPeriod.minutes, 'gateway.logs.minutes'],
    [LogSavingPeriod.seconds, 'gateway.logs.seconds']
  ]
);

export const storageTypesTranslations = new Map<StorageTypes, string>(
  [
    [StorageTypes.MEMORY, 'gateway.storage-types.memory-storage'],
    [StorageTypes.FILE, 'gateway.storage-types.file-storage'],
    [StorageTypes.SQLITE, 'gateway.storage-types.sqlite']
  ]
);

export enum SecurityTypes {
  ACCESS_TOKEN = 'accessToken',
  USERNAME_PASSWORD = 'usernamePassword',
  TLS_ACCESS_TOKEN = 'tlsAccessToken',
  TLS_PRIVATE_KEY = 'tlsPrivateKey'
}

export const securityTypesTranslationsMap = new Map<SecurityTypes, string>(
  [
    [SecurityTypes.ACCESS_TOKEN, 'gateway.security-types.access-token'],
    [SecurityTypes.USERNAME_PASSWORD, 'gateway.security-types.username-password'],
    [SecurityTypes.TLS_ACCESS_TOKEN, 'gateway.security-types.tls-access-token'],
    [SecurityTypes.TLS_PRIVATE_KEY, 'gateway.security-types.tls-private-key'],
  ]
);

@Component({
  selector: 'tb-gateway-configuration',
  templateUrl: './gateway-configuration.component.html',
  styleUrls: ['./gateway-configuration.component.scss']
})
export class GatewayConfigurationComponent implements OnInit {

  gatewayConfigGroup: FormGroup;

  storageTypes = storageTypesTranslations;

  logSavingPeriods = logSavingPeriodTranslations;

  securityTypes = securityTypesTranslationsMap;

  gatewayLogLevel = Object.values(GatewayLogLevel);

  localLogsConfigs = Object.values(LocalLogsConfigs);

  @Input()
  device: EntityId;

  @Input()
  dialogRef: MatDialogRef<any>;

  logSelector: FormControl;


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              public dialog: MatDialog) {
  }

  ngOnInit() {
    this.gatewayConfigGroup = this.fb.group({
      thingsboard: this.fb.group({
        host: ['thingsboard.cloud', [Validators.required]],
        port: [1883, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(/^-?[0-9]+$/)]],
        remoteShell: [false, []],
        remoteConfiguration: [true, []],
        statistics: this.fb.group({
          enable: [true, []],
          statsSendPeriodInSeconds: [3600, [Validators.required, Validators.min(0), Validators.pattern(/^-?[0-9]+$/)]],
          checkConnectorsConfigurationInSeconds: [60, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
          handleDeviceRenaming: [false, []],
          commands: this.fb.array([], [])
        }),
        maxPayloadSizeBytes: [1024, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        minPackSendDelayMS: [200, [Validators.required, Validators.min(0), Validators.pattern(/^-?[0-9]+$/)]],
        minPackSizeToSend: [500, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        handleDeviceRenaming: [true, []],
        checkingDeviceActivity: this.fb.group({
          checkDeviceInactivity: [false, []],
          inactivityTimeoutSeconds: [200, [Validators.min(1)]],
          inactivityCheckPeriodSeconds: [500, [Validators.min(1)]]
        }),
        security: this.fb.group({
          type: [SecurityTypes.ACCESS_TOKEN, [Validators.required]],
          accessToken: [null, [Validators.required]],
          clientId: [null, []],
          username: [null, []],
          password: [null, []],
          caCert: [null, []],
          cert: [null, []],
          privateKey: [null, []],
        }),
        qos: [1, [Validators.min(0), Validators.max(2), Validators.required]]
      }),
      storage: this.fb.group({
        type: [StorageTypes.MEMORY, [Validators.required]],
        read_records_count: [100, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]],
        max_records_count: [100000, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]],
        data_folder_path: ['./data/', []],
        max_file_count: [10, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        max_read_records_count: [10, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        max_records_per_file: [10000, [Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
        data_file_path: ['./data/data.db', []],
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
        dateFormat: ['%Y-%m-%d %H:%M:%S', [Validators.required]],
        logFormat: ['%(asctime)s - |%(levelname)s| - [%(filename)s] - %(module)s - %(funcName)s - %(lineno)d - %(message)s',
          [Validators.required]],
        type: ['remote', [Validators.required]],
        remote: this.fb.group({
          enabled: [false],
          logLevel: [GatewayLogLevel.info, [Validators.required]],
        }),
        local: this.fb.group({})
      })
    });

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
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').setValidators([Validators.min(1), Validators.required]);
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').setValidators([Validators.min(1), Validators.required]);
      } else {
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').clearValidators();
        checkingDeviceActivityGroup.get('inactivityTimeoutSeconds').setErrors(null);
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').clearValidators();
        checkingDeviceActivityGroup.get('inactivityCheckPeriodSeconds').setErrors(null);
      }
    });

    const securityGroup = this.gatewayConfigGroup.get('thingsboard.security') as FormGroup;
    securityGroup.get('type').valueChanges.subscribe(type => {
      this.removeAllSecurityValidators();
      if (type === SecurityTypes.ACCESS_TOKEN) {
        securityGroup.get('accessToken').addValidators([Validators.required]);
        securityGroup.get('accessToken').updateValueAndValidity();
      } else if (type === SecurityTypes.TLS_PRIVATE_KEY) {
        securityGroup.get('caCert').addValidators([Validators.required]);
        securityGroup.get('caCert').updateValueAndValidity();
        securityGroup.get('privateKey').addValidators([Validators.required]);
        securityGroup.get('privateKey').updateValueAndValidity();
        securityGroup.get('cert').addValidators([Validators.required]);
        securityGroup.get('cert').updateValueAndValidity();
      } else if (type === SecurityTypes.TLS_ACCESS_TOKEN) {
        securityGroup.get('accessToken').addValidators([Validators.required]);
        securityGroup.get('accessToken').updateValueAndValidity();
        securityGroup.get('caCert').addValidators([Validators.required]);
        securityGroup.get('caCert').updateValueAndValidity();
      } else if (type === SecurityTypes.USERNAME_PASSWORD) {
        securityGroup.get('clientId').addValidators([Validators.required]);
        securityGroup.get('clientId').updateValueAndValidity();
        securityGroup.get('username').addValidators([Validators.required]);
        securityGroup.get('username').updateValueAndValidity();
        securityGroup.get('password').addValidators([Validators.required]);
        securityGroup.get('password').updateValueAndValidity();
      }
      securityGroup.updateValueAndValidity();
    });

    const storageGroup = this.gatewayConfigGroup.get('storage') as FormGroup;
    storageGroup.get('type').valueChanges.subscribe(type => {
      this.removeAllStorageValidators();
      if (type === StorageTypes.MEMORY) {
        storageGroup.get('read_records_count').addValidators(
          [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]);
        storageGroup.get('max_records_count').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
      } else if (type === StorageTypes.FILE) {
        storageGroup.get('data_folder_path').addValidators([Validators.required]);
        storageGroup.get('max_file_count').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('max_read_records_count').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('max_records_per_file').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
      } else if (type === StorageTypes.SQLITE) {
        storageGroup.get('data_file_path').addValidators([Validators.required]);
        storageGroup.get('messages_ttl_check_in_hours').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
        storageGroup.get('messages_ttl_in_days').addValidators(
          [Validators.min(1), Validators.pattern(/^-?[0-9]+$/), Validators.required]);
      }
    });

    this.fetchConfigAttribute(this.device);
  }

  fetchConfigAttribute(entityId: EntityId) {
    this.attributeService.getEntityAttributes(entityId, AttributeScope.SHARED_SCOPE,
      ['configuration', 'RemoteLoggingLevel']).subscribe(attributes => {
      if (attributes.length) {
        const configuration = attributes.find(attribute => attribute.key === 'configuration').value;
        const remoteLoggingLevel = attributes.find(attribute => attribute.key === 'RemoteLoggingLevel').value;
        if (configuration) {
          const configObj = base64toObj(configuration).thingsboard;
          configObj.logs = this.logsBase64ToObj(configObj.logs);
          if (configObj.thingsboard.statistics && configObj.thingsboard.statistics.commands) {
            for (const command of Object.values(configObj.thingsboard.statistics.commands)) {
              this.addCommand(command);
            }
            delete configObj.thingsboard.statistics.commands;
          }
          this.gatewayConfigGroup.patchValue(configObj, {emitEvent: false});
          this.gatewayConfigGroup.markAsDirty();
          if (!configObj.thingsboard.remoteConfiguration) {
            this.gatewayConfigGroup.disable({emitEvent: false});
          }
        }
        if (remoteLoggingLevel) {
          const remoteLogsFormGroup = this.gatewayConfigGroup.get('logs.remote');
          remoteLogsFormGroup.patchValue({
            enabled: remoteLoggingLevel !== GatewayLogLevel.none,
            logLevel: remoteLoggingLevel
          }, {emitEvent: false});
          remoteLogsFormGroup.markAsDirty();
        }
      }
    });
  }

  logsBase64ToObj(logs64: string) {
    const logsString = base64toString(logs64).split('"').join('');
    const logsObject = {};
    logsString.split('logger_').forEach((split, index) => {
      if (index > 0) {
        const key = split.split(']')[0];
        if (LocalLogsConfigs[key]) {
          const logLevel = split.split('=')[1].replace('}}handlers', '');
          logsObject[key] = {
            logLevel
          };
        }
      }
    });
    logsString.split('handler_').forEach((split, index) => {
      if (index > 0) {
        const key = split.split('Handler]')[0];
        if (LocalLogsConfigs[key]) {
          const args = split.split('args=(')[1].split(',');
          logsObject[key].filePath = args[0].replace(`/${key}.log`, '');
          logsObject[key].savingPeriod = args[1].replace(' ', '');
          logsObject[key].savingTime = +args[2];
          logsObject[key].backupCount = +args[3];
        }
      }
    });
    const formatArgs = logsString.split('format=')[1].split('}}datefmt=');
    const logFormat = formatArgs[0];
    const dateFormat = formatArgs[1];

    return {local: logsObject, logFormat, dateFormat};
  }

  addCommand(command?): void {
    const data = command || {};
    const commandsFormArray = this.commandFormArray();
    const commandFormGroup = this.fb.group({
      attributeName: [data.attributeName || null, [Validators.required]],
      command: [data.command || null, [Validators.required]],
      timeout: [data.timeout || null, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
    });
    commandsFormArray.push(commandFormGroup);
  }

  addLocalLogConfig(name, config): void {
    const localLogsFormGroup = this.gatewayConfigGroup.get('logs.local') as FormGroup;
    const configGroup = this.fb.group({
      logLevel: [config.logLevel || GatewayLogLevel.info, [Validators.required]],
      filePath: [config.filePath || './data', [Validators.required]],
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

  removeCommandControl(index: number): void {
    this.commandFormArray().removeAt(index);
  }

  removeAllSecurityValidators(): void {
    const securityGroup = this.gatewayConfigGroup.get('thingsboard.security') as FormGroup;
    for (const controlsKey in securityGroup.controls) {
      if (controlsKey !== 'type') {
        securityGroup.controls[controlsKey].clearValidators();
        securityGroup.controls[controlsKey].setErrors(null);
        securityGroup.controls[controlsKey].updateValueAndValidity();
      }
    }
  }

  removeAllStorageValidators(): void {
    const storageGroup = this.gatewayConfigGroup.get('storage') as FormGroup;
    for (const storageKey in storageGroup.controls) {
      if (storageKey !== 'type') {
        storageGroup.controls[storageKey].clearValidators();
        storageGroup.controls[storageKey].setErrors(null);
        storageGroup.controls[storageKey].updateValueAndValidity();
      }
    }
  }

  removeEmpty(obj) {
    return Object.fromEntries(
      Object.entries(obj)
        .filter(([_, v]) => v != null)
        .map(([k, v]) => [k, v === Object(v) ? this.removeEmpty(v) : v])
    );
  }

  generateLogsFile(logsObj): string {
    const line = '[loggers]}}' +
      'keys=root, service, connector, converter, tb_connection, storage, extension}}' +
      '[handlers]}}' +
      'keys=consoleHandler, serviceHandler, connectorHandler, converterHandler, tb_connectionHandler, storageHandler, extensionHandler}}' +
      '[formatters]}}' +
      'keys=LogFormatter}}' +
      '[logger_root]}}' +
      'level=ERROR}}' +
      'handlers=consoleHandler}}' +
      '[logger_database]}}' +
      'level=INFO}}' +
      'handlers=databaseHandler}}' +
      'formatter=LogFormatter}}' +
      'qualname=database}}';

    let loggerLine = '';
    let handlerLine = '';

    for (const key of Object.keys(logsObj.local)) {
      loggerLine += `[logger_${key}]}}` +
        `level=${logsObj.local[key].logLevel}}}` +
        `handlers=${key}Handler}}` +
        `formatter=LogFormatter}}` +
        `qualname=${key}}}`;
      handlerLine += `[handler_${key}Handler]}}` +
        `level=${logsObj.local[key].logLevel}}}` +
        `class=thingsboard_gateway.tb_utility.tb_logger.TimedRotatingFileHandler}}` +
        `formatter=LogFormatter}}` +
        `args=("${logsObj.local[key].filePath}/${key}.log", ${logsObj.local[key].savingPeriod},` +
        ` ${logsObj.local[key].savingTime}, ${logsObj.local[key].backupCount},)}}`;
    }

    const logerEnding = `[formatter_LogFormatter]}}` +
      `format="${logsObj.logFormat}"}}` +
      `datefmt="${logsObj.dateFormat}"`;

    return line + loggerLine + handlerLine + logerEnding;
  }

  saveConfig(): void {
    const value = this.removeEmpty(this.gatewayConfigGroup.value);
    const attributes = [];
    attributes.push({
      key: 'RemoteLoggingLevel',
      value: value.logs.remote.logLevel
    });
    delete value.connectors;
    value.logs = stringToBase64(this.generateLogsFile(value.logs));
    const configuration = objToBase64({thingsboard: value});
    attributes.push({
      key: 'configuration',
      value: configuration
    });
    this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, attributes).subscribe(_ => {
      if (this.dialogRef) {
        this.dialogRef.close();
      }
    });
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
