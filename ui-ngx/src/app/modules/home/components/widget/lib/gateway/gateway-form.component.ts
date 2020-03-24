///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, ElementRef, Inject, Input, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, FormArray, FormBuilder, FormGroup, NgForm, Validators } from '@angular/forms';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import {
  CONFIGURATION_ATTRIBUTE,
  CONFIGURATION_DRAFT_ATTRIBUTE,
  ConnectorConfig,
  ConnectorForm,
  ConnectorType,
  CURRENT_CONFIGURATION_ATTRIBUTE,
  DEFAULT_CONNECTOR,
  GatewayLogLevel,
  GatewaySetting,
  GatewaySettingStorage,
  GatewaySettingStorageFile,
  GatewaySettingStorageMemory,
  GatewaySettingThingsboard,
  generateConfigConnectorFiles,
  generateFileName,
  generateLogConfigFile,
  generateYAMLConfigurationFile,
  getEntityId,
  getLogsConfig,
  MainGatewaySetting,
  REMOTE_LOGGING_LEVEL_ATTRIBUTE,
  Security,
  SecurityCertificate,
  SecurityToken,
  SecurityType,
  SecurityTypeTranslationMap,
  StorageType,
  StorageTypeTranslationMap,
  ValidateJSON,
  WidgetSetting
} from './gateway-form.models';
import { WINDOW } from '@core/services/window.service';
import { MatDialog } from '@angular/material/dialog';
import {
  JsonObjectEditDialogComponent,
  JsonObjectEditDialogData
} from '@shared/components/dialog/json-object-edit-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { forkJoin, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ImportExportService } from '@home/components/import-export/import-export.service';

@Component({
  selector: 'tb-gateway-form',
  templateUrl: './gateway-form.component.html',
  styleUrls: ['./gateway-form.component.scss']
})

export class GatewayFormComponent extends PageComponent implements OnInit, OnDestroy {
  constructor(
    protected store: Store<AppState>,
    private elementRef: ElementRef,
    private utils: UtilsService,
    private ngZone: NgZone,
    private fb: FormBuilder,
    @Inject(WINDOW) private window: Window,
    private dialog: MatDialog,
    private translate: TranslateService,
    private deviceService: DeviceService,
    private attributeService: AttributeService,
    private importExport: ImportExportService
  ) {
    super(store);
  }

  get connectors(): FormArray {
    return this.gatewayConfigurationGroup.get('connectors') as FormArray;
  }

  @ViewChild('formContainer', {static: true}) formContainerRef: ElementRef<HTMLElement>;
  @ViewChild('gatewayConfigurationForm', {static: true}) multipleInputForm: NgForm;

  private successfulSaved: string;
  private gatewayNameExists: string;
  private archiveFileName: string;
  private formResizeListener: any;
  private subscribeStorageType$: any;
  private subscribeGateway$: any;

  alignment = 'row';
  layoutGap = '5px';
  gatewayType: string;
  gatewayConfigurationGroup: FormGroup;
  securityTypes = SecurityTypeTranslationMap;
  gatewayLogLevels = Object.values(GatewayLogLevel);
  connectorTypes = Object.keys(ConnectorType);
  storageTypes = StorageTypeTranslationMap;

  toastTargetId = 'gateway-configuration-widget' + this.utils.guid();

  @Input()
  ctx: WidgetContext;

  ngOnInit(): void {
    this.initWidgetSettings(this.ctx.settings);

    this.buildForm();
    this.ctx.updateWidgetParams();
    this.formResizeListener = this.resize.bind(this);
    // @ts-ignore
    addResizeListener(this.formContainerRef.nativeElement, this.formResizeListener);
  }

  ngOnDestroy(): void {
    if (this.formResizeListener) {
      // @ts-ignore
      removeResizeListener(this.formContainerRef.nativeElement, this.formResizeListener);
    }
    this.subscribeGateway$.unsubscribe();
    this.subscribeStorageType$.unsubscribe();
  }

  private initWidgetSettings(settings: WidgetSetting): void {
    let widgetTitle;
    if (settings.widgetTitle && settings.widgetTitle.length) {
      widgetTitle = this.utils.customTranslation(settings.widgetTitle, settings.widgetTitle);
    } else {
      widgetTitle = this.translate.instant('gateway.gateway');
    }
    this.ctx.widgetTitle = widgetTitle;

    this.archiveFileName = settings.archiveFileName?.length ? settings.archiveFileName : 'gatewayConfiguration';
    this.gatewayType = settings.gatewayType?.length ? settings.gatewayType : 'Gateway';
    this.gatewayNameExists = this.utils.customTranslation(settings.gatewayNameExists, settings.gatewayNameExists) || this.translate.instant('gateway.gateway-exists');
    this.successfulSaved = this.utils.customTranslation(settings.successfulSave, settings.successfulSave) || this.translate.instant('gateway.gateway-saved');

    this.updateWidgetDisplaying();
  }

  private resize(): void {
    this.ngZone.run(() => {
      this.updateWidgetDisplaying();
      this.ctx.detectChanges();
    });
  }

  private updateWidgetDisplaying() {
    if(this.ctx.$container && this.ctx.$container[0].offsetWidth <= 425){
      this.layoutGap = '0';
      this.alignment = 'column';
    } else {
      this.layoutGap = '5px';
      this.alignment = 'row';
    }
  }

  private saveAttribute(attributeName: string, attributeValue: string, attributeScope: AttributeScope): Observable<any> {
    const gatewayId = this.gatewayConfigurationGroup.get('gateway').value;
    const attributes: AttributeData = {
      key: attributeName,
      value: attributeValue
    };
    return this.attributeService.saveEntityAttributes(getEntityId(gatewayId), attributeScope, [attributes]);
  }

  private createConnector(setting: ConnectorForm = DEFAULT_CONNECTOR): void {
    this.connectors.push(this.fb.group({
      enabled: [setting.enabled],
      configType: [setting.configType, [Validators.required]],
      name: [setting.name, [Validators.required]],
      config: [setting.config, [Validators.nullValidator, ValidateJSON]]
    }));
  }

  private getFormField(name: string): AbstractControl {
    return this.gatewayConfigurationGroup.get(name);
  }

  private buildForm(): void {
    this.gatewayConfigurationGroup = this.fb.group({
      gateway: [null],
      accessToken: [null, [Validators.required]],
      securityType: [SecurityType.accessToken],
      host: [this.window.location.hostname, [Validators.required]],
      port: [1883, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(/^-?[0-9]+$/)]],
      remoteConfiguration: [true],
      caCertPath: ['/etc/thingsboard-gateway/ca.pem'],
      privateKeyPath: ['/etc/thingsboard-gateway/privateKey.pem'],
      certPath: ['/etc/thingsboard-gateway/certificate.pem'],
      remoteLoggingLevel: [GatewayLogLevel.debug],
      remoteLoggingPathToLogs: ['./logs/', [Validators.required]],
      storageType: [StorageType.memory],
      readRecordsCount: [100, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      maxRecordsCount: [10000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      maxFilesCount: [5, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      dataFolderPath: ['./data/', [Validators.required]],
      connectors: this.fb.array([])
    });

    this.subscribeStorageType$ = this.getFormField('storageType').valueChanges.subscribe((value: StorageType) => {
      if (value === StorageType.memory) {
        this.getFormField('maxFilesCount').disable();
        this.getFormField('dataFolderPath').disable();
      } else {
        this.getFormField('maxFilesCount').enable();
        this.getFormField('dataFolderPath').enable();
      }
    });

    this.subscribeGateway$ = this.getFormField('gateway').valueChanges.subscribe((gatewayId: string) => {
      if (gatewayId !== null) {
        forkJoin([
          this.deviceService.getDeviceCredentials(gatewayId).pipe(tap(deviceCredential => {
            this.getFormField('accessToken').patchValue(deviceCredential.credentialsId);
          })),
          ...this.getAttributes(gatewayId)]).subscribe(value => {
          this.gatewayConfigurationGroup.markAsPristine();
          this.ctx.detectChanges();
        });
      } else {
        this.getFormField('accessToken').patchValue('');
      }
    })
  }

  gatewayExist(): void {
    this.ctx.showErrorToast(this.gatewayNameExists, 'top', 'left', this.toastTargetId);
  }

  exportConfig(): void {
    const gatewayConfiguration = this.gatewayConfigurationGroup.value;
    const filesZip: any = {};
    filesZip['tb_gateway.yaml'] = generateYAMLConfigurationFile(gatewayConfiguration);
    generateConfigConnectorFiles(filesZip, gatewayConfiguration.connectors);
    generateLogConfigFile(filesZip, gatewayConfiguration.remoteLoggingLevel, gatewayConfiguration.remoteLoggingPathToLogs);
    this.importExport.exportJSZip(filesZip, this.archiveFileName);
    this.saveAttribute(
      REMOTE_LOGGING_LEVEL_ATTRIBUTE,
      this.gatewayConfigurationGroup.value.remoteLoggingLevel.toUpperCase(),
      AttributeScope.SHARED_SCOPE);
  }

  addNewConnector(): void {
    this.createConnector();
  }

  removeConnector(index: number): void {
    if (index > -1) {
      this.connectors.removeAt(index);
      this.connectors.markAsDirty();
    }
  }

  openConfigDialog($event: Event, index: number, config: object, type: string): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, object>(JsonObjectEditDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: config,
        title: this.translate.instant('gateway.title-connectors-json', {typeName: type})
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.connectors.at(index).get('config').patchValue(res);
          this.ctx.detectChanges();
        }
      }
    );
  }

  private generateConnectorName(name: string, index: number = 0): string {
    const newKeyName = index ? name + index : name;
    const indexRes = this.gatewayConfigurationGroup.value.connectors.findIndex((element) => element.name === newKeyName);
    return indexRes === -1 ? newKeyName : this.generateConnectorName(name, ++index);
  }

  private validateConnectorName(name, connectorIndex, index = 0): string {
    const currentConnectors =  this.gatewayConfigurationGroup.value.connectors;
    for (let i = 0; i < currentConnectors.length; i++) {
      const nameEq = (index === 0) ? name : name + index;
      if (i !== connectorIndex && currentConnectors[i].name === nameEq) {
        this.validateConnectorName(name, connectorIndex, ++index);
      }
    }
    return (index === 0) ? name : name + index;
  }

  changeConnectorType(connector: AbstractControl): void {
    if (!connector.get('name').value) {
      const typeConnector = connector.get('configType').value;
      connector.get('name').patchValue(this.generateConnectorName(ConnectorType[typeConnector]));
    }
  }

  changeConnectorName(connector: AbstractControl, index: number): void {
    connector.get('name').patchValue(this.validateConnectorName(connector.get('name').value, index));
  }

  save(): void {
    const gatewayConfiguration = this.gatewayConfigurationGroup.value;
    forkJoin([
      this.saveAttribute(
        CONFIGURATION_ATTRIBUTE,
        window.btoa(JSON.stringify(this.getGatewayConfigJSON(gatewayConfiguration))),
        AttributeScope.SHARED_SCOPE),
      this.saveAttribute(
        CONFIGURATION_DRAFT_ATTRIBUTE,
        window.btoa(JSON.stringify(this.getDraftConnectorsJSON(gatewayConfiguration.connectors))),
        AttributeScope.SERVER_SCOPE),
      this.saveAttribute(
        REMOTE_LOGGING_LEVEL_ATTRIBUTE,
        this.gatewayConfigurationGroup.value.remoteLoggingLevel.toUpperCase(),
        AttributeScope.SHARED_SCOPE)
    ]).subscribe(() =>{
      this.ctx.showSuccessToast(this.successfulSaved,
        2000, 'top', 'left', this.toastTargetId);
      this.gatewayConfigurationGroup.markAsPristine();
    })
  }

  private getAttributes(gatewayId: string): Array<Observable<Array<AttributeData>>> {
    const tasks = [];
    tasks.push(forkJoin([this.getAttribute(CURRENT_CONFIGURATION_ATTRIBUTE, AttributeScope.CLIENT_SCOPE, gatewayId),
      this.getAttribute(CONFIGURATION_DRAFT_ATTRIBUTE, AttributeScope.SERVER_SCOPE, gatewayId)]).pipe(
        tap(([currentConfig, draftConfig]) => {
          this.processCurrentConfiguration(currentConfig);
          this.processConfigurationDrafts(draftConfig);
        })
      )
    );
    tasks.push(this.getAttribute(REMOTE_LOGGING_LEVEL_ATTRIBUTE, AttributeScope.SHARED_SCOPE, gatewayId).pipe(
      tap(logsLevel => this.processLoggingLevel(logsLevel))
    ));
    return tasks;
  }

  private getAttribute(attributeName: string, attributeScope: AttributeScope, gatewayId: string) {
    return this.attributeService.getEntityAttributes(getEntityId(gatewayId), attributeScope, [attributeName]);
  }

  private getDraftConnectorsJSON(currentConnectors: any) {
    const draftConnectors = {};
    for(const connector of currentConnectors){
      if (!connector.enabled) {
        draftConnectors[connector.name] = {
          connector: connector.configType,
          config: connector.config
        };
      }
    }
    return draftConnectors;
  }

  private getGatewayConfigJSON(gatewayConfiguration: any): MainGatewaySetting {
    const gatewayConfig = {
      thingsboard: this.gatewayMainConfigJSON(gatewayConfiguration)
    };
    this.gatewayConnectorConfigJSON(gatewayConfig, gatewayConfiguration.connectors);
    return gatewayConfig;
  }

  private gatewayMainConfigJSON(gatewayConfiguration: any): GatewaySetting {
    let security: Security;
    if (gatewayConfiguration.securityType === SecurityType.accessToken) {
      security = {
        accessToken: (gatewayConfiguration.accessToken) ? gatewayConfiguration.accessToken : ''
      }
    } else {
      security = {
        caCert: gatewayConfiguration.caCertPath,
        privateKey: gatewayConfiguration.privateKeyPath,
        cert: gatewayConfiguration.certPath
      }
    }
    const thingsboard: GatewaySettingThingsboard = {
      host: gatewayConfiguration.host,
      remoteConfiguration: gatewayConfiguration.remoteConfiguration,
      port: gatewayConfiguration.port,
      security
    };

    let storage: GatewaySettingStorage;
    if (gatewayConfiguration.storageType === StorageType.memory) {
      storage = {
        type: StorageType.memory,
        read_records_count: gatewayConfiguration.readRecordsCount,
        max_records_count: gatewayConfiguration.maxRecordsCount
      };
    } else if (gatewayConfiguration.storageType === StorageType.file) {
      storage = {
        type: StorageType.file,
        data_folder_path: gatewayConfiguration.dataFolderPath,
        max_file_count: gatewayConfiguration.maxFilesCount,
        max_read_records_count: gatewayConfiguration.readRecordsCount,
        max_records_per_file: gatewayConfiguration.maxRecordsCount
      };
    }

    const connectors: Array<ConnectorConfig> = [];
    for (const connector of gatewayConfiguration.connectors) {
      if (connector.enabled) {
        const connectorConfig: ConnectorConfig = {
          configuration: generateFileName(connector.name),
          name: connector.name,
          type: connector.configType
        };
        connectors.push(connectorConfig);
      }
    }

    const configuration =  {
      thingsboard,
      connectors,
      storage,
      logs: window.btoa(getLogsConfig(gatewayConfiguration.remoteLoggingLevel, gatewayConfiguration.remoteLoggingPathToLogs))
    };

    return configuration
  }

  private gatewayConnectorConfigJSON(gatewayConfiguration, currentConnectors): void {
    for (const connector of currentConnectors) {
      if (connector.enabled) {
        const typeConnector = connector.configType;
        if (!Array.isArray(gatewayConfiguration[typeConnector])) {
          gatewayConfiguration[typeConnector] = [];
        }

        const connectorConfig = {
          name: connector.name,
          config: connector.config
        };
        gatewayConfiguration[typeConnector].push(connectorConfig);
      }
    }
  }

  private processCurrentConfiguration(response: Array<AttributeData>): void {
    this.connectors.clear();
    if (response.length > 0) {
      const attribute = JSON.parse(window.atob(response[0].value));
      for (const attributeKey of Object.keys(attribute)) {
        const keyValue = attribute[attributeKey];
        if (attributeKey === 'thingsboard') {
          if (keyValue !== null && Object.keys(keyValue).length > 0) {
            this.setConfigGateway(keyValue);
          }
        } else {
          for (const connector of Object.keys(keyValue)) {
            let name = 'No name';
            if (Object.prototype.hasOwnProperty.call(keyValue[connector], 'name')) {
              name = keyValue[connector].name;
            }
            const newConnector: ConnectorForm = {
              enabled: true,
              configType: (attributeKey as ConnectorType),
              config: keyValue[connector].config,
              name
            };
            this.createConnector(newConnector);
          }
        }
      }
    }
  }

  private processConfigurationDrafts(response: Array<AttributeData>): void {
    if (response.length > 0) {
      const attribute = JSON.parse(window.atob(response[0].value));
      for (const connectorName of Object.keys(attribute)) {
        const newConnector: ConnectorForm = {
          enabled: false,
          configType: (attribute[connectorName].connector as ConnectorType),
          config: attribute[connectorName].config,
          name: connectorName
        };
        this.createConnector(newConnector);
      }
    }
  }

  private setConfigGateway(keyValue: GatewaySetting): void {
    if (Object.prototype.hasOwnProperty.call(keyValue, 'thingsboard')) {
      const formSetting: any = {};
      formSetting.host = keyValue.thingsboard.host;
      formSetting.port = keyValue.thingsboard.port;
      formSetting.remoteConfiguration = keyValue.thingsboard.remoteConfiguration;
      if (Object.prototype.hasOwnProperty.call(keyValue.thingsboard.security, SecurityType.accessToken)) {
        formSetting.securityType = SecurityType.accessToken;
        formSetting.accessToken = (keyValue.thingsboard.security as SecurityToken).accessToken;
      } else {
        formSetting.securityType = SecurityType.tls;
        formSetting.caCertPath = (keyValue.thingsboard.security as SecurityCertificate).caCert;
        formSetting.privateKeyPath = (keyValue.thingsboard.security as SecurityCertificate).privateKey;
        formSetting.certPath = (keyValue.thingsboard.security as SecurityCertificate).cert;
      }
      this.gatewayConfigurationGroup.patchValue(formSetting);
    }

    if (Object.prototype.hasOwnProperty.call(keyValue, 'storage') && Object.prototype.hasOwnProperty.call(keyValue.storage, 'type')) {
      const formSetting: any = {};
      if (keyValue.storage.type === StorageType.memory) {
        formSetting.storageType = StorageType.memory;
        formSetting.readRecordsCount = (keyValue.storage as GatewaySettingStorageMemory).read_records_count;
        formSetting.maxRecordsCount = (keyValue.storage as GatewaySettingStorageMemory).max_records_count;
      } else if (keyValue.storage.type === StorageType.file) {
        formSetting.storageType = StorageType.file;
        formSetting.dataFolderPath = (keyValue.storage as GatewaySettingStorageFile).data_folder_path;
        formSetting.maxFilesCount = (keyValue.storage as GatewaySettingStorageFile).max_file_count;
        formSetting.readRecordsCount = (keyValue.storage as GatewaySettingStorageMemory).read_records_count;
        formSetting.maxRecordsCount = (keyValue.storage as GatewaySettingStorageMemory).max_records_count;
      }
      this.gatewayConfigurationGroup.patchValue(formSetting);
    }
  }

  private processLoggingLevel(value: Array<AttributeData>): void {
    let logsLevel = GatewayLogLevel.debug;
    if (value.length > 0 && GatewayLogLevel[value[0].value.toLowerCase()]) {
      logsLevel = GatewayLogLevel[value[0].value.toLowerCase()];
    }
    this.getFormField('remoteLoggingLevel').patchValue(logsLevel);
  }
}
