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
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, take, takeUntil } from 'rxjs/operators';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
} from '@angular/forms';
import {
  LegacySlaveConfig,
  ModbusMasterConfig,
  ModbusProtocolLabelsMap,
  ModbusSlaveInfo,
  ModbusValues,
  SlaveConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { ModbusSlaveDialogComponent } from '../modbus-slave-dialog/modbus-slave-dialog.component';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  ModbusLegacySlaveDialogComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/modbus/modbus-slave-dialog/modbus-legacy-slave-dialog.component';

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
  ],
  standalone: true,
  imports: [CommonModule, SharedModule]
})
export class ModbusMasterTableComponent implements ControlValueAccessor, AfterViewInit, OnInit, OnDestroy {

  @ViewChild('searchInput') searchInputField: ElementRef;

  @coerceBoolean()
  @Input() isLegacy = false;

  textSearchMode = false;
  dataSource: SlavesDatasource;
  masterFormGroup: UntypedFormGroup;
  textSearch = this.fb.control('', {nonNullable: true});

  readonly ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;

  private onChange: (value: ModbusMasterConfig) => void = () => {};
  private onTouched: () => void  = () => {};

  private destroy$ = new Subject<void>();

  constructor(
    public translate: TranslateService,
    public dialog: MatDialog,
    private dialogService: DialogService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    this.masterFormGroup =  this.fb.group({ slaves: this.fb.array([]) });
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
    ).subscribe(text => this.updateTableData(this.slaves.value, text.trim()));
  }

  registerOnChange(fn: (value: ModbusMasterConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(master: ModbusMasterConfig): void {
    this.slaves.clear();
    this.pushDataAsFormArrays(master.slaves);
  }

  enterFilterMode(): void {
    this.textSearchMode = true;
    this.cdr.detectChanges();
    const searchInput = this.searchInputField.nativeElement;
    searchInput.focus();
    searchInput.setSelectionRange(0, 0);
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
    const withIndex = isDefinedAndNotNull(index);
    const value = withIndex ? this.slaves.at(index).value : {};
    this.getSlaveDialog(value, withIndex ? 'action.apply' : 'action.add').afterClosed()
      .pipe(take(1), takeUntil(this.destroy$))
      .subscribe(res => {
        if (res) {
          if (withIndex) {
            this.slaves.at(index).patchValue(res);
          } else {
            this.slaves.push(this.fb.control(res));
          }
          this.masterFormGroup.markAsDirty();
        }
    });
  }

  private getSlaveDialog(
    value: LegacySlaveConfig | SlaveConfig,
    buttonTitle: string
  ): MatDialogRef<ModbusLegacySlaveDialogComponent | ModbusSlaveDialogComponent> {
    if (this.isLegacy) {
      return this.dialog.open<ModbusLegacySlaveDialogComponent, ModbusSlaveInfo<LegacySlaveConfig>, ModbusValues>
      (ModbusLegacySlaveDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          value: value as LegacySlaveConfig,
          hideNewFields: true,
          buttonTitle
        }
      });
    }
    return this.dialog.open<ModbusSlaveDialogComponent, ModbusSlaveInfo, ModbusValues>(ModbusSlaveDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value: value as SlaveConfig,
        buttonTitle,
        hideNewFields: false,
      }
    });
  }

  deleteSlave($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('gateway.delete-slave-title'),
      '',
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).pipe(take(1), takeUntil(this.destroy$)).subscribe((result) => {
      if (result) {
        this.slaves.removeAt(index);
        this.masterFormGroup.markAsDirty();
      }
    });
  }

  private updateTableData(data: SlaveConfig[], textSearch?: string): void {
    if (textSearch) {
      data = data.filter(item =>
        Object.values(item).some(value =>
          value.toString().toLowerCase().includes(textSearch.toLowerCase())
        )
      );
    }
    this.dataSource.loadData(data);
  }

  private pushDataAsFormArrays(slaves: SlaveConfig[]): void {
    if (slaves?.length) {
      slaves.forEach((slave: SlaveConfig) => this.slaves.push(this.fb.control(slave)));
    }
  }
}

export class SlavesDatasource extends TbTableDatasource<SlaveConfig> {
  constructor() {
    super();
  }
}
