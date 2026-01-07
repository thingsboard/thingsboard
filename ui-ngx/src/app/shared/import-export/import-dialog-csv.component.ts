///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatStepper } from '@angular/material/stepper';
import {
  BulkImportRequest,
  BulkImportResult,
  ColumnMapping,
  convertCSVToJson,
  CsvColumnParam,
  CSVDelimiter,
  CsvToJsonConfig,
  CsvToJsonResult,
  ImportEntityColumnType
} from '@shared/import-export/import-export.models';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { TableColumnsAssignmentComponent } from '@shared/import-export/table-columns-assignment.component';
import { Ace } from 'ace-builds';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';

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
  implements AfterViewInit, OnDestroy {

  @ViewChild('importStepper', {static: true}) importStepper: MatStepper;

  @ViewChild('columnsAssignmentComponent', {static: true})
  columnsAssignmentComponent: TableColumnsAssignmentComponent;

  @ViewChild('failureDetailsEditor')
  failureDetailsEditorElmRef: ElementRef;

  entityType: EntityType;
  importTitle: string;
  importFileLabel: string;

  delimiters: { key: CSVDelimiter; value: string }[] = [{
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

  selectFileFormGroup: UntypedFormGroup;
  importParametersFormGroup: UntypedFormGroup;
  columnTypesFormGroup: UntypedFormGroup;

  isImportData = false;
  statistical: BulkImportResult;

  aceEditor: Ace.Editor;

  private allowAssignColumn: ImportEntityColumnType[];
  private initEditorComponent = false;
  private parseData: CsvToJsonResult;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ImportDialogCsvData,
              public dialogRef: MatDialogRef<ImportDialogCsvComponent, boolean>,
              public translate: TranslateService,
              private importExport: ImportExportService,
              private fb: UntypedFormBuilder,
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

  ngOnDestroy(): void {
    if (this.aceEditor) {
      this.aceEditor.destroy();
    }
    super.ngOnDestroy();
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
    const entitiesData: BulkImportRequest = {
      file: this.selectFileFormGroup.get('importData').value,
      mapping: {
        columns: this.processingColumnsParams(),
        delimiter: this.importParametersFormGroup.get('delim').value,
        header: this.importParametersFormGroup.get('isHeader').value,
        update: this.importParametersFormGroup.get('isUpdate').value
      }
    };
    this.importExport.bulkImportEntities(entitiesData, this.entityType, {ignoreErrors: true}).subscribe(
      (result) => {
        this.statistical = result;
        this.isImportData = false;
        this.importStepper.next();
      }
    );
  }

  private processingColumnsParams(): Array<ColumnMapping> {
    const parameterColumns: CsvColumnParam[] = this.columnTypesFormGroup.get('columnsParam').value;
    const allowKeyForTypeColumns: ImportEntityColumnType[] = [
      ImportEntityColumnType.serverAttribute,
      ImportEntityColumnType.timeseries,
      ImportEntityColumnType.sharedAttribute
    ];
    return parameterColumns.map(column => ({
      type: column.type,
      key: allowKeyForTypeColumns.some(type => type === column.type) ? column.key : undefined
    }));
  }

  initEditor() {
    if (!this.initEditorComponent) {
      this.createEditor(this.failureDetailsEditorElmRef, this.statistical.errorsList);
    }
  }

  private createEditor(editorElementRef: ElementRef, contents: string[]): void {
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
    const content = contents.map(error => error.replace('\n', '')).join('\n');
    getAce().subscribe(
      (ace) => {
        this.aceEditor = ace.edit(editorElement, editorOptions);
        this.aceEditor.session.setUseWrapMode(false);
        this.aceEditor.setValue(content, -1);
        updateEditorSize(editorElement, content, this.aceEditor, this.renderer, {setMinHeight: true, ignoreWidth: true});
      }
    );
  }

}
