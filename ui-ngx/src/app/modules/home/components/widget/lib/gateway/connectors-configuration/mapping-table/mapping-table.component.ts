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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, take, takeUntil } from 'rxjs/operators';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  AttributeUpdate,
  ConnectorMapping,
  ConnectRequest,
  ConverterConnectorMapping,
  ConvertorTypeTranslationsMap,
  DeviceConnectorMapping,
  DisconnectRequest,
  MappingInfo,
  MappingType,
  MappingTypeTranslationsMap,
  MappingValue,
  RequestMappingValue,
  RequestType,
  RequestTypesTranslationsMap,
  ServerSideRpc
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { MappingDialogComponent } from '@home/components/widget/lib/gateway/dialog/mapping-dialog.component';
import { isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';

@Component({
  selector: 'tb-mapping-table',
  templateUrl: './mapping-table.component.html',
  styleUrls: ['./mapping-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MappingTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MappingTableComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [CommonModule, SharedModule]
})
export class MappingTableComponent implements ControlValueAccessor, Validator, AfterViewInit, OnInit, OnDestroy {

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  set mappingType(value: MappingType) {
    if (this.mappingTypeValue !== value) {
      this.mappingTypeValue = value;
    }
  }

  get mappingType(): MappingType {
    return this.mappingTypeValue;
  }

  @ViewChild('searchInput') searchInputField: ElementRef;

  mappingTypeTranslationsMap = MappingTypeTranslationsMap;
  mappingTypeEnum = MappingType;
  displayedColumns = [];
  mappingColumns = [];
  textSearchMode = false;
  dataSource: MappingDatasource;
  hidePageSize = false;
  activeValue = false;
  dirtyValue = false;
  mappingTypeValue: MappingType;
  mappingFormGroup: UntypedFormArray;
  textSearch = this.fb.control('', {nonNullable: true});

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void  = () => {};

  private destroy$ = new Subject<void>();

  constructor(public translate: TranslateService,
              public dialog: MatDialog,
              private dialogService: DialogService,
              private fb: FormBuilder) {
    this.mappingFormGroup = this.fb.array([]);
    this.dirtyValue = !this.activeValue;
    this.dataSource = new MappingDatasource();
  }

  ngOnInit(): void {
    this.setMappingColumns();
    this.displayedColumns.push(...this.mappingColumns.map(column => column.def), 'actions');
    this.mappingFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateTableData(value);
      this.onChange(value);
      this.onTouched();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (prev ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe((text) => {
      const searchText = text.trim();
      this.updateTableData(this.mappingFormGroup.value, searchText.trim());
    });
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(connectorMappings: ConnectorMapping[]): void {
    this.mappingFormGroup.clear();
    this.pushDataAsFormArrays(connectorMappings);
  }

  validate(): ValidationErrors | null {
    return !this.required || this.mappingFormGroup.controls.length ? null : {
      mappingFormGroup: {valid: false}
    };
  }

  enterFilterMode(): void {
    this.textSearchMode = true;
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode(): void {
    this.updateTableData(this.mappingFormGroup.value);
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  manageMapping($event: Event, index?: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    const value = isDefinedAndNotNull(index) ? this.mappingFormGroup.at(index).value : {};
    this.dialog.open<MappingDialogComponent, MappingInfo, ConnectorMapping>(MappingDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        mappingType: this.mappingType,
        value,
        buttonTitle: isUndefinedOrNull(index) ?  'action.add' : 'action.apply'
      }
    }).afterClosed()
      .pipe(take(1), takeUntil(this.destroy$))
      .subscribe(res => {
        if (res) {
          if (isDefinedAndNotNull(index)) {
            this.mappingFormGroup.at(index).patchValue(res);
          } else {
            this.pushDataAsFormArrays([res]);
          }
          this.mappingFormGroup.markAsDirty();
        }
    });
  }

  private updateTableData(value: ConnectorMapping[], textSearch?: string): void {
    let tableValue = value.map(mappingValue => this.getMappingValue(mappingValue));
    if (textSearch) {
      tableValue = tableValue.filter(mappingValue =>
        Object.values(mappingValue).some(val =>
          val.toString().toLowerCase().includes(textSearch.toLowerCase())
        )
      );
    }
    this.dataSource.loadData(tableValue);
  }

  deleteMapping($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('gateway.delete-mapping-title'),
      '',
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.mappingFormGroup.removeAt(index);
        this.mappingFormGroup.markAsDirty();
      }
    });
  }

  private pushDataAsFormArrays(data: ConnectorMapping[]): void {
    if (data?.length) {
      data.forEach((mapping: ConnectorMapping) => this.mappingFormGroup.push(this.fb.control(mapping)));
    }
  }

  private getMappingValue(value: ConnectorMapping): MappingValue {
    switch (this.mappingType) {
      case MappingType.DATA:
        const converterType = ConvertorTypeTranslationsMap.get((value as ConverterConnectorMapping).converter?.type);
        return {
          topicFilter: (value as ConverterConnectorMapping).topicFilter,
          QoS: (value as ConverterConnectorMapping).subscriptionQos,
          converter: converterType ? this.translate.instant(converterType) : ''
        };
      case MappingType.REQUESTS:
        let details: string;
        const requestValue = value as RequestMappingValue;
        if (requestValue.requestType === RequestType.ATTRIBUTE_UPDATE) {
          details = (requestValue.requestValue as AttributeUpdate).attributeFilter;
        } else if (requestValue.requestType === RequestType.SERVER_SIDE_RPC) {
          details = (requestValue.requestValue as ServerSideRpc).methodFilter;
        } else {
          details = (requestValue.requestValue as ConnectRequest | DisconnectRequest).topicFilter;
        }
        return {
          requestType: (value as RequestMappingValue).requestType,
          type: this.translate.instant(RequestTypesTranslationsMap.get((value as RequestMappingValue).requestType)),
          details
        };
      case MappingType.OPCUA:
        const deviceNamePattern = (value as DeviceConnectorMapping).deviceInfo?.deviceNameExpression;
        const deviceProfileExpression = (value as DeviceConnectorMapping).deviceInfo?.deviceProfileExpression;
        const { deviceNodePattern } = value as DeviceConnectorMapping;
        return {
          deviceNodePattern,
          deviceNamePattern,
          deviceProfileExpression
        };
      default:
        return {} as MappingValue;
    }
  }

  private setMappingColumns(): void {
    switch (this.mappingType) {
      case MappingType.DATA:
        this.mappingColumns.push(
          { def: 'topicFilter', title: 'gateway.topic-filter' },
          { def: 'QoS', title: 'gateway.mqtt-qos' },
          { def: 'converter', title: 'gateway.payload-type' }
        );
        break;
      case MappingType.REQUESTS:
        this.mappingColumns.push(
          { def: 'type', title: 'gateway.type' },
          { def: 'details', title: 'gateway.details' }
        );
        break;
      case MappingType.OPCUA:
        this.mappingColumns.push(
          { def: 'deviceNodePattern', title: 'gateway.device-node' },
          { def: 'deviceNamePattern', title: 'gateway.device-name' },
          { def: 'deviceProfileExpression', title: 'gateway.device-profile' }
        );
    }
  }
}

export class MappingDatasource extends TbTableDatasource<MappingValue> {
  constructor() {
    super();
  }
}
