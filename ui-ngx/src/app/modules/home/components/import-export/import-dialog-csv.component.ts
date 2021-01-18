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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
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
import { ImportEntitiesResultInfo, ImportEntityData } from '@app/shared/models/entity.models';
import { ImportExportService } from '@home/components/import-export/import-export.service';

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
  implements OnInit {

  @ViewChild('importStepper', {static: true}) importStepper: MatVerticalStepper;

  entityType: EntityType;
  importTitle: string;
  importFileLabel: string;

  delimiters: {key: string, value: string}[] = [{
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

  private parseData: CsvToJsonResult;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ImportDialogCsvData,
              public dialogRef: MatDialogRef<ImportDialogCsvComponent, boolean>,
              public translate: TranslateService,
              private importExport: ImportExportService,
              private fb: FormBuilder) {
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

  ngOnInit(): void {
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
          {message: this.translate.instant(messageId, params),
            type: 'error'}));
      }
    );
  }

  private createColumnsData(): CsvColumnParam[] {
    const columnsParam: CsvColumnParam[] = [];
    const isHeader: boolean = this.importParametersFormGroup.get('isHeader').value;
    for (let i = 0; i < this.parseData.headers.length; i++) {
      let columnParam: CsvColumnParam;
      if (isHeader && this.parseData.headers[i].search(/^(name|type|label)$/im) === 0) {
        columnParam = {
          type: ImportEntityColumnType[this.parseData.headers[i].toLowerCase()],
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
    const parameterColumns: CsvColumnParam[] = this.columnTypesFormGroup.get('columnsParam').value;
    const entitiesData: ImportEntityData[] = [];
    let sentDataLength = 0;
    for (let row = 0; row < importData.rows.length; row++) {
      const entityData: ImportEntityData = {
        name: '',
        type: '',
        description: '',
        gateway: null,
        label: '',
        accessToken: '',
        attributes: {
          server: [],
          shared: []
        },
        timeseries: [],
        edgeLicenseKey: '',
        cloudEndpoint: '',
        routingKey: '',
        secret: ''
      };
      const i = row;
      for (let j = 0; j < parameterColumns.length; j++) {
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
          case ImportEntityColumnType.accessToken:
            entityData.accessToken = importData.rows[i][j];
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
          case ImportEntityColumnType.edgeLicenseKey:
            entityData.edgeLicenseKey = importData.rows[i][j];
            break;
          case ImportEntityColumnType.cloudEndpoint:
            entityData.cloudEndpoint = importData.rows[i][j];
            break;
          case ImportEntityColumnType.routingKey:
            entityData.routingKey = importData.rows[i][j];
            break;
          case ImportEntityColumnType.secret:
            entityData.secret = importData.rows[i][j];
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

}
