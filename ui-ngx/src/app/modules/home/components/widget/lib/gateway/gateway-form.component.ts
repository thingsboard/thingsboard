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
  ConnectorType,
  createFormConfig,
  CURRENT_CONFIGURATION_ATTRIBUTE,
  DEFAULT_CONNECTOR,
  gatewayConfigJSON,
  GatewayFormConnectorModel,
  GatewayFormModels,
  GatewayLogLevel,
  generateConnectorConfigFiles,
  generateLogConfigFile,
  generateYAMLConfigFile,
  getDraftConnectorsJSON,
  getEntityId,
  REMOTE_LOGGING_LEVEL_ATTRIBUTE,
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
import { ResizeObserver } from '@juggle/resize-observer';

// @dynamic
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
  private formResize$: ResizeObserver;
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

  @Input()
  isStateForm: boolean;

  isReadOnlyForm = false;
  deviceNameForm: string;

  ngOnInit(): void {
    this.initWidgetSettings(this.ctx.settings);
    if (this.ctx.datasources && this.ctx.datasources.length) {
      this.deviceNameForm = this.ctx.datasources[0].name;
    }

    this.buildForm();
    this.ctx.updateWidgetParams();
    this.formResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.formResize$.observe(this.formContainerRef.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.formResize$) {
      this.formResize$.disconnect();
    }
    this.subscribeGateway$.unsubscribe();
    this.subscribeStorageType$.unsubscribe();
  }

  private initWidgetSettings(settings: WidgetSetting): void {
    let widgetTitle;
    if (settings.gatewayTitle && settings.gatewayTitle.length) {
      widgetTitle = this.utils.customTranslation(settings.gatewayTitle, settings.gatewayTitle);
    } else {
      widgetTitle = this.translate.instant('gateway.gateway');
    }
    this.ctx.widgetTitle = widgetTitle;
    this.isReadOnlyForm = (settings.readOnly) ? settings.readOnly : false;

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

  private updateWidgetDisplaying(): void {
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

  private createConnector(setting: GatewayFormConnectorModel = DEFAULT_CONNECTOR): void {
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
      gateway: [null, []],
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

    if (this.isReadOnlyForm) {
      this.gatewayConfigurationGroup.disable({emitEvent: false});
    }

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
          ...this.getAttributes(gatewayId)]).subscribe(() => {
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
    const gatewayConfiguration: GatewayFormModels = this.gatewayConfigurationGroup.value;
    const filesZip: any = {};
    filesZip['tb_gateway.yaml'] = generateYAMLConfigFile(gatewayConfiguration);
    generateConnectorConfigFiles(filesZip, gatewayConfiguration.connectors);
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

  private createConnectorName(connectors: GatewayFormConnectorModel[], name: string, index: number = 0): string {
    const newKeyName = index ? name + index : name;
    const indexRes = connectors.findIndex((element) => element.name === newKeyName);
    return indexRes === -1 ? newKeyName : this.createConnectorName(connectors, name, ++index);
  }

  private validateConnectorName(connectors: GatewayFormConnectorModel[], name: string, connectorIndex: number, index = 0): string {
    for (let i = 0; i < connectors.length; i++) {
      const nameEq = (index === 0) ? name : name + index;
      if (i !== connectorIndex && connectors[i].name === nameEq) {
        this.validateConnectorName(connectors, name, connectorIndex, ++index);
      }
    }
    return (index === 0) ? name : name + index;
  }

  changeConnectorType(connector: AbstractControl): void {
    if (!connector.get('name').value) {
      const typeConnector = connector.get('configType').value;
      const connectors = this.gatewayConfigurationGroup.value.connectors;
      connector.get('name').patchValue(this.createConnectorName(connectors, ConnectorType[typeConnector]));
    }
  }

  changeConnectorName(connector: AbstractControl, index: number): void {
    const connectors = this.gatewayConfigurationGroup.value.connectors;
    connector.get('name').patchValue(this.validateConnectorName(connectors, connector.get('name').value, index));
  }

  save(): void {
    const gatewayConfiguration: GatewayFormModels = this.gatewayConfigurationGroup.value;
    forkJoin([
      this.saveAttribute(
        CONFIGURATION_ATTRIBUTE,
        window.btoa(JSON.stringify(gatewayConfigJSON(gatewayConfiguration))),
        AttributeScope.SHARED_SCOPE),
      this.saveAttribute(
        CONFIGURATION_DRAFT_ATTRIBUTE,
        window.btoa(JSON.stringify(getDraftConnectorsJSON(gatewayConfiguration.connectors))),
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
          this.setFormGatewaySettings(currentConfig);
          this.setFormConnectorsDraft(draftConfig);
          if (this.isReadOnlyForm) {
            this.gatewayConfigurationGroup.disable({emitEvent: false});
          }
        })
      )
    );
    tasks.push(this.getAttribute(REMOTE_LOGGING_LEVEL_ATTRIBUTE, AttributeScope.SHARED_SCOPE, gatewayId).pipe(
      tap(logsLevel => this.processLoggingLevel(logsLevel))
    ));
    return tasks;
  }

  private getAttribute(attributeName: string, attributeScope: AttributeScope, gatewayId: string): Observable<Array<AttributeData>> {
    return this.attributeService.getEntityAttributes(getEntityId(gatewayId), attributeScope, [attributeName]);
  }

  private setFormGatewaySettings(response: Array<AttributeData>): void {
    this.connectors.clear();
    if (response.length > 0) {
      const attribute = JSON.parse(window.atob(response[0].value));
      for (const attributeKey of Object.keys(attribute)) {
        const keyValue = attribute[attributeKey];
        if (attributeKey === 'thingsboard') {
          if (keyValue !== null && Object.keys(keyValue).length > 0) {
            this.gatewayConfigurationGroup.patchValue(createFormConfig(keyValue));
          }
        } else {
          for (const connector of Object.keys(keyValue)) {
            let name = 'No name';
            if (Object.prototype.hasOwnProperty.call(keyValue[connector], 'name')) {
              name = keyValue[connector].name;
            }
            const newConnector: GatewayFormConnectorModel = {
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

  private setFormConnectorsDraft(response: Array<AttributeData>): void {
    if (response.length > 0) {
      const attribute = JSON.parse(window.atob(response[0].value));
      for (const connectorName of Object.keys(attribute)) {
        const newConnector: GatewayFormConnectorModel = {
          enabled: false,
          configType: (attribute[connectorName].connector as ConnectorType),
          config: attribute[connectorName].config,
          name: connectorName
        };
        this.createConnector(newConnector);
      }
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
