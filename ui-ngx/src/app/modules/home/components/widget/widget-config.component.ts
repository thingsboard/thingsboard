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

import {
  ChangeDetectorRef,
  Component,
  ComponentRef,
  DestroyRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CellClickColumnInfo,
  DataKey,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  DynamicFormData,
  TargetDevice,
  targetDeviceValid,
  Widget,
  WidgetConfigMode,
  widgetTitleAutocompleteValues,
  widgetType,
  widgetTypeCanHaveTimewindow,
  widgetTypeHasTimewindow
} from '@shared/models/widget.models';
import {
  AsyncValidator,
  ControlValueAccessor,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { deepClone, genNextLabel, isDefined, isDefinedAndNotNull, isObject } from '@app/core/utils';
import { alarmFields, AlarmSearchStatus } from '@shared/models/alarm.models';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityAlias } from '@shared/models/alias.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { merge, Observable, of, Subject, Subscription } from 'rxjs';
import {
  IBasicWidgetConfigComponent,
  WidgetConfigCallbacks
} from '@home/components/widget/config/widget-config.component.models';
import {
  EntityAliasDialogComponent,
  EntityAliasDialogData
} from '@home/components/alias/entity-alias-dialog.component';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { EntityService } from '@core/http/entity.service';
import { Dashboard } from '@shared/models/dashboard.models';
import { entityFields } from '@shared/models/entity.models';
import { Filter, singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { FilterDialogComponent, FilterDialogData } from '@home/components/filter/filter-dialog.component';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TimewindowConfigData } from '@home/components/widget/config/timewindow-config-panel.component';
import { DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { defaultFormProperties, FormProperty } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WidgetService } from '@core/http/widget.service';
import { TimeService } from '@core/services/time.service';
import { initModelFromDefaultTimewindow } from '@shared/models/time/time.models';
import { findWidgetModelDefinition } from '@shared/models/widget/widget-model.definition';
import Timeout = NodeJS.Timeout;

@Component({
  selector: 'tb-widget-config',
  templateUrl: './widget-config.component.html',
  styleUrls: ['./widget-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetConfigComponent),
      multi: true
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => WidgetConfigComponent),
      multi: true,
    }
  ]
})
export class WidgetConfigComponent extends PageComponent implements OnInit, OnDestroy, ControlValueAccessor, AsyncValidator {

  @ViewChild('basicModeContainer', {read: ViewContainerRef, static: false}) basicModeContainer: ViewContainerRef;

  widgetTypes = widgetType;

  widgetConfigModes = WidgetConfigMode;

  entityTypes = EntityType;

  @Input()
  forceExpandDatasources: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dashboard: Dashboard;

  @Input()
  widget: Widget;

  @Input()
  functionsOnly: boolean;

  @Input()
  @coerceBoolean()
  hideHeader = false;

  @Input()
  @coerceBoolean()
  hideToggleHeader = false;

  @Input()
  @coerceBoolean()
  isAdd = false;

  @Input()
  @coerceBoolean()
  showLayoutConfig = true;

  @Input()
  @coerceBoolean()
  isDefaultBreakpoint = true;

  @Input() disabled: boolean;

  widgetConfigMode = WidgetConfigMode.advanced;

  widgetType: widgetType;

  widgetConfigCallbacks: WidgetConfigCallbacks = {
    createEntityAlias: this.createEntityAlias.bind(this),
    editEntityAlias: this.editEntityAlias.bind(this),
    createFilter: this.createFilter.bind(this),
    generateDataKey: this.generateDataKey.bind(this),
    fetchEntityKeysForDevice: this.fetchEntityKeysForDevice.bind(this),
    fetchEntityKeys: this.fetchEntityKeys.bind(this),
    fetchDashboardStates: this.fetchDashboardStates.bind(this),
    fetchCellClickColumns: this.fetchCellClickColumns.bind(this)
  };

  widgetEditMode = this.utils.widgetEditMode;

  basicModeDirectiveError: string;

  modelValue: WidgetConfigComponentData;

  private propagateChange = null;

  headerOptions: ToggleHeaderOption[] = [];
  selectedOption: string;
  predefinedValues = widgetTitleAutocompleteValues;

  public dataSettings: UntypedFormGroup;
  public targetDeviceSettings: UntypedFormGroup;
  public widgetSettings: UntypedFormGroup;
  public layoutSettings: UntypedFormGroup;
  public advancedSettings: UntypedFormGroup;
  public actionsSettings: UntypedFormGroup;

  private createBasicModeComponentTimeout: Timeout;
  private basicModeComponentRef: ComponentRef<IBasicWidgetConfigComponent>;
  private basicModeComponent: IBasicWidgetConfigComponent;
  private basicModeComponent$: Subject<IBasicWidgetConfigComponent> = null;
  private basicModeComponentChangeSubscription: Subscription;

  private dataSettingsChangesSubscription: Subscription;
  private targetDeviceSettingsSubscription: Subscription;
  private widgetSettingsSubscription: Subscription;
  private layoutSettingsSubscription: Subscription;
  private advancedSettingsSubscription: Subscription;
  private actionsSettingsSubscription: Subscription;

  private defaultConfigFormsType: widgetType;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private entityService: EntityService,
              public timeService: TimeService,
              private dialog: MatDialog,
              public translate: TranslateService,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.dataSettings = this.fb.group({});
    this.targetDeviceSettings = this.fb.group({});
    this.advancedSettings = this.fb.group({});
    this.widgetSettings = this.fb.group({
      title: [null, []],
      titleFont: [null, []],
      titleColor: [null, []],
      showTitleIcon: [null, []],
      titleIcon: [null, []],
      iconColor: [null, []],
      iconSize: [null, []],
      titleTooltip: [null, []],
      showTitle: [null, []],
      dropShadow: [null, []],
      enableFullscreen: [null, []],
      backgroundColor: [null, []],
      color: [null, []],
      padding: [null, []],
      margin: [null, []],
      borderRadius: [null, []],
      widgetStyle: [null, []],
      widgetCss: [null, []],
      titleStyle: [null, []],
      pageSize: [1024, [Validators.min(1), Validators.pattern(/^\d*$/)]],
      units: [null, []],
      decimals: [null, [Validators.min(0), Validators.max(15), Validators.pattern(/^\d*$/)]],
      noDataDisplayMessage: [null, []]
    });

    merge(this.widgetSettings.get('showTitle').valueChanges,
          this.widgetSettings.get('showTitleIcon').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateWidgetSettingsEnabledState();
    });

    this.layoutSettings = this.fb.group({
      resizable: [true],
      preserveAspectRatio: [false],
      mobileOrder: [null, [Validators.pattern(/^-?[0-9]+$/)]],
      mobileHeight: [null, [Validators.min(1), Validators.pattern(/^\d*$/)]],
      mobileHide: [false],
      desktopHide: [false]
    });

    this.layoutSettings.get('resizable').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateLayoutEnabledState();
    });

    this.actionsSettings = this.fb.group({
      actions: [null, []]
    });
  }

  ngOnDestroy(): void {
    this.destroyBasicModeComponent();
    this.removeChangeSubscriptions();
  }

  private removeChangeSubscriptions() {
    if (this.dataSettingsChangesSubscription) {
      this.dataSettingsChangesSubscription.unsubscribe();
      this.dataSettingsChangesSubscription = null;
    }
    if (this.targetDeviceSettingsSubscription) {
      this.targetDeviceSettingsSubscription.unsubscribe();
      this.targetDeviceSettingsSubscription = null;
    }
    if (this.widgetSettingsSubscription) {
      this.widgetSettingsSubscription.unsubscribe();
      this.widgetSettingsSubscription = null;
    }
    if (this.layoutSettingsSubscription) {
      this.layoutSettingsSubscription.unsubscribe();
      this.layoutSettingsSubscription = null;
    }
    if (this.advancedSettingsSubscription) {
      this.advancedSettingsSubscription.unsubscribe();
      this.advancedSettingsSubscription = null;
    }
    if (this.actionsSettingsSubscription) {
      this.actionsSettingsSubscription.unsubscribe();
      this.actionsSettingsSubscription = null;
    }
  }

  private createChangeSubscriptions() {
    this.dataSettingsChangesSubscription = this.dataSettings.valueChanges.subscribe(
      () => this.updateDataSettings()
    );
    this.targetDeviceSettingsSubscription = this.targetDeviceSettings.valueChanges.subscribe(
      () => this.updateTargetDeviceSettings()
    );
    this.widgetSettingsSubscription = this.widgetSettings.valueChanges.subscribe(
      () => this.updateWidgetSettings()
    );
    this.layoutSettingsSubscription = this.layoutSettings.valueChanges.subscribe(
      () => this.updateLayoutSettings()
    );
    this.advancedSettingsSubscription = this.advancedSettings.valueChanges.subscribe(
      () => this.updateAdvancedSettings()
    );
    this.actionsSettingsSubscription = this.actionsSettings.valueChanges.subscribe(
      () => this.updateActionSettings()
    );
  }

  private buildHeader() {
    this.headerOptions.length = 0;
    if (this.displayData) {
      this.headerOptions.push(
        {
          name: this.translate.instant('widget-config.data'),
          value: 'data'
        }
      );
    }
    if (this.displayAppearance) {
      this.headerOptions.push(
        {
          name: this.translate.instant('widget-config.appearance'),
          value: 'appearance'
        }
      );
    }
    this.headerOptions.push(
      {
        name: this.translate.instant('widget-config.widget-card'),
        value: 'card'
      }
    );
    this.headerOptions.push(
      {
        name: this.translate.instant('widget-config.actions'),
        value: 'actions'
      }
    );
    this.headerOptions.push(
      {
        name: this.translate.instant('widget-config.layout'),
        value: 'layout'
      }
    );
    if (!this.selectedOption || !this.headerOptions.find(o => o.value === this.selectedOption)) {
      this.selectedOption = this.headerOptions[0].value;
    }
  }

  private buildForms() {
    this.dataSettings = this.fb.group({});
    this.targetDeviceSettings = this.fb.group({});
    this.advancedSettings = this.fb.group({});
    if (widgetTypeCanHaveTimewindow(this.widgetType)) {
      this.dataSettings.addControl('timewindowConfig', this.fb.control({
        useDashboardTimewindow: true,
        displayTimewindow: true,
        timewindow: null,
        timewindowStyle: null
      }));
    }
    if (this.widgetType === widgetType.alarm) {
      this.dataSettings.addControl('alarmFilterConfig', this.fb.control(null));
    }
    if (this.modelValue.isDataEnabled) {
      if (this.widgetType !== widgetType.rpc &&
        this.widgetType !== widgetType.alarm &&
        this.widgetType !== widgetType.static) {
        this.dataSettings.addControl('datasources', this.fb.control(null));
      } else if (this.widgetType === widgetType.rpc) {
        this.targetDeviceSettings.addControl('targetDevice',
          this.fb.control(null, []));
      } else if (this.widgetType === widgetType.alarm) {
        this.dataSettings.addControl('alarmSource', this.fb.control(null));
      }
    }
    this.advancedSettings.addControl('settings',
      this.fb.control(null, []));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: WidgetConfigComponentData): void {
    this.modelValue = value;
    this.widgetType = this.modelValue?.widgetType;
    this.widgetConfigMode = this.modelValue?.hasBasicMode ?
      (this.modelValue?.config?.configMode || WidgetConfigMode.advanced) : WidgetConfigMode.advanced;
    this.setupConfig(this.isAdd);
  }

  setWidgetConfigMode(widgetConfigMode: WidgetConfigMode) {
    if (this.modelValue?.hasBasicMode && this.widgetConfigMode !== widgetConfigMode) {
      this.widgetConfigMode = widgetConfigMode;
      this.modelValue.config.configMode = widgetConfigMode;
      if (this.hasBasicModeDirective) {
        this.setupConfig();
      }
      this.propagateChange(this.modelValue);
    }
  }

  private setupConfig(isAdd = false) {
    if (this.modelValue) {
      this.destroyBasicModeComponent();
      this.removeChangeSubscriptions();
      if (this.hasBasicModeDirective && this.widgetConfigMode === WidgetConfigMode.basic) {
        this.setupBasicModeConfig(isAdd);
      } else {
        this.setupDefaultConfig();
      }
    }
  }

  private setupBasicModeConfig(isAdd = false) {
    const componentType = this.widgetService.getBasicWidgetSettingsComponentBySelector(this.modelValue.basicModeDirective);
    if (!componentType) {
      this.basicModeDirectiveError = this.translate.instant('widget-config.settings-component-not-found',
        {selector: this.modelValue.basicModeDirective});
    } else {
      this.createBasicModeComponentTimeout = setTimeout(() => {
        this.createBasicModeComponentTimeout = null;
        this.basicModeComponentRef = this.basicModeContainer.createComponent(componentType);
        this.basicModeComponent = this.basicModeComponentRef.instance;
        this.basicModeComponent.isAdd = isAdd;
        this.basicModeComponent.widgetConfig = this.modelValue;
        this.basicModeComponentChangeSubscription = this.basicModeComponent.widgetConfigChanged.subscribe((data) => {
          this.modelValue = data;
          this.propagateChange(this.modelValue);
          this.cd.markForCheck();
        });
        if (this.basicModeComponent$) {
          this.basicModeComponent$.next(this.basicModeComponent);
          this.basicModeComponent$.complete();
          this.basicModeComponent$ = null;
        }
        this.cd.markForCheck();
      }, 0);
    }
  }

  private destroyBasicModeComponent() {
    this.basicModeDirectiveError = null;
    if (this.basicModeComponentChangeSubscription) {
      this.basicModeComponentChangeSubscription.unsubscribe();
      this.basicModeComponentChangeSubscription = null;
    }
    if (this.createBasicModeComponentTimeout) {
      clearTimeout(this.createBasicModeComponentTimeout);
      this.createBasicModeComponentTimeout = null;
    }
    if (this.basicModeComponentRef) {
      this.basicModeComponentRef.destroy();
      this.basicModeComponentRef = null;
      this.basicModeComponent = null;
    }
    if (this.basicModeContainer) {
      this.basicModeContainer.clear();
    }
  }

  private setupDefaultConfig() {
    if (this.defaultConfigFormsType !== this.widgetType) {
      this.defaultConfigFormsType = this.widgetType;
      this.buildForms();
    }
    this.buildHeader();
    const config = this.modelValue.config;
    const layout = this.modelValue.layout;
    if (config) {
      const displayWidgetTitle = isDefined(config.showTitle) ? config.showTitle : false;
      this.widgetSettings.patchValue({
          title: config.title,
          titleFont: config.titleFont,
          titleColor: config.titleColor,
          showTitleIcon: isDefined(config.showTitleIcon) && displayWidgetTitle ? config.showTitleIcon : false,
          titleIcon: isDefined(config.titleIcon) ? config.titleIcon : '',
          iconColor: isDefined(config.iconColor) ? config.iconColor : 'rgba(0, 0, 0, 0.87)',
          iconSize: isDefined(config.iconSize) ? config.iconSize : '24px',
          titleTooltip: isDefined(config.titleTooltip) ? config.titleTooltip : '',
          showTitle: displayWidgetTitle,
          dropShadow: isDefined(config.dropShadow) ? config.dropShadow : true,
          enableFullscreen: isDefined(config.enableFullscreen) ? config.enableFullscreen : true,
          backgroundColor: config.backgroundColor,
          color: config.color,
          padding: config.padding,
          margin: config.margin,
          borderRadius: config.borderRadius,
          widgetStyle: isDefined(config.widgetStyle) ? config.widgetStyle : {},
          widgetCss: isDefined(config.widgetCss) ? config.widgetCss : '',
          titleStyle: isDefined(config.titleStyle) ? config.titleStyle : {
            fontSize: '16px',
            fontWeight: 400
          },
          pageSize: isDefined(config.pageSize) ? config.pageSize : 1024,
          units: config.units,
          decimals: config.decimals,
          noDataDisplayMessage: isDefined(config.noDataDisplayMessage) ? config.noDataDisplayMessage : ''
        },
        {emitEvent: false}
      );
      this.updateWidgetSettingsEnabledState();
      this.actionsSettings.patchValue(
        {
          actions: config.actions || {}
        },
        {emitEvent: false}
      );
      if (widgetTypeCanHaveTimewindow(this.widgetType)) {
        const useDashboardTimewindow = isDefined(config.useDashboardTimewindow) ?
          config.useDashboardTimewindow : true;
        this.dataSettings.get('timewindowConfig').patchValue({
          useDashboardTimewindow,
          displayTimewindow: isDefined(config.displayTimewindow) ?
            config.displayTimewindow : true,
          timewindow: isDefinedAndNotNull(config.timewindow)
            ? config.timewindow
            : initModelFromDefaultTimewindow(null, this.widgetType === widgetType.latest, this.onlyHistoryTimewindow(),
              this.timeService, this.widgetType === widgetType.timeseries),
          timewindowStyle: config.timewindowStyle
        }, {emitEvent: false});
      }
      if (this.modelValue.isDataEnabled) {
        if (this.widgetType !== widgetType.rpc &&
          this.widgetType !== widgetType.alarm &&
          this.widgetType !== widgetType.static) {
          this.dataSettings.patchValue({ datasources: config.datasources},
            {emitEvent: false});
        } else if (this.widgetType === widgetType.rpc) {
          const targetDevice: TargetDevice = config.targetDevice;
          this.targetDeviceSettings.patchValue({
            targetDevice
          }, {emitEvent: false});
        } else if (this.widgetType === widgetType.alarm) {
          this.dataSettings.patchValue(
            { alarmFilterConfig: isDefined(config.alarmFilterConfig) ?
                config.alarmFilterConfig :
                { statusList: [AlarmSearchStatus.ACTIVE], searchPropagatedAlarms: true },
              alarmSource: config.alarmSource }, {emitEvent: false}
          );
        }
      }

      this.updateAdvancedForm(config.settings);

      if (layout) {
        this.layoutSettings.patchValue(
          {
            resizable: isDefined(layout.resizable) ? layout.resizable : true,
            preserveAspectRatio: layout.preserveAspectRatio,
            mobileOrder: layout.mobileOrder,
            mobileHeight: layout.mobileHeight,
            mobileHide: layout.mobileHide,
            desktopHide: layout.desktopHide
          },
          {emitEvent: false}
        );
      } else {
        this.layoutSettings.patchValue(
          {
            resizable: true,
            preserveAspectRatio: false,
            mobileOrder: null,
            mobileHeight: null,
            mobileHide: false,
            desktopHide: false
          },
          {emitEvent: false}
        );
      }
      this.updateLayoutEnabledState();
    }
    this.createChangeSubscriptions();
  }

  private updateWidgetSettingsEnabledState() {
    const showTitle: boolean = this.widgetSettings.get('showTitle').value;
    const showTitleIcon: boolean = this.widgetSettings.get('showTitleIcon').value;
    if (showTitle) {
      this.widgetSettings.get('title').enable({emitEvent: false});
      this.widgetSettings.get('titleFont').enable({emitEvent: false});
      this.widgetSettings.get('titleColor').enable({emitEvent: false});
      this.widgetSettings.get('titleTooltip').enable({emitEvent: false});
      this.widgetSettings.get('titleStyle').enable({emitEvent: false});
      this.widgetSettings.get('showTitleIcon').enable({emitEvent: false});
    } else {
      this.widgetSettings.get('title').disable({emitEvent: false});
      this.widgetSettings.get('titleFont').disable({emitEvent: false});
      this.widgetSettings.get('titleColor').disable({emitEvent: false});
      this.widgetSettings.get('titleTooltip').disable({emitEvent: false});
      this.widgetSettings.get('titleStyle').disable({emitEvent: false});
      this.widgetSettings.get('showTitleIcon').disable({emitEvent: false});
    }
    if (showTitle && showTitleIcon) {
      this.widgetSettings.get('titleIcon').enable({emitEvent: false});
      this.widgetSettings.get('iconColor').enable({emitEvent: false});
      this.widgetSettings.get('iconSize').enable({emitEvent: false});
    } else {
      this.widgetSettings.get('titleIcon').disable({emitEvent: false});
      this.widgetSettings.get('iconColor').disable({emitEvent: false});
      this.widgetSettings.get('iconSize').disable({emitEvent: false});
    }
  }

  private updateLayoutEnabledState() {
    const resizable: boolean = this.layoutSettings.get('resizable').value;
    if (resizable) {
      this.layoutSettings.get('preserveAspectRatio').enable({emitEvent: false});
    } else {
      this.layoutSettings.get('preserveAspectRatio').disable({emitEvent: false});
    }
  }

  private updateAdvancedForm(settings?: any) {
    const dynamicFormData: DynamicFormData = {};
    dynamicFormData.model = settings || {};
    if (this.modelValue.settingsForm?.length) {
      dynamicFormData.settingsForm = this.modelValue.settingsForm;
    } else {
      dynamicFormData.settingsForm = [];
    }
    dynamicFormData.settingsDirective = this.modelValue.settingsDirective;
    this.advancedSettings.patchValue({ settings: dynamicFormData }, {emitEvent: false});
  }

  private updateDataSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        let data = this.dataSettings.value;
        if (data.timewindowConfig) {
          const timewindowConfig: TimewindowConfigData = data.timewindowConfig;
          data = {...data, ...timewindowConfig};
          delete data.timewindowConfig;
        }
        Object.assign(this.modelValue.config, data);
      }
      this.propagateChange(this.modelValue);
    }
  }

  private updateTargetDeviceSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        this.modelValue.config.targetDevice = this.targetDeviceSettings.get('targetDevice').value;
      }
      this.propagateChange(this.modelValue);
    }
  }

  private updateWidgetSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        Object.assign(this.modelValue.config, this.widgetSettings.value);
      }
      this.propagateChange(this.modelValue);
    }
  }

  private updateLayoutSettings() {
    if (this.modelValue) {
      if (this.modelValue.layout) {
        Object.assign(this.modelValue.layout, this.layoutSettings.value);
      }
      this.propagateChange(this.modelValue);
    }
  }

  private updateAdvancedSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        this.modelValue.config.settings = this.advancedSettings.get('settings').value?.model;
      }
      this.propagateChange(this.modelValue);
    }
  }

  private updateActionSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        this.modelValue.config.actions = this.actionsSettings.get('actions').value;
      }
      this.propagateChange(this.modelValue);
    }
  }

  public get hasBasicModeDirective(): boolean {
    return this.modelValue?.basicModeDirective?.length > 0;
  }

  public get useDefinedBasicModeDirective(): boolean {
    return this.modelValue?.basicModeDirective?.length && !this.basicModeDirectiveError;
  }

  public get displayData(): boolean {
    return !this.modelValue?.typeParameters?.hideDataTab && this.widgetType !== widgetType.static;
  }

  public get displayAppearance(): boolean {
    return this.displayAppearanceDataSettings || this.displayAdvancedAppearance;
  }

  public get displayAdvancedAppearance(): boolean {
    return !!this.modelValue && (!!this.modelValue.settingsForm && !!this.modelValue.settingsForm.length ||
        !!this.modelValue.settingsDirective && !!this.modelValue.settingsDirective.length);
  }

  public get displayTimewindowConfig(): boolean {
    if (widgetTypeHasTimewindow(this.widgetType)) {
      return true;
    } else if (this.widgetType === widgetType.latest) {
      const widgetDefinition = findWidgetModelDefinition(this.widget);
      if (widgetDefinition) {
        return widgetDefinition.hasTimewindow(this.widget);
      } else {
        const datasources = this.dataSettings.get('datasources').value;
        return datasourcesHasAggregation(datasources);
      }
    }
  }

  public get displayLimits(): boolean {
    return this.widgetType !== widgetType.rpc && this.widgetType !== widgetType.alarm &&
      this.modelValue?.isDataEnabled && !this.modelValue?.typeParameters?.singleEntity;
  }

  public get displayAppearanceDataSettings(): boolean {
    return !this.modelValue?.typeParameters?.hideDataSettings && (this.displayUnitsConfig || this.displayNoDataDisplayMessageConfig);
  }

  public get displayUnitsConfig(): boolean {
    return this.widgetType === widgetType.latest || this.widgetType === widgetType.timeseries;
  }

  public get displayNoDataDisplayMessageConfig(): boolean {
    return this.widgetType !== widgetType.static && !this.modelValue?.typeParameters?.processNoDataByWidget;
  }

  public onlyHistoryTimewindow(): boolean {
    if (this.widgetType === widgetType.latest) {
      const datasources = this.dataSettings.get('datasources').value;
      return datasourcesHasOnlyComparisonAggregation(datasources);
    } else {
      return false;
    }
  }

  public generateDataKey(chip: any, type: DataKeyType, dataKeySettingsForm: FormProperty[],
                         isLatestDataKey: boolean, dataKeySettingsFunction: DataKeySettingsFunction): DataKey {
    if (isObject(chip)) {
      (chip as DataKey)._hash = Math.random();
      return chip;
    } else {
      let label: string = chip;
      if (type === DataKeyType.alarm || type === DataKeyType.entityField) {
        const keyField = type === DataKeyType.alarm ? alarmFields[label] : entityFields[chip];
        if (keyField) {
          label = this.translate.instant(keyField.name);
        }
      }
      const datasources = this.widgetType === widgetType.alarm ? [this.modelValue.config.alarmSource] : this.modelValue.config.datasources;
      label = genNextLabel(label, datasources);
      const result: DataKey = {
        name: chip,
        type,
        label,
        color: this.genNextColor(),
        settings: {},
        _hash: Math.random()
      };
      if (type === DataKeyType.function) {
        result.name = 'f(x)';
        result.funcBody = this.utils.getPredefinedFunctionBody(chip);
        if (!result.funcBody) {
          result.funcBody = 'return prevValue + 1;';
        }
      } else if (type === DataKeyType.count) {
        result.name = 'count';
      }
      if (dataKeySettingsForm?.length) {
        result.settings = defaultFormProperties(dataKeySettingsForm);
      } else if (dataKeySettingsFunction) {
        const settings = dataKeySettingsFunction(result, isLatestDataKey);
        if (settings) {
          result.settings = settings;
        }
      }
      return result;
    }
  }

  private genNextColor(): string {
    let i = 0;
    const datasources = this.widgetType === widgetType.alarm ? [this.modelValue.config.alarmSource] : this.modelValue.config.datasources;
    if (datasources) {
      datasources.forEach((datasource) => {
        if (datasource && (datasource.dataKeys || datasource.latestDataKeys)) {
          i += ((datasource.dataKeys ? datasource.dataKeys.length : 0) +
            (datasource.latestDataKeys ? datasource.latestDataKeys.length : 0));
        }
      });
    }
    return this.utils.getMaterialColor(i);
  }

  private createEntityAlias(alias: string, allowedEntityTypes: Array<EntityType>): Observable<EntityAlias> {
    const singleEntityAlias: EntityAlias = {id: null, alias, filter: {resolveMultiple: false}};
    return this.dialog.open<EntityAliasDialogComponent, EntityAliasDialogData,
      EntityAlias>(EntityAliasDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true,
        allowedEntityTypes,
        entityAliases: this.dashboard.configuration.entityAliases,
        alias: singleEntityAlias
      }
    }).afterClosed().pipe(
      tap((entityAlias) => {
        if (entityAlias) {
          this.dashboard.configuration.entityAliases[entityAlias.id] = entityAlias;
          this.aliasController.updateEntityAliases(this.dashboard.configuration.entityAliases);
        }
      })
    );
  }

  private editEntityAlias(alias: EntityAlias, allowedEntityTypes: Array<EntityType>): Observable<EntityAlias> {
    return this.dialog.open<EntityAliasDialogComponent, EntityAliasDialogData,
      EntityAlias>(EntityAliasDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: false,
        allowedEntityTypes,
        entityAliases: this.dashboard.configuration.entityAliases,
        alias: deepClone(alias)
      }
    }).afterClosed().pipe(
      tap((entityAlias) => {
        if (entityAlias) {
          this.dashboard.configuration.entityAliases[entityAlias.id] = entityAlias;
          this.aliasController.updateEntityAliases(this.dashboard.configuration.entityAliases);
        }
      })
    );
  }

  private createFilter(filter: string): Observable<Filter> {
    const singleFilter: Filter = {id: null, filter, keyFilters: [], editable: true};
    return this.dialog.open<FilterDialogComponent, FilterDialogData,
      Filter>(FilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true,
        filters: this.dashboard.configuration.filters,
        filter: singleFilter
      }
    }).afterClosed().pipe(
      tap((result) => {
        if (result) {
          this.dashboard.configuration.filters[result.id] = result;
          this.aliasController.updateFilters(this.dashboard.configuration.filters);
        }
      })
    );
  }

  private fetchEntityKeysForDevice(deviceId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
      const entityFilter = singleEntityFilterFromDeviceId(deviceId);
      return this.entityService.getEntityKeysByEntityFilter(
        entityFilter,
        dataKeyTypes, [EntityType.DEVICE],
        {ignoreLoading: true, ignoreErrors: true}
      ).pipe(
        catchError(() => of([]))
      );
  }

  private fetchEntityKeys(entityAliasId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    return this.aliasController.getAliasInfo(entityAliasId).pipe(
      mergeMap((aliasInfo) => this.entityService.getEntityKeysByEntityFilter(
          aliasInfo.entityFilter,
          dataKeyTypes,  [],
          {ignoreLoading: true, ignoreErrors: true}
        ).pipe(
          catchError(() => of([]))
        )),
      catchError(() => of([] as Array<DataKey>))
    );
  }

  private fetchDashboardStates(query: string): Array<string> {
    const stateIds = Object.keys(this.dashboard.configuration.states);
    const result = query ? stateIds.filter(this.createFilterForDashboardState(query)) : stateIds;
    if (result && result.length) {
      return result;
    } else {
      return [query];
    }
  }

  private fetchCellClickColumns(): Array<CellClickColumnInfo> {
    if (this.modelValue) {
      const configuredColumns = new Array<CellClickColumnInfo>();
      if (this.modelValue.config?.datasources[0]?.dataKeys?.length) {
        const {
          displayEntityLabel,
          displayEntityName,
          displayEntityType,
          entityNameColumnTitle,
          entityLabelColumnTitle
        } = this.modelValue.config.settings;
        const displayEntitiesArray = [];
        if (isDefined(displayEntityName)) {
          const displayName = entityNameColumnTitle ? entityNameColumnTitle : 'entityName';
          displayEntitiesArray.push({name: displayName, label: displayName});
        }
        if (isDefined(displayEntityLabel)) {
          const displayLabel = entityLabelColumnTitle ? entityLabelColumnTitle : 'entityLabel';
          displayEntitiesArray.push({name: displayLabel, label: displayLabel});
        }
        if (isDefined(displayEntityType)) {
          displayEntitiesArray.push({name: 'entityType', label: 'entityType'});
        }
        configuredColumns.push(...displayEntitiesArray, ...this.keysToCellClickColumns(this.modelValue.config.datasources[0].dataKeys));
      }
      if (this.modelValue.config?.alarmSource?.dataKeys?.length) {
        configuredColumns.push(...this.keysToCellClickColumns(this.modelValue.config.alarmSource.dataKeys));
      }
      return configuredColumns;
    }
  }

  private keysToCellClickColumns(dataKeys: Array<DataKey>): Array<CellClickColumnInfo> {
    const result: Array<CellClickColumnInfo> = [];
    for (const dataKey of dataKeys) {
      result.push({
        name: dataKey.name,
        label: dataKey?.label
      });
    }
    return result;
  }

  private createFilterForDashboardState(query: string): (stateId: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return stateId => stateId.toLowerCase().indexOf(lowercaseQuery) === 0;
  }

  public validate(c: UntypedFormControl): Observable<ValidationErrors | null> {
    const basicComponentMode = this.hasBasicModeDirective && this.widgetConfigMode === WidgetConfigMode.basic;
    let comp$: Observable<IBasicWidgetConfigComponent>;
    if (basicComponentMode) {
      if (this.basicModeComponent) {
        comp$ = of(this.basicModeComponent);
      } else {
        if (this.useDefinedBasicModeDirective) {
          this.basicModeComponent$ = new Subject();
          comp$ = this.basicModeComponent$;
        } else {
          comp$ = of(null);
        }
      }
    } else {
      comp$ = of(null);
    }
    return comp$.pipe(
      map((comp) => this.doValidate(basicComponentMode, comp))
    );
  }

  private doValidate(basicComponentMode: boolean, basicModeComponent?: IBasicWidgetConfigComponent): ValidationErrors | null {
    if (basicComponentMode) {
      if (!basicModeComponent || !basicModeComponent.validateConfig()) {
        return {
          basicWidgetConfig: {
            valid: false
          }
        };
      }
    } else {
      if (!this.dataSettings.valid) {
        return {
          dataSettings: {
            valid: false
          }
        };
      } else if (!this.widgetSettings.valid) {
        return {
          widgetSettings: {
            valid: false
          }
        };
      } else if (!this.layoutSettings.valid) {
        return {
          widgetSettings: {
            valid: false
          }
        };
      } else if (!this.advancedSettings.valid) {
        return {
          advancedSettings: {
            valid: false
          }
        };
      }
    }
    if (this.modelValue) {
      const config = this.modelValue.config;
      if (this.widgetType === widgetType.rpc && this.modelValue.isDataEnabled) {
        if ((!this.widgetEditMode && !this.modelValue?.typeParameters.targetDeviceOptional) && !targetDeviceValid(config.targetDevice)) {
          return {
            targetDevice: {
              valid: false
            }
          };
        }
      }
    }
    return null;
  }

}
