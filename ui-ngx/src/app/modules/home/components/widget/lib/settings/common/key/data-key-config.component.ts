///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, DestroyRef, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  ComparisonResultType,
  comparisonResultTypeTranslationMap,
  DataKey,
  dataKeyAggregationTypeHintTranslationMap,
  DataKeyConfigMode,
  DynamicFormData,
  Widget,
  widgetType
} from '@shared/models/widget.models';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityService } from '@core/http/entity.service';
import { DataKeySettingsFunction } from './data-keys.component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable, of } from 'rxjs';
import { map, mergeMap, publishReplay, refCount, tap } from 'rxjs/operators';
import { alarmFields } from '@shared/models/alarm.models';
import { JsFuncComponent } from '@shared/components/js-func.component';
import { WidgetService } from '@core/http/widget.service';
import { Dashboard } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { aggregationTranslations, AggregationType, ComparisonDuration } from '@shared/models/time/time.models';
import { genNextLabel, isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { WidgetComponentService } from '@home/components/widget/widget-component.service';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { isNotEmptyTbFunction, TbFunction } from '@shared/models/js-function.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-data-key-config',
  templateUrl: './data-key-config.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataKeyConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DataKeyConfigComponent),
      multi: true,
    }
  ]
})
export class DataKeyConfigComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  dataKeyConfigModes = DataKeyConfigMode;

  dataKeyTypes = DataKeyType;

  widgetTypes = widgetType;

  aggregations = [AggregationType.NONE, ...Object.keys(AggregationType).filter(type => type !== AggregationType.NONE)];

  aggregationTypes = AggregationType;

  aggregationTypesTranslations = aggregationTranslations;

  dataKeyAggregationTypeHintTranslations = dataKeyAggregationTypeHintTranslationMap;

  comparisonResultTypes = ComparisonResultType;

  comparisonResults = Object.keys(ComparisonResultType);

  comparisonResultTypeTranslations = comparisonResultTypeTranslationMap;

  @Input()
  dataKeyConfigMode: DataKeyConfigMode;

  @Input()
  deviceId: string;

  @Input()
  entityAliasId: string;

  @Input()
  callbacks: WidgetConfigCallbacks;

  @Input()
  dashboard: Dashboard;

  @Input()
  aliasController: IAliasController;

  @Input()
  widget: Widget;

  @Input()
  widgetType: widgetType;

  @Input()
  dataKeySettingsForm: FormProperty[];

  @Input()
  dataKeySettingsDirective: string;

  @Input()
  showPostProcessing = true;

  @Input()
  @coerceBoolean()
  hideDataKeyName = false;

  @Input()
  @coerceBoolean()
  hideDataKeyLabel = false;

  @Input()
  @coerceBoolean()
  hideDataKeyColor = false;

  @Input()
  @coerceBoolean()
  hideDataKeyUnits = false;

  @Input()
  @coerceBoolean()
  hideDataKeyDecimals = false;

  @ViewChild('keyInput') keyInput: ElementRef;

  @ViewChild('funcBodyEdit', {static: false}) funcBodyEdit: JsFuncComponent;
  @ViewChild('postFuncBodyEdit', {static: false}) postFuncBodyEdit: JsFuncComponent;

  hasAdvanced = false;

  modelValue: DataKey;

  widgetConfig: WidgetConfigComponentData;

  private propagateChange = null;

  public dataKeyFormGroup: UntypedFormGroup;

  public dataKeySettingsFormGroup: UntypedFormGroup;

  private dataKeySettingsData: DynamicFormData;

  private alarmKeys: Array<DataKey>;
  private functionTypeKeys: Array<DataKey>;

  filteredKeys: Observable<Array<string>>;
  private latestKeySearchResult: Array<string> = null;
  private fetchObservable$: Observable<Array<string>> = null;

  keySearchText = '';

  functionScopeVariables: string[];

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private entityService: EntityService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private widgetComponentService: WidgetComponentService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
  }

  ngOnInit(): void {

    const widgetInfo = this.widgetComponentService.getInstantWidgetInfo(this.widget);
    const typeParameters = widgetInfo.typeParameters;
    const dataKeySettingsFunction: DataKeySettingsFunction = typeParameters?.dataKeySettingsFunction;

    this.widgetConfig = {
      widgetName: widgetInfo.widgetName,
      config: this.widget.config,
      widgetType: this.widget.type,
      typeParameters,
      dataKeySettingsFunction,
      settingsDirective: widgetInfo.settingsDirective,
      dataKeySettingsDirective: widgetInfo.dataKeySettingsDirective,
      latestDataKeySettingsDirective: widgetInfo.latestDataKeySettingsDirective,
      hasBasicMode: isDefinedAndNotNull(widgetInfo.hasBasicMode) ? widgetInfo.hasBasicMode : false,
      basicModeDirective: widgetInfo.basicModeDirective
    } as WidgetConfigComponentData;

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
    if (this.dataKeySettingsForm?.length ||
      this.dataKeySettingsDirective && this.dataKeySettingsDirective.length) {
      this.hasAdvanced = true;
      this.dataKeySettingsData = {
        settingsForm: this.dataKeySettingsForm,
        settingsDirective: this.dataKeySettingsDirective
      };
      this.dataKeySettingsFormGroup = this.fb.group({
        settings: [null, []]
      });
      this.dataKeySettingsFormGroup.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateModel();
      });
    }
    this.dataKeyFormGroup = this.fb.group({
      name: [null, []],
      aggregationType: [null, []],
      comparisonEnabled: [null, []],
      timeForComparison: [null, [Validators.required]],
      comparisonCustomIntervalValue: [null, [Validators.required, Validators.min(1000)]],
      comparisonResultType: [null, [Validators.required]],
      label: [null, [Validators.required]],
      color: [null, [Validators.required]],
      units: [null, []],
      decimals: [null, [Validators.min(0), Validators.max(15), Validators.pattern(/^\d*$/)]],
      funcBody: [null, []],
      usePostProcessing: [null, []],
      postFuncBody: [null, []]
    });

    this.dataKeyFormGroup.get('aggregationType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (aggType) => {
        if (!this.dataKeyFormGroup.get('label').dirty) {
          let newLabel = this.dataKeyFormGroup.get('name').value;
          if (aggType !== AggregationType.NONE) {
            const prefix = this.translate.instant(aggregationTranslations.get(aggType));
            newLabel = genNextLabel(prefix + ' ' + newLabel, this.widget.config.datasources);
          }
          this.dataKeyFormGroup.get('label').patchValue(newLabel);
        }
        this.updateComparisonValidators();
      }
    );

    this.dataKeyFormGroup.get('comparisonEnabled').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.updateComparisonValues();
      }
    );

    this.dataKeyFormGroup.get('timeForComparison').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.updateComparisonValues();
      }
    );

    this.dataKeyFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });

    this.dataKeyFormGroup.get('usePostProcessing').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((usePostProcessing: boolean) => {
      const postFuncBody: TbFunction = this.dataKeyFormGroup.get('postFuncBody').value;
      if (usePostProcessing && !isNotEmptyTbFunction(postFuncBody)) {
        this.dataKeyFormGroup.get('postFuncBody').patchValue('return value;');
      } else if (!usePostProcessing && isNotEmptyTbFunction(postFuncBody)) {
        this.dataKeyFormGroup.get('postFuncBody').patchValue(null);
      }
    });

    this.filteredKeys = this.dataKeyFormGroup.get('name').valueChanges
      .pipe(
        map(value => value ? value : ''),
        mergeMap(name => this.fetchKeys(name) )
      );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
  }

  writeValue(value: DataKey): void {
    this.modelValue = value;
    if (isNotEmptyTbFunction(this.modelValue.postFuncBody)) {
      this.modelValue.usePostProcessing = true;
    }
    if (this.widgetType === widgetType.latest && this.modelValue.type === DataKeyType.timeseries && !this.modelValue.aggregationType) {
      this.modelValue.aggregationType = AggregationType.NONE;
    }
    this.dataKeyFormGroup.patchValue(this.modelValue, {emitEvent: false});
    this.updateValidators();
    if (this.hasAdvanced) {
      this.dataKeySettingsData.model = this.modelValue.settings;
      this.dataKeySettingsFormGroup.patchValue({
        settings: this.dataKeySettingsData
      }, {emitEvent: false});
    }
  }

  private updateValidators() {
    this.dataKeyFormGroup.get('name').setValidators(this.modelValue.type !== DataKeyType.function &&
    this.modelValue.type !== DataKeyType.count
      ? [Validators.required] : []);
    if (this.modelValue.type === DataKeyType.count) {
      this.dataKeyFormGroup.get('name').disable({emitEvent: false});
    } else {
      this.dataKeyFormGroup.get('name').enable({emitEvent: false});
    }
    this.dataKeyFormGroup.get('name').updateValueAndValidity({emitEvent: false});
    this.updateComparisonValidators();
  }

  private updateComparisonValues() {
    const comparisonEnabled = this.dataKeyFormGroup.get('comparisonEnabled').value;
    if (comparisonEnabled) {
      const timeForComparison: ComparisonDuration = this.dataKeyFormGroup.get('timeForComparison').value;
      if (!timeForComparison) {
        this.dataKeyFormGroup.get('timeForComparison').patchValue('previousInterval', {emitEvent: false});
      } else if (timeForComparison === 'customInterval') {
        const comparisonCustomIntervalValue = this.dataKeyFormGroup.get('comparisonCustomIntervalValue').value;
        if (!comparisonCustomIntervalValue) {
          this.dataKeyFormGroup.get('comparisonCustomIntervalValue').patchValue(7200000, {emitEvent: false});
        }
      }
      const comparisonResultType: ComparisonResultType = this.dataKeyFormGroup.get('comparisonResultType').value;
      if (!comparisonResultType) {
        this.dataKeyFormGroup.get('comparisonResultType').patchValue(ComparisonResultType.DELTA_ABSOLUTE, {emitEvent: false});
      }
    }
    this.updateComparisonValidators();
  }

  private updateComparisonValidators() {
    const aggregationType: AggregationType = this.dataKeyFormGroup.get('aggregationType').value;
    if (aggregationType && aggregationType !== AggregationType.NONE) {
      this.dataKeyFormGroup.get('comparisonEnabled').enable({emitEvent: false});
      const comparisonEnabled = this.dataKeyFormGroup.get('comparisonEnabled').value;
      if (comparisonEnabled) {
        this.dataKeyFormGroup.get('timeForComparison').enable({emitEvent: false});
        const timeForComparison: ComparisonDuration = this.dataKeyFormGroup.get('timeForComparison').value;
        if (timeForComparison) {
          this.dataKeyFormGroup.get('comparisonResultType').enable({emitEvent: false});
          if (timeForComparison === 'customInterval') {
            this.dataKeyFormGroup.get('comparisonCustomIntervalValue').enable({emitEvent: false});
          } else {
            this.dataKeyFormGroup.get('comparisonCustomIntervalValue').disable({emitEvent: false});
          }
        } else {
          this.dataKeyFormGroup.get('comparisonResultType').disable({emitEvent: false});
          this.dataKeyFormGroup.get('comparisonCustomIntervalValue').disable({emitEvent: false});
        }
      } else {
        this.dataKeyFormGroup.get('timeForComparison').disable({emitEvent: false});
        this.dataKeyFormGroup.get('comparisonResultType').disable({emitEvent: false});
        this.dataKeyFormGroup.get('comparisonCustomIntervalValue').disable({emitEvent: false});
      }
    } else {
      this.dataKeyFormGroup.get('comparisonEnabled').disable({emitEvent: false});
      this.dataKeyFormGroup.get('timeForComparison').disable({emitEvent: false});
      this.dataKeyFormGroup.get('comparisonResultType').disable({emitEvent: false});
      this.dataKeyFormGroup.get('comparisonCustomIntervalValue').disable({emitEvent: false});
    }
    this.dataKeyFormGroup.get('comparisonEnabled').updateValueAndValidity({emitEvent: false});
    this.dataKeyFormGroup.get('timeForComparison').updateValueAndValidity({emitEvent: false});
    this.dataKeyFormGroup.get('comparisonResultType').updateValueAndValidity({emitEvent: false});
    this.dataKeyFormGroup.get('comparisonCustomIntervalValue').updateValueAndValidity({emitEvent: false});
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataKeyFormGroup.value};
    if (this.hasAdvanced) {
      this.modelValue.settings = this.dataKeySettingsFormGroup.get('settings').value.model;
    }
    if (this.modelValue.name) {
      this.modelValue.name = this.modelValue.name.trim();
    }
    this.propagateChange(this.modelValue);
  }

  clearKey() {
    this.dataKeyFormGroup.get('name').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  private fetchKeys(searchText?: string): Observable<Array<string>> {
    if (this.keySearchText !== searchText || this.latestKeySearchResult === null) {
      this.keySearchText = searchText;
      const dataKeyFilter = this.createKeyFilter(this.keySearchText);
      return this.getKeys().pipe(
        map(name => name.filter(dataKeyFilter)),
        tap(res => this.latestKeySearchResult = res)
      );
    }
    return of(this.latestKeySearchResult);
  }

  private getKeys() {
    if (this.fetchObservable$ === null) {
      let fetchObservable: Observable<Array<DataKey>>;
      if (this.modelValue.type === DataKeyType.alarm) {
        fetchObservable = of(this.alarmKeys);
      } else if (this.modelValue.type === DataKeyType.function) {
        fetchObservable = of(this.functionTypeKeys);
      } else {
        if (this.deviceId || this.entityAliasId) {
          const dataKeyTypes = [this.modelValue.type];
          if (this.deviceId) {
            fetchObservable = this.callbacks.fetchEntityKeysForDevice(this.deviceId, dataKeyTypes);
          } else {
            fetchObservable = this.callbacks.fetchEntityKeys(this.entityAliasId, dataKeyTypes);
          }
        } else {
          fetchObservable = of([]);
        }
      }
      this.fetchObservable$ = fetchObservable.pipe(
        map((dataKeys) => dataKeys.map((dataKey) => dataKey.name)),
        publishReplay(1),
        refCount()
      );
    }
    return this.fetchObservable$;
  }

  private createKeyFilter(query: string): (key: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.toLowerCase().startsWith(lowercaseQuery);
  }

  public validateOnSubmit(): Observable<void> {
    if (this.modelValue.type === DataKeyType.function && this.funcBodyEdit) {
      return this.funcBodyEdit.validateOnSubmit();
    } else if ((this.modelValue.type === DataKeyType.timeseries ||
                this.modelValue.type === DataKeyType.attribute) && this.dataKeyFormGroup.get('usePostProcessing').value &&
                this.postFuncBodyEdit) {
      return this.postFuncBodyEdit.validateOnSubmit();
    } else {
      return of(null);
    }
  }

  public validate(c: UntypedFormControl) {
    if (!this.dataKeyFormGroup.valid) {
      return {
        dataKey: {
          valid: false
        }
      };
    }
    if (this.hasAdvanced && (!this.dataKeySettingsFormGroup.valid || !this.modelValue.settings)) {
      return {
        dataKeySettings: {
          valid: false
        }
      };
    }
    return null;
  }
}
