///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Inject, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatVerticalStepper } from '@angular/material/stepper';
import {
  convertCSVToJson,
  CsvColumnParam,
  CsvToJsonConfig,
  CsvToJsonResult,
  ImportEntityColumnType
} from '@home/components/import-export/import-export.models';
import { EdgeImportEntityData, ImportEntitiesResultInfo, ImportEntityData } from '@app/shared/models/entity.models';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { TableColumnsAssignmentComponent } from '@home/components/import-export/table-columns-assignment.component';
import { getDeviceCredentialMQTTDefault } from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { getLwm2mSecurityConfigModelsDefault } from '@shared/models/lwm2m-security-config.models';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';

export interface ImportDialogCsvData {
  entityType: EntityType;
  importTitle: string;
  importFileLabel: string;
}

@Component({
  selector: 'tb-import-csv-dialog',
  templateUrl: './import-dialog-csv.component.html',
  providers: [],
  styleUrls: ['./import-dialog-csv.component.scss']
})
export class ImportDialogCsvComponent extends DialogComponent<ImportDialogCsvComponent, boolean>
  implements AfterViewInit {

  @ViewChild('importStepper', {static: true}) importStepper: MatVerticalStepper;

  @ViewChild('columnsAssignmentComponent', {static: true})
  columnsAssignmentComponent: TableColumnsAssignmentComponent;

  @ViewChild('failureDetailsEditor')
  failureDetailsEditorElmRef: ElementRef;

  entityType: EntityType;
  importTitle: string;
  importFileLabel: string;

  delimiters: { key: string, value: string }[] = [{
    key: ',',
    value: ','
  }, {
    key: ';',
    value: ';'
  }, {
    key: '|',
    value: '|'
  }, {
    key: '\t',
    value: 'Tab'
  }];

  selectedIndex = 0;

  selectFileFormGroup: FormGroup;
  importParametersFormGroup: FormGroup;
  columnTypesFormGroup: FormGroup;

  isImportData = false;
  progressCreate = 0;
  statistical: ImportEntitiesResultInfo;

  private allowAssignColumn: ImportEntityColumnType[];
  private initEditorComponent = false;
  private parseData: CsvToJsonResult;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ImportDialogCsvData,
              public dialogRef: MatDialogRef<ImportDialogCsvComponent, boolean>,
              public translate: TranslateService,
              private importExport: ImportExportService,
              private fb: FormBuilder,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
    this.entityType = data.entityType;
    this.importTitle = data.importTitle;
    this.importFileLabel = data.importFileLabel;

    this.selectFileFormGroup = this.fb.group(
      {
        importData: [null, [Validators.required]]
      }
    );
    this.importParametersFormGroup = this.fb.group({
      delim: [',', [Validators.required]],
      isHeader: [true, []],
      isUpdate: [true, []],
    });
    this.columnTypesFormGroup = this.fb.group({
      columnsParam: [[], []]
    });
  }

  ngAfterViewInit() {
    let columns = this.columnsAssignmentComponent.columnTypes;
    if (this.entityType === EntityType.DEVICE) {
      columns = columns.concat(this.columnsAssignmentComponent.columnDeviceCredentials);
    }
    this.allowAssignColumn = columns.map(column => column.value);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  previousStep() {
    this.importStepper.previous();
  }

  nextStep(step: number) {
    switch (step) {
      case 2:
        this.importStepper.next();
        break;
      case 3:
        const importData: string = this.selectFileFormGroup.get('importData').value;
        const parseData = this.parseCSV(importData);
        if (parseData === -1) {
          this.importStepper.previous();
          this.importStepper.selected.reset();
        } else {
          this.parseData = parseData as CsvToJsonResult;
          const columnsParam = this.createColumnsData();
          this.columnTypesFormGroup.patchValue({columnsParam}, {emitEvent: true});
          this.importStepper.next();
        }
        break;
      case 4:
        this.importStepper.next();
        this.isImportData = true;
        this.addEntities();
        break;
      case 6:
        this.dialogRef.close(true);
        break;
    }
  }

  private parseCSV(importData: string): CsvToJsonResult | number {
    const config: CsvToJsonConfig = {
      delim: this.importParametersFormGroup.get('delim').value,
      header: this.importParametersFormGroup.get('isHeader').value
    };
    return convertCSVToJson(importData, config,
      (messageId, params) => {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.translate.instant(messageId, params),
            type: 'error'
          }));
      }
    );
  }

  private createColumnsData(): CsvColumnParam[] {
    const columnsParam: CsvColumnParam[] = [];
    const isHeader: boolean = this.importParametersFormGroup.get('isHeader').value;
    for (let i = 0; i < this.parseData.headers.length; i++) {
      let columnParam: CsvColumnParam;
      let findEntityColumnType: ImportEntityColumnType;
      if (isHeader) {
        const headerColumnName = this.parseData.headers[i].toUpperCase();
        findEntityColumnType = this.allowAssignColumn.find(column => column === headerColumnName);
      }
      if (isHeader && findEntityColumnType) {
        columnParam = {
          type: findEntityColumnType,
          key: this.parseData.headers[i].toLowerCase(),
          sampleData: this.parseData.rows[0][i]
        };
      } else {
        columnParam = {
          type: ImportEntityColumnType.serverAttribute,
          key: isHeader ? this.parseData.headers[i] : '',
          sampleData: this.parseData.rows[0][i]
        };
      }
      columnsParam.push(columnParam);
    }
    return columnsParam;
  }


  private addEntities() {
    const importData = this.parseData;
    const isHeader: boolean = this.importParametersFormGroup.get('isHeader').value;
    const parameterColumns: CsvColumnParam[] = this.columnTypesFormGroup.get('columnsParam').value;
    const entitiesData: ImportEntityData[] = [];
    let sentDataLength = 0;
    const startLineNumber = isHeader ? 2 : 1;
    for (let row = 0; row < importData.rows.length; row++) {
      const entityData: ImportEntityData = this.constructDraftImportEntityData();
      const i = row;
      entityData.lineNumber = startLineNumber + i;
      for (let j = 0; j < parameterColumns.length; j++) {
        if (!isDefinedAndNotNull(importData.rows[i][j]) || importData.rows[i][j] === '') {
          continue;
        }
        switch (parameterColumns[j].type) {
          case ImportEntityColumnType.serverAttribute:
            entityData.attributes.server.push({
              key: parameterColumns[j].key,
              value: importData.rows[i][j]
            });
            break;
          case ImportEntityColumnType.timeseries:
            entityData.timeseries.push({
              key: parameterColumns[j].key,
              value: importData.rows[i][j]
            });
            break;
          case ImportEntityColumnType.sharedAttribute:
            entityData.attributes.shared.push({
              key: parameterColumns[j].key,
              value: importData.rows[i][j]
            });
            break;
          case ImportEntityColumnType.name:
            entityData.name = importData.rows[i][j];
            break;
          case ImportEntityColumnType.type:
            entityData.type = importData.rows[i][j];
            break;
          case ImportEntityColumnType.label:
            entityData.label = importData.rows[i][j];
            break;
          case ImportEntityColumnType.isGateway:
            entityData.gateway = importData.rows[i][j];
            break;
          case ImportEntityColumnType.description:
            entityData.description = importData.rows[i][j];
            break;
          case ImportEntityColumnType.accessToken:
            entityData.credential.accessToken = importData.rows[i][j];
            break;
          case ImportEntityColumnType.x509:
            entityData.credential.x509 = importData.rows[i][j];
            break;
          case ImportEntityColumnType.mqttClientId:
            if (!entityData.credential.mqtt) {
              entityData.credential.mqtt = getDeviceCredentialMQTTDefault();
            }
            entityData.credential.mqtt.clientId = importData.rows[i][j];
            break;
          case ImportEntityColumnType.mqttUserName:
            if (!entityData.credential.mqtt) {
              entityData.credential.mqtt = getDeviceCredentialMQTTDefault();
            }
            entityData.credential.mqtt.userName = importData.rows[i][j];
            break;
          case ImportEntityColumnType.mqttPassword:
            if (!entityData.credential.mqtt) {
              entityData.credential.mqtt = getDeviceCredentialMQTTDefault();
            }
            entityData.credential.mqtt.password = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mClientEndpoint:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.client.endpoint = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mClientSecurityConfigMode:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.client.securityConfigClientMode = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mClientIdentity:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.client.identity = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mClientKey:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.client.key = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mClientCert:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.client.cert = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mBootstrapServerSecurityMode:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.bootstrap.bootstrapServer.securityMode = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mBootstrapServerClientPublicKeyOrId:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.bootstrap.bootstrapServer.clientPublicKeyOrId = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mBootstrapServerClientSecretKey:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.bootstrap.bootstrapServer.clientSecretKey = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mServerSecurityMode:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.bootstrap.lwm2mServer.securityMode = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mServerClientPublicKeyOrId:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.bootstrap.lwm2mServer.clientPublicKeyOrId = importData.rows[i][j];
            break;
          case ImportEntityColumnType.lwm2mServerClientSecretKey:
            if (!entityData.credential.lwm2m) {
              entityData.credential.lwm2m = getLwm2mSecurityConfigModelsDefault();
            }
            entityData.credential.lwm2m.bootstrap.lwm2mServer.clientSecretKey = importData.rows[i][j];
            break;
          case ImportEntityColumnType.edgeLicenseKey:
            (entityData as EdgeImportEntityData).edgeLicenseKey = importData.rows[i][j];
            break;
          case ImportEntityColumnType.cloudEndpoint:
            (entityData as EdgeImportEntityData).cloudEndpoint = importData.rows[i][j];
            break;
          case ImportEntityColumnType.routingKey:
            (entityData as EdgeImportEntityData).routingKey = importData.rows[i][j];
            break;
          case ImportEntityColumnType.secret:
            (entityData as EdgeImportEntityData).secret = importData.rows[i][j];
            break;
        }
      }
      entitiesData.push(entityData);
    }
    const createImportEntityCompleted = () => {
      sentDataLength++;
      this.progressCreate = Math.round((sentDataLength / importData.rows.length) * 100);
    };

    const isUpdate: boolean = this.importParametersFormGroup.get('isUpdate').value;

    this.importExport.importEntities(entitiesData, this.entityType, isUpdate,
      createImportEntityCompleted, {ignoreErrors: true, resendRequest: true}).subscribe(
      (result) => {
        this.statistical = result;
        this.isImportData = false;
        this.importStepper.next();
      }
    );
  }

  private constructDraftImportEntityData(): ImportEntityData {
    const entityData: ImportEntityData = {
      lineNumber: 1,
      name: '',
      type: '',
      description: '',
      gateway: null,
      label: '',
      attributes: {
        server: [],
        shared: []
      },
      credential: {},
      timeseries: []
    };
    if (this.entityType === EntityType.EDGE) {
      const edgeEntityData: EdgeImportEntityData = entityData as EdgeImportEntityData;
      edgeEntityData.edgeLicenseKey = '';
      edgeEntityData.cloudEndpoint = '';
      edgeEntityData.routingKey = '';
      edgeEntityData.secret = '';
      return edgeEntityData;
    } else {
      return entityData;
    }
  }

  initEditor() {
    if (!this.initEditorComponent) {
      this.createEditor(this.failureDetailsEditorElmRef, this.statistical.error.errors);
    }
  }

  private createEditor(editorElementRef: ElementRef, content: string): void {
    const editorElement = editorElementRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/java',
      theme: 'ace/theme/github',
      showGutter: false,
      showPrintMargin: false,
      readOnly: true
    };

    const advancedOptions = {
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        const editor = ace.edit(editorElement, editorOptions);
        editor.session.setUseWrapMode(false);
        editor.setValue(content, -1);
        this.updateEditorSize(editorElement, content, editor);
      }
    );
  }

  private updateEditorSize(editorElement: any, content: string, editor: Ace.Editor) {
    let newHeight = 200;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 16 * lines.length + 24;
    }
    const minHeight = Math.min(200, newHeight);
    this.renderer.setStyle(editorElement, 'minHeight', minHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    editor.resize();
  }

}
