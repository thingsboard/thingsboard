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
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, take, takeUntil } from 'rxjs/operators';
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
  ConnectorMapping,
  DevicesConfigMapping,
  MappingInfo,
  MappingType,
  MappingValue
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { DataSource } from '@angular/cdk/collections';
import {
  DeviceDialogComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/socket/device-dialog/device-dialog.component';
import { isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tb-devices-config',
  templateUrl: './devices-config.component.html',
  styleUrls: ['./devices-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DevicesConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DevicesConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [CommonModule, SharedModule]
})
export class DevicesConfigComponent implements ControlValueAccessor, Validator, AfterViewInit, OnInit, OnDestroy {

  @Input()
  @coerceBoolean()
  required = false;

  get mappingType(): MappingType {
    return this.mappingTypeValue;
  }

  @ViewChild('searchInput')
  searchInputField: ElementRef;
  displayedColumns = [];
  deviceConfigColumns = [];
  textSearchMode = false;
  dataSource: DevicesConfigDatasource;
  activeValue = false;
  dirtyValue = false;
  mappingTypeValue: MappingType;
  deviceFormGroup: UntypedFormArray;
  textSearch = this.fb.control('', {nonNullable: true});

  private onChange: (value: string) => void = () => {
  };
  private onTouched: () => void = () => {
  };

  private destroy$ = new Subject<void>();

  constructor(public translate: TranslateService,
              public dialog: MatDialog,
              private dialogService: DialogService,
              private fb: FormBuilder) {
    this.deviceFormGroup = this.fb.array([]);
    this.dirtyValue = !this.activeValue;
    this.dataSource = new DevicesConfigDatasource();
  }

  ngOnInit(): void {
    this.setDeviceColumns();
    this.displayedColumns.push(...this.deviceConfigColumns.map(column => column.def), 'actions');
    this.deviceFormGroup.valueChanges.pipe(
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
      this.updateTableData(this.deviceFormGroup.value, searchText.trim());
    });
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(connectorMappings: ConnectorMapping[]): void {
    this.deviceFormGroup.clear();
    this.pushDataAsFormArrays(connectorMappings);
  }

  validate(): ValidationErrors | null {
    return !this.required || this.deviceFormGroup.controls.length ? null : {
      deviceFormGroup: {valid: false}
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
    this.updateTableData(this.deviceFormGroup.value);
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  manageDevices($event: Event, index?: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    const value = isDefinedAndNotNull(index) ? this.deviceFormGroup.at(index).value : {};
    this.dialog.open<DeviceDialogComponent, MappingInfo, MappingValue>(DeviceDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        mappingType: this.mappingType,
        value,
        buttonTitle: isUndefinedOrNull(index) ? 'action.add' : 'action.apply'
      }
    }).afterClosed()
      .pipe(take(1), takeUntil(this.destroy$))
      .subscribe(res => {
        if (res) {
          if (isDefinedAndNotNull(index)) {
            this.deviceFormGroup.at(index).patchValue(res);
          } else {
            this.deviceFormGroup.push(this.fb.group(res));
          }
          this.deviceFormGroup.markAsDirty();
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
    this.dataSource.loadMappings(tableValue);
  }

  deleteDevices($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('gateway.delete-device-title'),
      '',
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.deviceFormGroup.removeAt(index);
        this.deviceFormGroup.markAsDirty();
      }
    });
  }

  private pushDataAsFormArrays(data: ConnectorMapping[]): void {
    if (data?.length) {
      data.forEach((mapping: ConnectorMapping) => this.deviceFormGroup.push(this.fb.control(mapping)));
    }
  }

  private getMappingValue(value: ConnectorMapping): MappingValue {
    return {
      address: (value as DevicesConfigMapping).address,
      deviceName: (value as DevicesConfigMapping).deviceName,
      deviceType: (value as DevicesConfigMapping).deviceType
    };
  }

  private setDeviceColumns(): void {
    this.deviceConfigColumns.push(
      {def: 'address', title: 'gateway.address-filter'},
      {def: 'deviceName', title: 'gateway.device-name'},
      {def: 'deviceType', title: 'gateway.device-profile'}
    );
  }
}

class DevicesConfigDatasource implements DataSource<{ [key: string]: any }> {

  private deviceConfigSubject = new BehaviorSubject<Array<{ [key: string]: any }>>([]);

  constructor() {
  }

  connect(): Observable<Array<{ [key: string]: any }>> {
    return this.deviceConfigSubject.asObservable();
  }

  disconnect(): void {
    this.deviceConfigSubject.complete();
  }

  loadMappings(mappings: Array<{ [key: string]: any }>): void {
    this.deviceConfigSubject.next(mappings);
  }

  isEmpty(): Observable<boolean> {
    return this.deviceConfigSubject.pipe(
      map((mappings) => !mappings.length)
    );
  }

  total(): Observable<number> {
    return this.deviceConfigSubject.pipe(
      map((mappings) => mappings.length)
    );
  }
}
