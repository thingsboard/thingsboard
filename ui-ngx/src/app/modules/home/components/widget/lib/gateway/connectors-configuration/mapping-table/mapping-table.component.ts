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
  inject,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { PageLink } from '@shared/models/page/page-link';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, take, takeUntil } from 'rxjs/operators';
import {
  ControlContainer,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
} from '@angular/forms';
import {
  ConnectorMapping, ConverterConnectorMapping,
  ConvertorTypeTranslationsMap, DeviceConnectorMapping,
  MappingInfo,
  MappingType,
  MappingTypeTranslationsMap, MappingValue, RequestMappingData,
  RequestType,
  RequestTypesTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { MappingDialogComponent } from '@home/components/widget/lib/gateway/dialog/mapping-dialog.component';
import { isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { validateArrayIsNotEmpty } from '@shared/validators/form-array.validators';

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
    }
  ]
})
export class MappingTableComponent implements ControlValueAccessor, AfterViewInit, OnInit, OnDestroy {
  @Input() controlKey = 'dataMapping';

  @coerceBoolean()
  @Input() required = false;

  parentContainer = inject(ControlContainer);
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

  get mappingType(): MappingType {
    return this.mappingTypeValue;
  }

  get parentFormGroup(): FormGroup {
    return this.parentContainer.control as FormGroup;
  }

  @Input()
  set mappingType(value: MappingType) {
    if (this.mappingTypeValue !== value) {
      this.mappingTypeValue = value;
    }
  }

  @ViewChild('searchInput') searchInputField: ElementRef;

  mappingFormGroup: UntypedFormArray;
  textSearch = this.fb.control('', {nonNullable: true});

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
    });
    this.addSelfControl();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.removeSelfControl();
  }

  ngAfterViewInit(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (prev ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe((text) => {
      const searchText = text.trim();
      this.updateTableData(this.mappingFormGroup.value, searchText.trim())
    });
  }

  registerOnChange(fn: any): void {}

  registerOnTouched(fn: any): void {}

  writeValue(obj: any): void {}

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
    this.dialog.open<MappingDialogComponent, MappingInfo, MappingValue>(MappingDialogComponent, {
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
            this.mappingFormGroup.push(this.fb.group(res));
          }
          this.mappingFormGroup.markAsDirty();
        }
    });
  }

  updateTableData(value: ConnectorMapping[], textSearch?: string): void {
    let tableValue =
      value.map((value: ConnectorMapping) => this.getMappingValue(value));
    if (textSearch) {
      tableValue = tableValue.filter(value =>
        Object.values(value).some(val =>
          val.toString().toLowerCase().includes(textSearch.toLowerCase())
        )
      );
    }
    this.dataSource.loadMappings(tableValue);
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

  private getMappingValue(value: ConnectorMapping): MappingValue {
    switch (this.mappingType) {
      case MappingType.DATA:
        return {
          topicFilter: (value as ConverterConnectorMapping).topicFilter,
          QoS: (value as ConverterConnectorMapping).subscriptionQos,
          converter: this.translate.instant(ConvertorTypeTranslationsMap.get((value as ConverterConnectorMapping).converter.type))
        };
      case MappingType.REQUESTS:
        let details;
        if ((value as RequestMappingData).requestType === RequestType.ATTRIBUTE_UPDATE) {
          details = (value as RequestMappingData).requestValue.attributeFilter;
        } else if ((value as RequestMappingData).requestType === RequestType.SERVER_SIDE_RPC) {
          details = (value as RequestMappingData).requestValue.methodFilter;
        } else {
          details = (value as RequestMappingData).requestValue.topicFilter;
        }
        return {
          requestType: (value as RequestMappingData).requestType,
          type: this.translate.instant(RequestTypesTranslationsMap.get((value as RequestMappingData).requestType)),
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

  private addSelfControl(): void {
    this.parentFormGroup.addControl(this.controlKey,  this.mappingFormGroup);
    if (this.required) {
      this.mappingFormGroup.addValidators(validateArrayIsNotEmpty());
    }
  }

  private removeSelfControl(): void {
    this.parentFormGroup.removeControl(this.controlKey);
  }
}

export class MappingDatasource implements DataSource<{[key: string]: any}> {

  private mappingSubject = new BehaviorSubject<Array<{[key: string]: any}>>([]);

  private allMappings: Observable<Array<{[key: string]: any}>>;

  constructor() {}

  connect(collectionViewer: CollectionViewer): Observable<Array<{[key: string]: any}>> {
    return this.mappingSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.mappingSubject.complete();
  }

  loadMappings(mappings: Array<{[key: string]: any}>, pageLink?: PageLink, reload: boolean = false): void {
    if (reload) {
      this.allMappings = null;
    }
    this.mappingSubject.next(mappings);
  }

  isEmpty(): Observable<boolean> {
    return this.mappingSubject.pipe(
      map((mappings) => !mappings.length)
    );
  }

  total(): Observable<number> {
    return this.mappingSubject.pipe(
      map((mappings) => mappings.length)
    );
  }
}
