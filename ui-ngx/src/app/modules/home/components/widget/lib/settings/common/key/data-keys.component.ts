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

import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import {
  Component,
  DestroyRef,
  ElementRef,
  forwardRef, HostBinding,
  Input,
  OnChanges,
  OnInit,
  Renderer2,
  SimpleChanges,
  SkipSelf,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgForm,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Observable, of, ReplaySubject } from 'rxjs';
import { filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocomplete, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatChipGrid, MatChipInputEvent, MatChipRow } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKey, DatasourceType, Widget, widgetType } from '@shared/models/widget.models';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeySettingsFunction } from './data-keys.component.models';
import { alarmFields } from '@shared/models/alarm.models';
import { UtilsService } from '@core/services/utils.service';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from './data-key-config-dialog.component';
import { deepClone, guid, isDefinedAndNotNull, isObject, isUndefined } from '@core/utils';
import { Dashboard } from '@shared/models/dashboard.models';
import { AggregationType } from '@shared/models/time/time.models';
import { DndDropEvent } from 'ngx-drag-drop/lib/dnd-dropzone.directive';
import { moveItemInArray } from '@angular/cdk/drag-drop';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ColorPickerPanelComponent } from '@shared/components/color-picker/color-picker-panel.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';

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
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DataKeysComponent),
      multi: true,
    },
    {
      provide: ErrorStateMatcher,
      useExisting: DataKeysComponent
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class DataKeysComponent implements ControlValueAccessor, OnInit, OnChanges, ErrorStateMatcher, Validator {

  @HostBinding('class')
  hostClass = 'tb-data-keys';

  @Input()
  label: string;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  inlineField = false;

  @Input()
  @coerceBoolean()
  hideDataKeyLabel: boolean;

  @Input()
  @coerceBoolean()
  hideDataKeyColor: boolean;

  @Input()
  @coerceBoolean()
  hideDataKeyUnits: boolean;

  @Input()
  @coerceBoolean()
  hideDataKeyDecimals: boolean;

  @Input()
  @coerceBoolean()
  disableDrag = false;

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
    return this.isCountDatasource ? 1 : this.maxDataKeysValue;
  }

  @Input()
  set maxDataKeys(value: number) {
    this.maxDataKeysValue = value;
  }

  @Input()
  @coerceBoolean()
  optDataKeys: boolean;

  @Input()
  @coerceBoolean()
  latestDataKeys = false;

  @Input()
  @coerceBoolean()
  simpleDataKeysLabel = false;

  @Input()
  @coerceBoolean()
  supportsUnitConversion = false;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeySettingsForm: FormProperty[];

  @Input()
  datakeySettingsFunction: DataKeySettingsFunction;

  @Input()
  dataKeySettingsDirective: string;

  @Input()
  dashboard: Dashboard;

  @Input()
  widget: Widget;

  @Input()
  callbacks: WidgetConfigCallbacks;

  @Input()
  entityAliasId: string;

  @Input()
  deviceId: string;

  @Input()
  generateKey: (key: DataKey) => DataKey;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue || !this.optDataKeys || this.isCountDatasource;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @ViewChild('keyInput') keyInput: ElementRef<HTMLInputElement>;
  @ViewChild('keyAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild(MatAutocompleteTrigger) autocomplete: MatAutocompleteTrigger;
  @ViewChild('chipList') chipList: MatChipGrid;

  keys: Array<DataKey> = [];
  filteredKeys: Observable<Array<DataKey>>;

  separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  alarmKeys: Array<DataKey>;
  functionTypeKeys: Array<DataKey>;

  dataKeyType: DataKeyType;
  placeholder: string;
  secondaryPlaceholder: string;
  requiredText: string;

  searchText = '';

  dndId = guid();

  dragIndex: number;

  private latestSearchTextResult: Array<DataKey> = null;
  private fetchObservable$: Observable<Array<DataKey>> = null;

  private dirty = false;

  private propagateChange = (_v: any) => { };

  private keysRequired = this._keysRequired.bind(this);
  private keysValidator = this._keysValidator.bind(this);

  constructor(@SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private viewContainerRef: ViewContainerRef,
              private renderer: Renderer2,
              private destroyRef: DestroyRef) {
  }

  updateValidators() {
    if (this.required) {
      this.keysListFormGroup.get('keys').addValidators(this.keysRequired);
    } else {
      this.keysListFormGroup.get('keys').removeValidators(this.keysRequired);
    }
    this.keysListFormGroup.get('keys').updateValueAndValidity();
  }

  private _keysRequired(_control: AbstractControl): ValidationErrors | null {
    const value = this.modelValue;
    if (value && Array.isArray(value) && value.length) {
      return null;
    } else {
      return {required: true};
    }
  }

  private _keysValidator(_control: AbstractControl): ValidationErrors | null {
    const value = this.modelValue;
    if (value && Array.isArray(value)) {
      if (value.some(v => isObject(v) && (!v.type || !v.name))) {
        return {
          dataKey: true
        };
      }
    }
    return null;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (!this.keysListFormGroup.valid) {
      this.propagateChange(this.modelValue);
    }
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.keysListFormGroup = this.fb.group({
        keys: [null, [this.keysValidator]],
        key: [null]
    });
    if (this.required) {
      this.keysListFormGroup.get('keys').addValidators(this.keysRequired);
    }
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
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchKeys(name) ),
        share()
      );

    this.aliasController.entityAliasesChanged.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(aliasIds => {
      if (aliasIds.includes(this.entityAliasId)) {
        this.clearSearchCache();
        this.dirty = true;
      }
    })
  }

  public maxDataKeysText(): string {
    if (this.maxDataKeys !== null && this.maxDataKeys > -1) {
      if (this.datasourceType === DatasourceType.function) {
        return this.translate.instant('datakey.maximum-function-types', {count: this.maxDataKeys});
      } else if (!this.isCountDatasource) {
        return this.translate.instant('datakey.maximum-timeseries-or-attributes', {count: this.maxDataKeys});
      } else {
        return '';
      }
    } else {
      return '';
    }
  }

  private updateParams() {
    const singleKey = this.maxDataKeysSet && this.maxDataKeys === 1;
    this.secondaryPlaceholder = '+' + this.translate.instant('action.add');
    if (this.datasourceType === DatasourceType.function) {
      this.dataKeyType = DataKeyType.function;
      this.requiredText = this.translate.instant('datakey.function-types-required');
      if (this.widgetType === widgetType.latest) {
        this.placeholder = this.translate.instant(singleKey ? 'datakey.latest-key-function' : 'datakey.latest-key-functions');
      } else if (this.widgetType === widgetType.alarm) {
        this.placeholder = this.translate.instant(singleKey ? 'datakey.alarm-key-function' : 'datakey.alarm-key-functions');
      } else {
        this.placeholder = this.translate.instant(singleKey ? 'datakey.timeseries-key-function' : 'datakey.timeseries-key-functions');
      }
    } else {
      if (this.widgetType !== widgetType.latest && this.widgetType !== widgetType.alarm) {
        this.dataKeyType = DataKeyType.timeseries;
      } else {
        this.dataKeyType = null;
      }
      if (this.simpleDataKeysLabel && this.widgetType !== widgetType.alarm) {
        this.placeholder = this.translate.instant(singleKey ? 'datakey.data-key' : 'datakey.data-keys');
        this.requiredText = this.translate.instant(singleKey ? 'datakey.data-key-required' : 'datakey.data-keys-required');
      } else {
        if (this.widgetType === widgetType.latest) {
          this.placeholder = this.translate.instant(singleKey ? 'datakey.latest-key' : 'datakey.latest-keys');
          this.requiredText = this.translate.instant('datakey.timeseries-or-attributes-required');
        } else if (this.widgetType === widgetType.alarm) {
          this.placeholder = this.translate.instant(singleKey ? 'datakey.alarm-key' : 'datakey.alarm-keys');
          this.requiredText = this.translate.instant('datakey.alarm-fields-timeseries-or-attributes-required');
        } else {
          this.placeholder = this.translate.instant(singleKey ? 'datakey.timeseries-key' : 'datakey.timeseries-keys');
          this.requiredText = this.translate.instant('datakey.timeseries-required');
        }
      }
    }
  }

  private reset() {
    if (this.widgetType === widgetType.alarm) {
      this.keys = this.utils.getDefaultAlarmDataKeys();
    } else if (this.isCountDatasource) {
      this.keys = [this.callbacks.generateDataKey('count', DataKeyType.count, this.dataKeySettingsForm,
        this.latestDataKeys, this.datakeySettingsFunction)];
    } else {
      this.keys = [];
    }
    this.modelValue = this.keys.length ? [...this.keys] : null;
    this.keysListFormGroup.get('keys').setValue(this.keys);
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
        if (['deviceId', 'entityAliasId'].includes(propName)) {
          this.clearSearchCache();
          this.dirty = true;
        } else if (['widgetType', 'datasourceType', 'maxDataKeys', 'simpleDataKeysLabel'].includes(propName)) {
          if (propName === 'datasourceType' &&
              [DatasourceType.device, DatasourceType.entity].includes(change.previousValue) &&
              [DatasourceType.device, DatasourceType.entity].includes(change.currentValue)) {
            this.clearSearchCache();
          } else {
            this.clearSearchCache();
            this.updateParams();
            setTimeout(() => {
              this.reset();
            }, 1);
          }
        } else if (['required', 'optDataKeys'].includes(propName)) {
          this.updateValidators();
        }
      }
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = this.keysListFormGroup.get('keys').hasError('dataKey');
    return originalErrorState || customErrorState;
  }

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
      this.modelValue = null;
      this.keysListFormGroup.get('keys').setValue(this.keys);
    }
    this.dirty = true;
  }

  validate(_c: FormControl) {
    return (this.keysListFormGroup.get('keys').hasError('dataKey')) ? {
      dataKeys: {
        valid: false,
      },
    } : null;
  }

  onFocus() {
    if (this.dirty) {
      this.keysListFormGroup.get('key').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private addFromChipValue(chip: DataKey) {
    let key: DataKey;
    if (this.generateKey) {
      key = this.generateKey(chip);
    } else {
      key = this.callbacks.generateDataKey(chip.name, chip.type, this.dataKeySettingsForm, this.latestDataKeys,
        this.datakeySettingsFunction);
    }
    this.addKey(key);
  }

  addKey(key: DataKey): void {
    if (!this.maxDataKeysSet ||
        !this.modelValue || this.modelValue.length < this.maxDataKeys) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(key);
      this.keys.push(key);
      this.keysListFormGroup.get('keys').setValue(this.keys);
    }
    this.propagateChange(this.modelValue);
    const focus = !this.maxDataKeysSet || this.modelValue.length < this.maxDataKeys;
    this.clear('', focus);
  }

  add(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim() && this.dataKeyType) {
      this.addFromChipValue({name: value.trim(), type: this.dataKeyType});
    } else {
      this.clear();
    }
  }

  remove(key: DataKey) {
    const index = this.keys.indexOf(key);
    if (index >= 0) {
      this.keys.splice(index, 1);
      this.modelValue.splice(index, 1);
      this.keysListFormGroup.get('keys').setValue(this.keys);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  chipDragStart(index: number, chipRow: MatChipRow, placeholderChipRow: Element) {
    this.autocomplete.closePanel();
    this.renderer.setStyle(placeholderChipRow,
      'width', chipRow._elementRef.nativeElement.offsetWidth + 'px');
    this.dragIndex = index;
  }

  chipDragEnd() {
    this.dragIndex = -1;
  }

  onChipDrop(event: DndDropEvent) {
    let index = event.index;
    if (isUndefined(index)) {
      index = this.keys.length;
    }
    moveItemInArray(this.keys, this.dragIndex, index);
    moveItemInArray(this.modelValue, this.dragIndex, index);
    this.keysListFormGroup.get('keys').setValue(this.keys);
    this.dragIndex = -1;
    this.propagateChange(this.modelValue);
  }

  openColorPickerPopup(key: DataKey, $event: Event, keyColorButton: HTMLDivElement) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = keyColorButton;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const colorPickerPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ColorPickerPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {
          color: key.color,
          colorCancelButton: true
        },
        showCloseButton: false,
        popoverContentStyle: {padding: '12px 4px 12px 12px'},
        isModal: true
      });
      colorPickerPopover.tbComponentRef.instance.popover = colorPickerPopover;
      colorPickerPopover.tbComponentRef.instance.colorSelected.subscribe((color) => {
        colorPickerPopover.hide();
        if (color && key.color !== color) {
          key.color = color;
          this.propagateChange(this.modelValue);
        }
      });
    }
  }

  editDataKey(key: DataKey, index: number) {
    this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(key),
          dataKeySettingsForm: this.dataKeySettingsForm,
          dataKeySettingsDirective: this.dataKeySettingsDirective,
          dashboard: this.dashboard,
          aliasController: this.aliasController,
          widget: this.widget,
          widgetType: this.widgetType,
          deviceId: this.deviceId,
          entityAliasId: this.entityAliasId,
          showPostProcessing: this.widgetType !== widgetType.alarm,
          callbacks: this.callbacks,
          hideDataKeyLabel: this.hideDataKeyLabel,
          hideDataKeyColor: this.hideDataKeyColor,
          hideDataKeyUnits: this.hideDataKeyUnits,
          hideDataKeyDecimals: this.hideDataKeyDecimals,
          supportsUnitConversion: this.supportsUnitConversion
        }
      }).afterClosed().subscribe((updatedDataKey) => {
        if (updatedDataKey) {
          this.keys[index] = updatedDataKey;
          this.modelValue[index] = updatedDataKey;
          this.keysListFormGroup.get('keys').setValue(this.keys);
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

  dataKeyHasAggregation(key: DataKey): boolean {
    return this.widgetType === widgetType.latest && key.type === DataKeyType.timeseries
      && key.aggregationType && key.aggregationType !== AggregationType.NONE;
  }

  dataKeyHasPostprocessing(key: DataKey): boolean {
    return this.datasourceType !== DatasourceType.function && !!key.postFuncBody;
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
      this.fetchObservable$ = fetchObservable.pipe(
        share({
          connector: () => new ReplaySubject(1),
          resetOnError: false,
          resetOnComplete: false,
          resetOnRefCountZero: false
        })
      );
    }
    return this.fetchObservable$;
  }

  private createDataKeyFilter(query: string): (key: DataKey) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.name.toLowerCase().startsWith(lowercaseQuery);
  }

  textIsNotEmpty(text: string): boolean {
    return text?.length > 0;
  }

  clear(value: string = '', focus = true) {
    this.autocomplete.closePanel();
    this.keyInput.nativeElement.value = value;
    this.keysListFormGroup.get('key').patchValue(value, {emitEvent: focus});
    if (focus) {
      setTimeout(() => {
        this.keyInput.nativeElement.blur();
        this.keyInput.nativeElement.focus();
      }, 0);
    }
  }

  get isCountDatasource(): boolean {
    return [DatasourceType.entityCount, DatasourceType.alarmCount].includes(this.datasourceType);
  }

  get isEntityDatasource(): boolean {
    return [DatasourceType.device, DatasourceType.entity].includes(this.datasourceType);
  }

  get inputDisabled(): boolean {
    return this.isCountDatasource || (this.maxDataKeysSet && this.keys.length >= this.maxDataKeys);
  }

  get dragDisabled(): boolean {
    return this.keys.length < 2 || this.disableDrag;
  }

  get maxDataKeysSet(): boolean {
    return isDefinedAndNotNull(this.maxDataKeysValue) && this.maxDataKeysValue > -1;
  }

  private clearSearchCache() {
    this.searchText = '';
    this.fetchObservable$ = null;
    this.latestSearchTextResult = null;
  }
}
