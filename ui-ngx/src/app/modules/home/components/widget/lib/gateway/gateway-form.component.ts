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
  connectorType,
  CURRENT_CONFIGURATION_ATTRIBUTE,
  gatewayLogLevel,
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
  REMOTE_LOGGING_LEVEL_ATTRIBUTE,
  Security,
  SecurityCertificate,
  SecurityToken,
  SecurityType,
  securityTypeTranslationMap,
  storageType,
  storageTypeTranslationMap,
  ValidateJSON
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

  get connectors() : FormArray {
    return this.gatewayConfigurationGroup.get('connectors') as FormArray;
  }

  @ViewChild('formContainer', {static: true}) formContainerRef: ElementRef<HTMLElement>;
  @ViewChild('gatewayConfigurationForm', {static: true}) multipleInputForm: NgForm;

  private successfulSaved =  this.translate.instant('gateway.gateway-saved');
  private archiveFileName = 'gatewayConfiguration';
  private formResizeListener: any;
  private subscribeStorageType$: any;
  private subscribeGateway$: any;

  disabled = false;
  changeAlignment = false;
  countConnectors = 0;
  gatewayConfigurationGroup: FormGroup;
  securityTypes = securityTypeTranslationMap;
  gatewayLogLevels = Object.values(gatewayLogLevel);
  connectorTypes = Object.keys(connectorType);
  storageTypes = storageTypeTranslationMap;

  toastTargetId = 'gateway-configuration-widget' + this.utils.guid();

  @Input()
  ctx: WidgetContext;

  ngOnInit(): void {
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

  private resize() {
    this.ngZone.run(() => {
      this.updateWidgetDisplaying();
      this.ctx.detectChanges();
    });
  }

  private updateWidgetDisplaying() {

  }

  private saveAttribute(gatewayId: string, attributeName: string, attributeValue: string, attributeScope: AttributeScope) {
    const attributes: AttributeData = {
      key: attributeName,
      value: attributeValue
    };
    return this.attributeService.saveEntityAttributes(getEntityId(gatewayId), attributeScope, [attributes]);
  }

  private createConnector(): FormGroup {
    return this.fb.group({
      enabled: [false],
      configType: [null, [Validators.required]],
      name: ['', [Validators.required]],
      config: [{}, [Validators.nullValidator, ValidateJSON]]
    });
  }

  private buildForm() {
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
      remoteLoggingLevel: [gatewayLogLevel.debug],
      remoteLoggingPathToLogs: ['./logs/', [Validators.required]],
      storageTypes: [storageType.memoryStorage],
      readRecordsCount: [100, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      maxRecordsCount: [10000, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      maxFilesCount: [5, [Validators.required, Validators.min(1), Validators.pattern(/^-?[0-9]+$/)]],
      dataFolderPath: ['./data/', [Validators.required]],
      connectors: this.fb.array([])
    });

    this.subscribeStorageType$ = this.gatewayConfigurationGroup.get('storageTypes').valueChanges.subscribe((value: storageType) => {
      if(value === storageType.memoryStorage){
        this.gatewayConfigurationGroup.get('maxFilesCount').disable();
        this.gatewayConfigurationGroup.get('dataFolderPath').disable();
      } else {
        this.gatewayConfigurationGroup.get('maxFilesCount').enable();
        this.gatewayConfigurationGroup.get('dataFolderPath').enable();
      }
    });

    this.subscribeGateway$ = this.gatewayConfigurationGroup.get('gateway').valueChanges.subscribe((gatewayId: string) => {
      if(gatewayId !== null) {
        forkJoin([
          this.deviceService.getDeviceCredentials(gatewayId).pipe(tap(deviceCredential => {
            this.gatewayConfigurationGroup.get('accessToken').patchValue(deviceCredential.credentialsId);
          })),
          ...this.getAttributes(gatewayId)]).subscribe(value => {
          this.gatewayConfigurationGroup.markAsPristine();
          this.ctx.detectChanges();
          console.log(gatewayId, value);
        });
      } else {
        this.gatewayConfigurationGroup.get('accessToken').patchValue('');
      }
    })
  }

  exportConfig() {
    const gatewayValues = this.gatewayConfigurationGroup.value;
    const filesZip: any = {};
    filesZip['tb_gateway.yaml'] = generateYAMLConfigurationFile(gatewayValues);
    generateConfigConnectorFiles(filesZip, gatewayValues.connectors);
    generateLogConfigFile(filesZip, gatewayValues.remoteLoggingLevel, gatewayValues.remoteLoggingPathToLogs);
    this.importExport.exportJSZip(filesZip, this.archiveFileName);
    this.saveAttribute(this.gatewayConfigurationGroup.value.gateway,
      REMOTE_LOGGING_LEVEL_ATTRIBUTE,
      this.gatewayConfigurationGroup.value.remoteLoggingLevel.toUpperCase(),
      AttributeScope.SHARED_SCOPE);
  }

  addNewConnector() {
    this.connectors.push(this.createConnector());
    this.countConnectors++;
  }

  removeConnector(index: number) {
    if (index > -1) {
      this.connectors.removeAt(index);
    }
  }

  openConfigDialog($event: Event, index: number, config: object, type: string): void{
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, object>(JsonObjectEditDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: config,
        title: this.translate.instant('gateway.title-connectors-json', { typeName: type})
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
      connector.get('name').patchValue(this.generateConnectorName(connectorType[typeConnector]));
    }
  }

  changeConnectorName(connector: AbstractControl, connectorIndex: number): void {
    connector.get('name').patchValue(this.validateConnectorName(connector.get('name').value, connectorIndex));
  }

  save(): void {
    const dateweyId = this.gatewayConfigurationGroup.get('gateway').value;
    forkJoin([
      this.saveAttribute(
        dateweyId,
        CONFIGURATION_ATTRIBUTE,
        window.btoa(JSON.stringify(this.getGatewayConfigJSON())),
        AttributeScope.SHARED_SCOPE),
      this.saveAttribute(
        dateweyId,
        CONFIGURATION_DRAFT_ATTRIBUTE,
        window.btoa(JSON.stringify(this.getDraftConnectorsJSON())),
        AttributeScope.SERVER_SCOPE),
      this.saveAttribute(
        dateweyId,
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

  private getDraftConnectorsJSON() {
    const currentConnectors =  this.gatewayConfigurationGroup.value.connectors;
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

  private getGatewayConfigJSON() {
    const gatewayConfig = {
      thingsboard: null
    };
    gatewayConfig.thingsboard = this.gatewayMainConfigJSON();
    this.gatewayConnectorConfigJSON(gatewayConfig);
    return gatewayConfig;
  }

  private gatewayMainConfigJSON() {
    const gatewayValues = this.gatewayConfigurationGroup.value;
    let security: Security;
    if (gatewayValues.securityType === SecurityType.accessToken) {
      security = {
        accessToken: (gatewayValues.accessToken) ? gatewayValues.accessToken : ''
      }
    } else {
      security = {
        caCert: gatewayValues.caCertPath,
        privateKey: gatewayValues.privateKeyPath,
        cert: gatewayValues.certPath
      }
    }
    const thingsboard: GatewaySettingThingsboard = {
      host: gatewayValues.host,
      remoteConfiguration: gatewayValues.remoteConfiguration,
      port: gatewayValues.port,
      security
    };

    let storage: GatewaySettingStorage;
    if (gatewayValues.storageType === storageType.memoryStorage) {
      storage = {
        type: 'memory',
        read_records_count: gatewayValues.readRecordsCount,
        max_records_count: gatewayValues.maxRecordsCount
      };
    } else if (gatewayValues.storageType ===  storageType.fileStorage) {
      storage = {
        type: 'file',
        data_folder_path: gatewayValues.dataFolderPath,
        max_file_count: gatewayValues.maxFilesCount,
        max_read_records_count: gatewayValues.readRecordsCount,
        max_records_per_file: gatewayValues.maxRecordsCount
      };
    }

    const connectors: Array<ConnectorConfig> = [];
    for( const connector of gatewayValues.connectors) {
      if (connector.enabled) {
        const connectorConfig: ConnectorConfig = {
          configuration: generateFileName(connector.name),
          name: connector.name,
          type: connector.configType
        };
        connectors.push(connectorConfig);
      }
    }

    const configuration: GatewaySetting = {
      thingsboard,
      connectors,
      storage,
      logs: window.btoa(getLogsConfig(gatewayValues.remoteLoggingLevel, gatewayValues.remoteLoggingPathToLogs))
    };

    return configuration;
  }

  private gatewayConnectorConfigJSON(gatewayConfiguration) {
    const currentConnectors =  this.gatewayConfigurationGroup.value.connectors;
    for(const connector of currentConnectors){
      if (connector.enabled) {
        const typeConnector = connector.configType;
        if(!Array.isArray(gatewayConfiguration[typeConnector])){
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

  private processCurrentConfiguration(response: Array<AttributeData>) {
    if (response.length > 0) {
      this.connectors.clear();
      const connectors = [];
      const attribute = JSON.parse(window.atob(response[0].value));
      for (const attributeKey of Object.keys(attribute)) {
        const keyValue = attribute[attributeKey];
        if (attributeKey === 'thingsboard') {
          if (keyValue !== null && Object.keys(keyValue).length > 0) {
            this.setConfigGateway(keyValue);
          }
        } if (attributeKey === 'storage') {
          if (keyValue !== null && Object.keys(keyValue).length > 0) {
            this.setConfigGateway(keyValue);
          }
        } else {
          for (const connector of Object.keys(keyValue)) {
            let name = 'No name';
            if (Object.prototype.hasOwnProperty.call(keyValue[connector], 'name')) {
              name = keyValue[connector].name ;
            }
            const connectorForm = this.fb.group({
              enabled: [true],
              configType: [attributeKey, [Validators.required]],
              name: [name, [Validators.required]],
              config: [keyValue[connector].config, [Validators.nullValidator, ValidateJSON]]
            });
            this.connectors.push(connectorForm);
          }
        }
      }
      this.connectors.patchValue(connectors);
    }
  }

  private setConfigGateway(keyValue: GatewaySetting) {
    if (Object.prototype.hasOwnProperty.call(keyValue, 'thingsboard')) {
      const formSetting: any = {};
      formSetting.host = keyValue.thingsboard.host;
      formSetting.port = keyValue.thingsboard.port;
      formSetting.remoteConfiguration = keyValue.thingsboard.remoteConfiguration;
      if (Object.prototype.hasOwnProperty.call(keyValue.thingsboard.security, SecurityType.accessToken)) {
        formSetting.securityType = 'accessToken';
        formSetting.accessToken = (keyValue.thingsboard.security as SecurityToken).accessToken;
      } else {
        formSetting.securityType = 'tls';
        formSetting.caCertPath = (keyValue.thingsboard.security as SecurityCertificate).caCert;
        formSetting.privateKeyPath = (keyValue.thingsboard.security as SecurityCertificate).privateKey;
        formSetting.certPath = (keyValue.thingsboard.security as SecurityCertificate).cert;
      }
      this.gatewayConfigurationGroup.patchValue(formSetting);
    }

    if (Object.prototype.hasOwnProperty.call(keyValue, 'storage') && Object.prototype.hasOwnProperty.call(keyValue.storage, 'type')) {
      const formSetting: any = {};
      if (keyValue.storage.type === 'memory') {
        formSetting.storageType = storageType.memoryStorage;
        formSetting.readRecordsCount = (keyValue.storage as GatewaySettingStorageMemory).read_records_count;
        formSetting.maxRecordsCount = (keyValue.storage as GatewaySettingStorageMemory).max_records_count;
      } else if (keyValue.storage.type === 'file') {
        formSetting.storageType = storageType.fileStorage;
        formSetting.dataFolderPath = (keyValue.storage as GatewaySettingStorageFile).data_folder_path;
        formSetting.maxFilesCount = (keyValue.storage as GatewaySettingStorageFile).max_file_count;
        formSetting.readRecordsCount = (keyValue.storage as GatewaySettingStorageFile).max_read_records_count;
        formSetting.maxRecordsCount = (keyValue.storage as GatewaySettingStorageFile).max_read_records_count;
      }
      this.gatewayConfigurationGroup.patchValue(formSetting);
    }
  }

  private processConfigurationDrafts(response: Array<AttributeData>) {
    if (response.length > 0) {
      const attribute = JSON.parse(window.atob(response[0].value));
      for (const key of Object.keys(attribute)) {
        const connector = this.fb.group({
          enabled: [false],
          configType: [attribute[key].connector, [Validators.required]],
          name: [key, [Validators.required]],
          config: [attribute[key].config, [Validators.nullValidator, ValidateJSON]]
        });
        this.connectors.push(connector)
      }
    }
  }

  private processLoggingLevel(value: Array<AttributeData>) {
    let logsLevel = gatewayLogLevel.debug;
    if (value.length > 0 && gatewayLogLevel[value[0].value.toLowerCase()]) {
      logsLevel = gatewayLogLevel[value[0].value.toLowerCase()];
    }
    this.gatewayConfigurationGroup.get('remoteLoggingLevel').patchValue(logsLevel);
  }
}
