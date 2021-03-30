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

import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  SkipSelf,
  ViewChild
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  NgForm,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { filter, map, mergeMap, publishReplay, refCount, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipInputEvent, MatChipList } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeysCallbacks } from './data-keys.component.models';
import { alarmFields } from '@shared/models/alarm.models';
import { UtilsService } from '@core/services/utils.service';
import { ErrorStateMatcher } from '@angular/material/core';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from '@home/components/widget/data-key-config-dialog.component';
import { deepClone } from '@core/utils';
import { MatChipDropEvent } from '@app/shared/components/mat-chip-draggable.directive';

@Component({
  selector: 'tb-data-keys',
  templateUrl: './data-keys.component.html',
  styleUrls: ['./data-keys.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataKeysComponent),
      multi: true
    },
    {
      provide: ErrorStateMatcher,
      useExisting: DataKeysComponent
    }
  ]
})
export class DataKeysComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges, ErrorStateMatcher {

  datasourceTypes = DatasourceType;
  widgetTypes = widgetType;
  dataKeyTypes = DataKeyType;

  keysListFormGroup: FormGroup;

  modelValue: Array<DataKey> | null;

  @Input()
  widgetType: widgetType;

  @Input()
  datasourceType: DatasourceType;

  private maxDataKeysValue: number;
  get maxDataKeys(): number {
    return this.datasourceType === DatasourceType.entityCount ? 1 : this.maxDataKeysValue;
  }

  @Input()
  set maxDataKeys(value: number) {
    this.maxDataKeysValue = value;
  }

  @Input()
  optDataKeys: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  datakeySettingsSchema: any;

  @Input()
  callbacks: DataKeysCallbacks;

  @Input()
  entityAliasId: string;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue || !this.optDataKeys || this.isEntityCountDatasource;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @ViewChild('keyInput') keyInput: ElementRef<HTMLInputElement>;
  @ViewChild('keyAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList') chipList: MatChipList;

  keys: Array<DataKey> = [];
  filteredKeys: Observable<Array<DataKey>>;

  separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  alarmKeys: Array<DataKey>;
  functionTypeKeys: Array<DataKey>;

  dataKeyType: DataKeyType;
  placeholder: string;
  requiredText: string;

  searchText = '';
  private latestSearchTextResult: Array<DataKey> = null;
  private fetchObservable$: Observable<Array<DataKey>> = null;

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public translate: TranslateService,
              private utils: UtilsService,
              private dialogs: DialogService,
              private dialog: MatDialog,
              private fb: FormBuilder,
              public truncate: TruncatePipe) {
  }

  updateValidators() {
    this.keysListFormGroup.get('keys').setValidators(this.required ? [Validators.required] : []);
    this.keysListFormGroup.get('keys').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.keysListFormGroup = this.fb.group({
        keys: [null, this.required ? [Validators.required] : []],
        key: [null]
    });
    this.alarmKeys = [];
    for (const name of Object.keys(alarmFields)) {
      this.alarmKeys.push({
        name,
        type: DataKeyType.alarm
      });
    }
    this.functionTypeKeys = [];
    for (const type of this.utils.getPredefinedFunctionsList()) {
      this.functionTypeKeys.push({
        name: type,
        type: DataKeyType.function
      });
    }
    this.updateParams();
    this.filteredKeys = this.keysListFormGroup.get('key').valueChanges
      .pipe(
        tap((value: string | DataKey) => {
          if (value && typeof value !== 'string') {
            this.addFromChipValue(value);
          } else if (value === null) {
            this.clear(this.keyInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchKeys(name) ),
        share()
      );
  }

  public maxDataKeysText(): string {
    if (this.maxDataKeys !== null && this.maxDataKeys > -1) {
      if (this.datasourceType === DatasourceType.function) {
        return this.translate.instant('datakey.maximum-function-types', {count: this.maxDataKeys});
      } else if (!this.isEntityCountDatasource) {
        return this.translate.instant('datakey.maximum-timeseries-or-attributes', {count: this.maxDataKeys});
      } else {
        return '';
      }
    } else {
      return '';
    }
  }

  private updateParams() {
      if (this.datasourceType === DatasourceType.function) {
        this.dataKeyType = DataKeyType.function;
        this.placeholder = this.translate.instant('datakey.function-types');
        this.requiredText = this.translate.instant('datakey.function-types-required');
      } else {
        if (this.widgetType === widgetType.latest) {
          this.dataKeyType = null;
          this.requiredText = this.translate.instant('datakey.timeseries-or-attributes-required');
        } else if (this.widgetType === widgetType.alarm) {
          this.dataKeyType = null;
          this.requiredText = this.translate.instant('datakey.alarm-fields-timeseries-or-attributes-required');
        } else {
          this.dataKeyType = DataKeyType.timeseries;
          this.requiredText = this.translate.instant('datakey.timeseries-required');
        }
        this.placeholder = '';
      }
  }

  private reset() {
    if (this.widgetType === widgetType.alarm) {
      this.keys = this.utils.getDefaultAlarmDataKeys();
    } else if (this.isEntityCountDatasource) {
      this.keys = [this.callbacks.generateDataKey('count', DataKeyType.count)];
    } else {
      this.keys = [];
    }
    this.keysListFormGroup.get('keys').setValue(this.keys);
    this.modelValue = this.keys.length ? [...this.keys] : null;
    if (this.keyInput) {
      this.keyInput.nativeElement.value = '';
    }
    this.keysListFormGroup.get('key').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
    this.latestSearchTextResult = null;
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'entityAliasId') {
          this.clearSearchCache();
          this.dirty = true;
        } else if (['widgetType', 'datasourceType'].includes(propName)) {
          this.clearSearchCache();
          this.updateParams();
          setTimeout(() => {
            this.reset();
          }, 1);
        } else if (['required', 'optDataKeys'].includes('propName')) {
          this.updateValidators();
        }
      }
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = this.required && !this.modelValue;
    return originalErrorState || customErrorState;
  }

  ngAfterViewInit(): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.keysListFormGroup.disable({emitEvent: false});
    } else {
      this.keysListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<DataKey> | null): void {
    this.searchText = '';
    if (value != null && value.length) {
      this.modelValue = [...value];
      this.keys = [...value];
      this.keysListFormGroup.get('keys').setValue(this.keys);
    } else {
      this.keys = [];
      this.keysListFormGroup.get('keys').setValue(this.keys);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.keysListFormGroup.get('key').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private addFromChipValue(chip: DataKey) {
    const key = this.callbacks.generateDataKey(chip.name, chip.type);
    this.addKey(key);
  }

  addKey(key: DataKey): void {
    if (!this.maxDataKeys || this.maxDataKeys < 0 ||
        !this.modelValue || this.modelValue.length < this.maxDataKeys) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(key);
      this.keys.push(key);
      this.keysListFormGroup.get('keys').setValue(this.keys);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  add(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim()) {
      if (this.dataKeyType) {
        this.addFromChipValue({name: value.trim(), type: this.dataKeyType});
      }
    }
    this.clear();
  }

  remove(key: DataKey) {
    const index = this.keys.indexOf(key);
    if (index >= 0) {
      this.keys.splice(index, 1);
      this.keysListFormGroup.get('keys').setValue(this.keys);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  onChipDrop(event: MatChipDropEvent) {
    const from = event.from;
    const to = event.to;
    this.keys.splice(to, 0, this.keys.splice(from, 1)[0]);
    this.keysListFormGroup.get('keys').setValue(this.keys);
    this.modelValue.splice(to, 0, this.modelValue.splice(from, 1)[0]);
    this.propagateChange(this.modelValue);
  }

  showColorPicker(key: DataKey) {
    this.dialogs.colorPicker(key.color).subscribe(
      (color) => {
        if (color && key.color !== color) {
          key.color = color;
          this.propagateChange(this.modelValue);
        }
      }
    );
  }

  editDataKey(key: DataKey, index: number) {
    this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(key),
          dataKeySettingsSchema: this.datakeySettingsSchema,
          entityAliasId: this.entityAliasId,
          showPostProcessing: this.widgetType !== widgetType.alarm,
          callbacks: this.callbacks
        }
      }).afterClosed().subscribe((updatedDataKey) => {
        if (updatedDataKey) {
          this.keys[index] = updatedDataKey;
          this.keysListFormGroup.get('keys').setValue(this.keys);
          this.modelValue[index] = updatedDataKey;
          this.propagateChange(this.modelValue);
        }
    });
  }

  createKey(name: string, dataKeyType: DataKeyType = this.dataKeyType) {
    this.addFromChipValue({name: name ? name.trim() : '', type: dataKeyType});
  }

  displayKeyFn(key?: DataKey): string | undefined {
    return key ? key.name : undefined;
  }

  private fetchKeys(searchText?: string): Observable<Array<DataKey>> {
    if (this.searchText !== searchText || this.latestSearchTextResult === null) {
      this.searchText = searchText;
      const dataKeyFilter = this.createDataKeyFilter(this.searchText);
      return this.getKeys().pipe(
        map(name => name.filter(dataKeyFilter)),
        tap(res => this.latestSearchTextResult = res)
      );
    }
    return of(this.latestSearchTextResult);
  }

  private getKeys(): Observable<Array<DataKey>> {
    if (this.fetchObservable$ === null) {
      let fetchObservable: Observable<Array<DataKey>>;
      if (this.datasourceType === DatasourceType.function) {
        const targetKeysList = this.widgetType === widgetType.alarm ? this.alarmKeys : this.functionTypeKeys;
        fetchObservable = of(targetKeysList);
      } else if (this.datasourceType === DatasourceType.entity && this.entityAliasId) {
        const dataKeyTypes = [DataKeyType.timeseries];
        if (this.widgetType === widgetType.latest || this.widgetType === widgetType.alarm) {
          dataKeyTypes.push(DataKeyType.attribute);
          dataKeyTypes.push(DataKeyType.entityField);
          if (this.widgetType === widgetType.alarm) {
            dataKeyTypes.push(DataKeyType.alarm);
          }
        }
        fetchObservable = this.callbacks.fetchEntityKeys(this.entityAliasId, dataKeyTypes);
      } else {
        fetchObservable = of([]);
      }
      this.fetchObservable$ = fetchObservable.pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.fetchObservable$;
  }

  private createDataKeyFilter(query: string): (key: DataKey) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.name.toLowerCase().startsWith(lowercaseQuery);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text != null && text.length > 0) ? true : false;
  }

  clear(value: string = '') {
    this.keyInput.nativeElement.value = value;
    this.keysListFormGroup.get('key').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  get isEntityCountDatasource(): boolean {
    return this.datasourceType === DatasourceType.entityCount;
  }

  private clearSearchCache() {
    this.searchText = '';
    this.fetchObservable$ = null;
    this.latestSearchTextResult = null;
  }
}
