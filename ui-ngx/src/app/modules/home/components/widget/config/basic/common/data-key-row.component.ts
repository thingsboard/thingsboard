///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKey, DatasourceType, JsonSettingsSchema, widgetType } from '@shared/models/widget.models';
import { DataKeysPanelComponent } from '@home/components/widget/config/basic/common/data-keys-panel.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { AggregationType } from '@shared/models/time/time.models';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipGrid, MatChipInputEvent } from '@angular/material/chips';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import { MatAutocomplete, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Observable, of } from 'rxjs';
import { filter, map, mergeMap, publishReplay, refCount, share, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';

export const dataKeyRowValidator = (control: AbstractControl): ValidationErrors | null => {
  const dataKey: DataKey = control.value;
  if (!dataKey || !dataKey.type || !dataKey.name) {
    return {
      dataKey: true
    };
  }
  return null;
};

@Component({
  selector: 'tb-data-key-row',
  templateUrl: './data-key-row.component.html',
  styleUrls: ['./data-key-row.component.scss', '../../data-keys.component.scss', '../../widget-config.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataKeyRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class DataKeyRowComponent implements ControlValueAccessor, OnInit, OnChanges {

  dataKeyTypes = DataKeyType;
  widgetTypes = widgetType;

  separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  @ViewChild('keyInput') keyInput: ElementRef<HTMLInputElement>;
  @ViewChild('keyAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild(MatAutocompleteTrigger) autocomplete: MatAutocompleteTrigger;
  @ViewChild('chipList') chipList: MatChipGrid;

  @Input()
  disabled: boolean;

  @Input()
  datasourceType: DatasourceType;

  @Input()
  entityAliasId: string;

  @Input()
  deviceId: string;

  keyFormControl: UntypedFormControl;

  keyRowFormGroup: UntypedFormGroup;

  modelValue: DataKey;

  filteredKeys: Observable<Array<DataKey>>;

  keySearchText = '';

  private latestKeySearchTextResult: Array<DataKey> = null;
  private keyFetchObservable$: Observable<Array<DataKey>> = null;

  get dataKeyType(): DataKeyType {
    return this.dataKeysPanelComponent.dataKeyType;
  }

  get alarmKeys(): Array<DataKey> {
    return this.dataKeysPanelComponent.alarmKeys;
  }

  get functionTypeKeys(): Array<DataKey> {
    return this.dataKeysPanelComponent.functionTypeKeys;
  }

  get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  get callbacks(): DataKeysCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  get datakeySettingsSchema(): JsonSettingsSchema {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsSchema;
  }

  get isEntityDatasource(): boolean {
    return [DatasourceType.device, DatasourceType.entity].includes(this.datasourceType);
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private dataKeysPanelComponent: DataKeysPanelComponent,
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  ngOnInit() {
    this.keyFormControl = this.fb.control('');
    this.keyRowFormGroup = this.fb.group({
      label: [null, []],
      color: [null, []],
      units: [null, []],
      decimals: [null, []],
    });
    this.keyRowFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.filteredKeys = this.keyFormControl.valueChanges
      .pipe(
        tap((value: string | DataKey) => {
          if (value && typeof value !== 'string') {
            this.addKeyFromChipValue(value);
          } else if (value === null) {
            this.clearKeyChip(this.keyInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchKeys(name) ),
        share()
      );
  }

  private reset() {
    if (this.keyInput) {
      this.keyInput.nativeElement.value = '';
    }
    this.keyFormControl.patchValue('', {emitEvent: false});
    this.latestKeySearchTextResult = null;
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['deviceId', 'entityAliasId'].includes(propName)) {
          this.clearKeySearchCache();
        } else if (['datasourceType'].includes(propName)) {
          if ([DatasourceType.device, DatasourceType.entity].includes(change.previousValue) &&
              [DatasourceType.device, DatasourceType.entity].includes(change.currentValue)) {
            this.clearKeySearchCache();
          } else {
            this.clearKeySearchCache();
            setTimeout(() => {
              this.reset();
            }, 1);
          }
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.keyRowFormGroup.disable({emitEvent: false});
    } else {
      this.keyRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKey): void {
    this.modelValue = value || {} as DataKey;
    this.keyRowFormGroup.patchValue(
      {
        label: value?.label,
        color: value?.color,
        units: value?.units,
        decimals: value?.decimals
      }, {emitEvent: false}
    );
    this.cd.markForCheck();
  }

  dataKeyHasAggregation(): boolean {
    return this.widgetConfigComponent.widgetType === widgetType.latest && this.modelValue?.type === DataKeyType.timeseries
      && this.modelValue?.aggregationType && this.modelValue?.aggregationType !== AggregationType.NONE;
  }

  dataKeyHasPostprocessing(): boolean {
    return !!this.modelValue?.postFuncBody;
  }

  displayKeyFn(key?: DataKey): string | undefined {
    return key ? key.name : undefined;
  }

  createKey(name: string, dataKeyType: DataKeyType = this.dataKeyType) {
    this.addKeyFromChipValue({name: name ? name.trim() : '', type: dataKeyType});
  }

  addKey(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim() && this.dataKeyType) {
      this.addKeyFromChipValue({name: value.trim(), type: this.dataKeyType});
    } else {
      this.clearKeyChip();
    }
  }

  editKey() {

  }

  removeKey() {
    this.modelValue = {} as DataKey;
    this.updateModel();
    this.clearKeyChip();
  }

  textIsNotEmpty(text: string): boolean {
    return text && text.length > 0;
  }

  clearKeyChip(value: string = '', focus = true) {
    this.autocomplete.closePanel();
    this.keyInput.nativeElement.value = value;
    this.keyFormControl.patchValue(value, {emitEvent: focus});
    if (focus) {
      setTimeout(() => {
        this.keyInput.nativeElement.blur();
        this.keyInput.nativeElement.focus();
      }, 0);
    }
  }

  onKeyInputFocus() {
    if (!this.modelValue.type) {
      this.keyFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
    }
  }

  private fetchKeys(searchText?: string): Observable<Array<DataKey>> {
    if (this.keySearchText !== searchText || this.latestKeySearchTextResult === null) {
      this.keySearchText = searchText;
      const dataKeyFilter = this.createDataKeyFilter(this.keySearchText);
      return this.getKeys().pipe(
        map(name => name.filter(dataKeyFilter)),
        tap(res => this.latestKeySearchTextResult = res)
      );
    }
    return of(this.latestKeySearchTextResult);
  }

  private getKeys(): Observable<Array<DataKey>> {
    if (this.keyFetchObservable$ === null) {
      let fetchObservable: Observable<Array<DataKey>>;
      if (this.datasourceType === DatasourceType.function) {
        const targetKeysList = this.widgetType === widgetType.alarm ? this.alarmKeys : this.functionTypeKeys;
        fetchObservable = of(targetKeysList);
      } else if (this.datasourceType === DatasourceType.entity && this.entityAliasId ||
        this.datasourceType === DatasourceType.device && this.deviceId) {
        const dataKeyTypes = [DataKeyType.timeseries];
        if (this.widgetType === widgetType.latest || this.widgetType === widgetType.alarm) {
          dataKeyTypes.push(DataKeyType.attribute);
          dataKeyTypes.push(DataKeyType.entityField);
          if (this.widgetType === widgetType.alarm) {
            dataKeyTypes.push(DataKeyType.alarm);
          }
        }
        if (this.datasourceType === DatasourceType.device) {
          fetchObservable = this.callbacks.fetchEntityKeysForDevice(this.deviceId, dataKeyTypes);
        } else {
          fetchObservable = this.callbacks.fetchEntityKeys(this.entityAliasId, dataKeyTypes);
        }
      } else {
        fetchObservable = of([]);
      }
      this.keyFetchObservable$ = fetchObservable.pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.keyFetchObservable$;
  }

  private createDataKeyFilter(query: string): (key: DataKey) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.name.toLowerCase().startsWith(lowercaseQuery);
  }

  private addKeyFromChipValue(chip: DataKey) {
    this.modelValue = this.callbacks.generateDataKey(chip.name, chip.type, this.datakeySettingsSchema);
    if (!this.keyRowFormGroup.get('label').value) {
      this.keyRowFormGroup.get('label').patchValue(this.modelValue.label, {emitEvent: false});
    }
    this.updateModel();
    this.clearKeyChip('', false);
  }

  private clearKeySearchCache() {
    this.keySearchText = '';
    this.keyFetchObservable$ = null;
    this.latestKeySearchTextResult = null;
  }

  private updateModel() {
    const value: DataKey = this.keyRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
