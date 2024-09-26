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

import { ChangeDetectorRef, Component, Input, AfterViewInit, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { DeviceService } from '@core/http/device.service';
import { Observable, of, Subject } from 'rxjs';
import { mergeMap, switchMap, takeUntil } from 'rxjs/operators';
import { DeviceCredentials, DeviceCredentialsType } from '@shared/models/device.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import {
  GatewayLogLevel,
  SecurityTypes,
  ConfigurationModes,
  LocalLogsConfigs,
  LogSavingPeriod, Attribute
} from '../gateway-widget.models';
import { deepTrim, isEqual } from '@core/utils';
import {
  GatewayConfigSecurity,
  GatewayConfigValue,
  GatewayGeneralConfig,
  GatewayGRPCConfig,
  GatewayLogsConfig,
  GatewayStorageConfig,
  LocalLogs,
  LogAttribute,
  LogConfig,
} from './models/gateway-configuration.models';
import { DeviceId } from '@shared/models/id/device-id';

@Component({
  selector: 'tb-gateway-configuration',
  templateUrl: './gateway-configuration.component.html',
  styleUrls: ['./gateway-configuration.component.scss']
})
export class GatewayConfigurationComponent implements AfterViewInit, OnDestroy {

  @Input() device: EntityId;

  @Input() dialogRef: MatDialogRef<GatewayConfigurationComponent>;

  initialCredentials: DeviceCredentials;
  gatewayConfigGroup: FormGroup;
  ConfigurationModes = ConfigurationModes;

  private destroy$ = new Subject<void>();
  private readonly gatewayConfigAttributeKeys =
    ['general_configuration', 'grpc_configuration', 'logs_configuration', 'storage_configuration', 'RemoteLoggingLevel', 'mode'];

  constructor(private fb: FormBuilder,
              private attributeService: AttributeService,
              private deviceService: DeviceService,
              private cd: ChangeDetectorRef
  ) {

    this.gatewayConfigGroup = this.fb.group({
      basicConfig: [],
      advancedConfig: [],
      mode: [ConfigurationModes.BASIC],
    });

    this.observeAlignConfigs();
  }

  ngAfterViewInit(): void {
    this.fetchConfigAttribute(this.device);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  saveConfig(): void {
    const { mode, advancedConfig } = deepTrim(this.removeEmpty(this.gatewayConfigGroup.value));
    const value = { mode, ...advancedConfig as GatewayConfigValue };
    value.thingsboard.statistics.commands = Object.values(value.thingsboard.statistics.commands ?? []);
    const attributes = this.generateAttributes(value);

    this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, attributes).pipe(
      switchMap(_ => this.updateCredentials(value.thingsboard.security)),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      if (this.dialogRef) {
        this.dialogRef.close();
      } else {
        this.gatewayConfigGroup.markAsPristine();
        this.cd.detectChanges();
      }
    });
  }

  private observeAlignConfigs(): void {
    this.gatewayConfigGroup.get('basicConfig').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(value => {
      const advancedControl = this.gatewayConfigGroup.get('advancedConfig');

      if (!isEqual(advancedControl.value, value) && this.gatewayConfigGroup.get('mode').value === ConfigurationModes.BASIC) {
        advancedControl.patchValue(value, {emitEvent: false});
      }
    });

    this.gatewayConfigGroup.get('advancedConfig').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(value => {
      const basicControl = this.gatewayConfigGroup.get('basicConfig');

      if (!isEqual(basicControl.value, value) && this.gatewayConfigGroup.get('mode').value === ConfigurationModes.ADVANCED) {
        basicControl.patchValue(value, {emitEvent: false});
      }
    });
  }

  private generateAttributes(value: GatewayConfigValue): Attribute[] {
    const attributes = [];

    const addAttribute = (key: string, val: unknown) => {
      attributes.push({ key, value: val });
    };

    const addTimestampedAttribute = (key: string, val: unknown) => {
      val = {...val as Record<string, unknown>, ts: new Date().getTime()};
      addAttribute(key, val);
    };

    addAttribute('RemoteLoggingLevel', value.logs?.remote?.enabled ? value.logs.remote.logLevel : GatewayLogLevel.NONE);

    delete value.connectors;
    addAttribute('logs_configuration', this.generateLogsFile(value.logs));

    addTimestampedAttribute('grpc_configuration', value.grpc);
    addTimestampedAttribute('storage_configuration', value.storage);
    addTimestampedAttribute('general_configuration', value.thingsboard);

    addAttribute('mode', value.mode);

    return attributes;
  }

  private updateCredentials(securityConfig: GatewayConfigSecurity): Observable<DeviceCredentials> {
    let newCredentials: Partial<DeviceCredentials> = {};

    switch (securityConfig.type) {
      case SecurityTypes.USERNAME_PASSWORD:
        if (this.shouldUpdateCredentials(securityConfig)) {
          newCredentials = this.generateMqttCredentials(securityConfig);
        }
        break;

      case SecurityTypes.ACCESS_TOKEN:
      case SecurityTypes.TLS_ACCESS_TOKEN:
        if (this.shouldUpdateAccessToken(securityConfig)) {
          newCredentials = {
            credentialsType: DeviceCredentialsType.ACCESS_TOKEN,
            credentialsId: securityConfig.accessToken
          };
        }
        break;
    }

    return Object.keys(newCredentials).length
      ? this.deviceService.saveDeviceCredentials({ ...this.initialCredentials, ...newCredentials })
      : of(null);
  }

  private shouldUpdateCredentials(securityConfig: GatewayConfigSecurity): boolean {
    if (this.initialCredentials.credentialsType !== DeviceCredentialsType.MQTT_BASIC) {
      return true;
    }
    const parsedCredentials = JSON.parse(this.initialCredentials.credentialsValue);
    return !(
      parsedCredentials.clientId === securityConfig.clientId &&
      parsedCredentials.userName === securityConfig.username &&
      parsedCredentials.password === securityConfig.password
    );
  }

  private generateMqttCredentials(securityConfig: GatewayConfigSecurity): Partial<DeviceCredentials> {
    const { clientId, username, password } = securityConfig;

    const credentialsValue = {
      ...(clientId && { clientId }),
      ...(username && { userName: username }),
      ...(password && { password }),
    };

    return {
      credentialsType: DeviceCredentialsType.MQTT_BASIC,
      credentialsValue: JSON.stringify(credentialsValue)
    };
  }

  private shouldUpdateAccessToken(securityConfig: GatewayConfigSecurity): boolean {
    return this.initialCredentials.credentialsType !== DeviceCredentialsType.ACCESS_TOKEN ||
      this.initialCredentials.credentialsId !== securityConfig.accessToken;
  }

  cancel(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }

  private removeEmpty(obj: Record<string, unknown>): Record<string, unknown> {
    return Object.fromEntries(
      Object.entries(obj)
        .filter(([_, v]) => v != null)
        .map(([k, v]) => [k, v === Object(v) ? this.removeEmpty(v as Record<string, unknown>) : v])
    );
  }

  private generateLogsFile(logsObj: GatewayLogsConfig): LogAttribute {
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
          level: 0,
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

    this.addLocalLoggers(logAttrObj, logsObj.local);

    return logAttrObj;
  }

  private addLocalLoggers(logAttrObj: LogAttribute, localLogs: LocalLogs): void {
    for (const key of Object.keys(localLogs)) {
      logAttrObj.handlers[key + 'Handler'] = this.createHandlerObj(localLogs[key], key);
      logAttrObj.loggers[key] = this.createLoggerObj(localLogs[key], key);
    }
  }

  private createHandlerObj(logObj: LogConfig, key: string) {
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

  private createLoggerObj(logObj: LogConfig, key: string) {
    return {
      handlers: [`${key}Handler`, 'consoleHandler'],
      level: logObj.logLevel,
      propagate: false
    };
  }

  private fetchConfigAttribute(entityId: EntityId): void {
    if (entityId.id === NULL_UUID) {
      return;
    }

    this.attributeService.getEntityAttributes(entityId, AttributeScope.CLIENT_SCOPE,
      )
      .pipe(
        mergeMap(attributes => attributes.length ? of(attributes) : this.attributeService.getEntityAttributes(
          entityId, AttributeScope.SHARED_SCOPE, this.gatewayConfigAttributeKeys)
        ),
        takeUntil(this.destroy$)
      )
      .subscribe(attributes => {
        this.updateConfigs(attributes);
        this.cd.detectChanges();
      });
  }

  private updateConfigs(attributes: AttributeData[]): void {
    const formValue: GatewayConfigValue = {
      thingsboard: {} as GatewayGeneralConfig,
      grpc: {} as GatewayGRPCConfig,
      logs: {} as GatewayLogsConfig,
      storage: {} as GatewayStorageConfig,
      mode: ConfigurationModes.BASIC
    };

    attributes.forEach(attr => {
      switch (attr.key) {
        case 'general_configuration':
          formValue.thingsboard = attr.value;
          this.updateFormControls(attr.value);
          break;
        case 'grpc_configuration':
          formValue.grpc = attr.value;
          break;
        case 'logs_configuration':
          formValue.logs = this.logsToObj(attr.value);
          break;
        case 'storage_configuration':
          formValue.storage = attr.value;
          break;
        case 'mode':
          formValue.mode = attr.value;
          break;
        case 'RemoteLoggingLevel':
          formValue.logs = {
            ...formValue.logs,
            remote: {
              enabled: attr.value !== GatewayLogLevel.NONE,
              logLevel: attr.value
            }
          };
      }
    });

    this.gatewayConfigGroup.get('basicConfig').setValue(formValue, { emitEvent: false });
    this.gatewayConfigGroup.get('advancedConfig').setValue(formValue, { emitEvent: false });
  }

  private updateFormControls(thingsboard: GatewayGeneralConfig): void {
    const { type, accessToken, ...securityConfig } = thingsboard.security ?? {};

    this.initialCredentials = {
      deviceId: this.device as DeviceId,
      credentialsType: type as unknown as DeviceCredentialsType,
      credentialsId: accessToken,
      credentialsValue: JSON.stringify(securityConfig)
    };
  }

  private logsToObj(logsConfig: LogAttribute): GatewayLogsConfig {
    const { format: logFormat, datefmt: dateFormat } = logsConfig.formatters.LogFormatter;

    const localLogs = Object.keys(LocalLogsConfigs).reduce((acc, key) => {
      const handler = logsConfig.handlers[`${key}Handler`] || {};
      const logger = logsConfig.loggers[key] || {};

      acc[key] = {
        logLevel: logger.level || GatewayLogLevel.INFO,
        filePath: handler.filename?.split(`/${key}`)[0] || './logs',
        backupCount: handler.backupCount || 7,
        savingTime: handler.interval || 3,
        savingPeriod: handler.when || LogSavingPeriod.days
      };

      return acc;
    }, {}) as LocalLogs;

    return { local: localLogs, logFormat, dateFormat };
  }
}
