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
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ModbusClientTypeLabelsMap, ModbusMasterConfig, SlaveConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TbDatasource } from '@shared/abstract/datasource/datasource.abstract';
import { ModbusSlaveDialogComponent } from '@home/components/widget/lib/gateway/connectors-configuration/modbus';

@Component({
  selector: 'tb-modbus-master-table',
  templateUrl: './modbus-master-table.component.html',
  styleUrls: ['./modbus-master-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusMasterTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusMasterTableComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [CommonModule, SharedModule]
})
export class ModbusMasterTableComponent implements ControlValueAccessor, Validator, AfterViewInit, OnInit, OnDestroy {

  @ViewChild('searchInput') searchInputField: ElementRef;

  textSearchMode = false;
  dataSource: SlavesDatasource;
  hidePageSize = false;
  activeValue = false;
  dirtyValue = false;
  masterFormGroup: UntypedFormGroup;
  textSearch = this.fb.control('', {nonNullable: true});

  readonly ModbusClientTypeLabelsMap = ModbusClientTypeLabelsMap;

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void  = () => {};

  private destroy$ = new Subject<void>();

  constructor(
    public translate: TranslateService,
    public dialog: MatDialog,
    private dialogService: DialogService,
    private fb: FormBuilder
  ) {
    this.masterFormGroup =  this.fb.group({ slaves: this.fb.array([])});
    this.dirtyValue = !this.activeValue;
    this.dataSource = new SlavesDatasource();
  }

  get slaves(): FormArray {
    return this.masterFormGroup.get('slaves') as FormArray;
  }

  ngOnInit(): void {
    this.masterFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateTableData(value.slaves);
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
      this.updateTableData(this.slaves.value, searchText.trim());
    });
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(master: ModbusMasterConfig): void {
    this.slaves.clear();
    this.pushDataAsFormArrays(master.slaves);
  }

  validate(): ValidationErrors | null {
    return this.slaves.controls.length ? null : {
      slavesFormGroup: {valid: false}
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
    this.updateTableData(this.slaves.value);
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  manageSlave($event: Event, index?: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    const value = isDefinedAndNotNull(index) ? this.slaves.at(index).value : {};
    this.dialog.open<ModbusSlaveDialogComponent, any, any>(ModbusSlaveDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle: isUndefinedOrNull(index) ?  'action.add' : 'action.apply'
      }
    }).afterClosed()
      .pipe(take(1), takeUntil(this.destroy$))
      .subscribe(res => {
        if (res) {
          if (isDefinedAndNotNull(index)) {
            this.slaves.at(index).patchValue(res);
          } else {
            this.slaves.push(this.fb.control(res));
          }
          this.masterFormGroup.markAsDirty();
        }
    });
  }

  deleteMapping($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('gateway.delete-slave-title'),
      '',
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.slaves.removeAt(index);
        this.masterFormGroup.markAsDirty();
      }
    });
  }

  private updateTableData(data: SlaveConfig[], textSearch?: string): void {
    let tableValue = data;
    if (textSearch) {
      tableValue = tableValue.filter(value =>
        Object.values(value).some(val =>
          val.toString().toLowerCase().includes(textSearch.toLowerCase())
        )
      );
    }
    this.dataSource.loadData(tableValue);
  }

  private pushDataAsFormArrays(slaves: SlaveConfig[]): void {
    if (slaves?.length) {
      slaves.forEach((slave: SlaveConfig) => this.slaves.push(this.fb.control(slave)));
    }
  }
}

export class SlavesDatasource extends TbDatasource<SlaveConfig> {
  constructor() {
    super();
  }
}
