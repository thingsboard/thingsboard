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

import { Component, ElementRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import {
  CsvColumnParam,
  ImportEntityColumnType,
  importEntityColumnTypeTranslations,
  importEntityObjectColumns
} from '@home/components/import-export/import-export.models';
import { BehaviorSubject, Observable } from 'rxjs';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';

@Component({
  selector: 'tb-table-columns-assignment',
  templateUrl: './table-columns-assignment.component.html',
  styleUrls: ['./table-columns-assignment.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TableColumnsAssignmentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TableColumnsAssignmentComponent),
      multi: true,
    }
  ]
})
export class TableColumnsAssignmentComponent implements OnInit, ControlValueAccessor, Validator {

  @Input() entityType: EntityType;

  @Input() disabled: boolean;

  dataSource = new CsvColumnsDatasource();

  displayedColumns = ['order', 'sampleData', 'type', 'key'];

  columnTypes: AssignmentColumnType[] = [];

  columnDeviceCredentials: AssignmentColumnType[] = [];

  columnTypesTranslations = importEntityColumnTypeTranslations;

  readonly entityTypeDevice = EntityType.DEVICE;

  private columns: CsvColumnParam[];

  private valid = true;

  private propagateChangePending = false;
  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>) {
  }

  ngOnInit(): void {
    this.columnTypes.push(
      { value: ImportEntityColumnType.name },
      { value: ImportEntityColumnType.type },
      { value: ImportEntityColumnType.label },
      { value: ImportEntityColumnType.description },
    );
    switch (this.entityType) {
      case EntityType.DEVICE:
        this.columnTypes.push(
          { value: ImportEntityColumnType.sharedAttribute },
          { value: ImportEntityColumnType.serverAttribute },
          { value: ImportEntityColumnType.timeseries },
          { value: ImportEntityColumnType.isGateway }
        );
        this.columnDeviceCredentials.push(
          { value: ImportEntityColumnType.accessToken },
          { value: ImportEntityColumnType.x509 },
          { value: ImportEntityColumnType.mqttClientId },
          { value: ImportEntityColumnType.mqttUserName },
          { value: ImportEntityColumnType.mqttPassword },
          { value: ImportEntityColumnType.lwm2mClientEndpoint },
          { value: ImportEntityColumnType.lwm2mClientSecurityConfigMode },
          { value: ImportEntityColumnType.lwm2mClientIdentity },
          { value: ImportEntityColumnType.lwm2mClientKey },
          { value: ImportEntityColumnType.lwm2mClientCert },
          { value: ImportEntityColumnType.lwm2mBootstrapServerSecurityMode },
          { value: ImportEntityColumnType.lwm2mBootstrapServerClientPublicKeyOrId },
          { value: ImportEntityColumnType.lwm2mBootstrapServerClientSecretKey },
          { value: ImportEntityColumnType.lwm2mServerSecurityMode },
          { value: ImportEntityColumnType.lwm2mServerClientPublicKeyOrId },
          { value: ImportEntityColumnType.lwm2mServerClientSecretKey },
        );
        break;
      case EntityType.ASSET:
        this.columnTypes.push(
          { value: ImportEntityColumnType.serverAttribute },
          { value: ImportEntityColumnType.timeseries }
        );
        break;
      case EntityType.EDGE:
        this.columnTypes.push(
          { value: ImportEntityColumnType.edgeLicenseKey },
          { value: ImportEntityColumnType.cloudEndpoint },
          { value: ImportEntityColumnType.routingKey },
          { value: ImportEntityColumnType.secret }
        );
        break;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChange(this.columns);
      this.propagateChangePending = false;
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  columnsUpdated() {
    const isSelectName = this.columns.findIndex((column) => column.type === ImportEntityColumnType.name) > -1;
    const isSelectType = this.columns.findIndex((column) => column.type === ImportEntityColumnType.type) > -1;
    const isSelectLabel = this.columns.findIndex((column) => column.type === ImportEntityColumnType.label) > -1;
    const isSelectDescription = this.columns.findIndex((column) => column.type === ImportEntityColumnType.description) > -1;
    const isSelectEdgeLicenseKey = this.columns.findIndex((column) => column.type === ImportEntityColumnType.edgeLicenseKey) > -1;
    const isSelectCloudEndpoint = this.columns.findIndex((column) => column.type === ImportEntityColumnType.cloudEndpoint) > -1;
    const isSelectRoutingKey = this.columns.findIndex((column) => column.type === ImportEntityColumnType.routingKey) > -1;
    const isSelectSecret = this.columns.findIndex((column) => column.type === ImportEntityColumnType.secret) > -1;
    const hasInvalidColumn = this.columns.findIndex((column) => !this.columnValid(column)) > -1;

    this.valid = isSelectName && isSelectType && !hasInvalidColumn;

    this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.name).disabled = isSelectName;
    this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.type).disabled = isSelectType;
    this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.label).disabled = isSelectLabel;
    this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.description).disabled = isSelectDescription;

    if (this.entityType === EntityType.DEVICE) {
      const isSelectGateway = this.columns.findIndex((column) => column.type === ImportEntityColumnType.isGateway) > -1;

      const isGatewayColumnType = this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.isGateway);
      if (isGatewayColumnType) {
        isGatewayColumnType.disabled = isSelectGateway;
      }

      this.columnDeviceCredentials.forEach((columnCredential) => {
        columnCredential.disabled = this.columns.findIndex(column => column.type === columnCredential.value) > -1;
      });
    }

    const edgeLicenseKeyColumnType = this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.edgeLicenseKey);
    if (edgeLicenseKeyColumnType) {
      edgeLicenseKeyColumnType.disabled = isSelectEdgeLicenseKey;
    }
    const cloudEndpointColumnType = this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.cloudEndpoint);
    if (cloudEndpointColumnType) {
      cloudEndpointColumnType.disabled = isSelectCloudEndpoint;
    }
    const routingKeyColumnType = this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.routingKey);
    if (routingKeyColumnType) {
      routingKeyColumnType.disabled = isSelectRoutingKey;
    }
    const secretColumnType = this.columnTypes.find((columnType) => columnType.value === ImportEntityColumnType.secret);
    if (secretColumnType) {
      secretColumnType.disabled = isSelectSecret;
    }
    if (this.propagateChange) {
      this.propagateChange(this.columns);
    } else {
      this.propagateChangePending = true;
    }
  }

  public isColumnTypeDiffers(columnType: ImportEntityColumnType): boolean {
    return columnType === ImportEntityColumnType.clientAttribute ||
      columnType === ImportEntityColumnType.sharedAttribute ||
      columnType === ImportEntityColumnType.serverAttribute ||
      columnType === ImportEntityColumnType.timeseries;
  }

  private columnValid(column: CsvColumnParam): boolean {
    if (!importEntityObjectColumns.includes(column.type)) {
      return column.key && column.key.trim().length > 0;
    } else {
      return true;
    }
  }

  public validate(c: FormControl) {
    return (this.valid) ? null : {
      columnsInvalid: true
    };
  }

  writeValue(value: CsvColumnParam[]): void {
    this.columns = value;
    this.dataSource.setColumns(this.columns);
    this.columnsUpdated();
  }
}

interface AssignmentColumnType {
  value: ImportEntityColumnType;
  disabled?: boolean;
}

class CsvColumnsDatasource implements DataSource<CsvColumnParam> {

  private columnsSubject = new BehaviorSubject<CsvColumnParam[]>([]);

  constructor() {}

  connect(collectionViewer: CollectionViewer): Observable<CsvColumnParam[] | ReadonlyArray<CsvColumnParam>> {
    return this.columnsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.columnsSubject.complete();
  }

  setColumns(columns: CsvColumnParam[]) {
    this.columnsSubject.next(columns);
  }

}
