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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  DataKey,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  GroupInfo,
  JsonSchema,
  JsonSettingsSchema,
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
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { deepClone, genNextLabel, isDefined, isObject } from '@app/core/utils';
import { alarmFields, AlarmSearchStatus } from '@shared/models/alarm.models';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityAlias } from '@shared/models/alias.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of, Subscription } from 'rxjs';
import { WidgetConfigCallbacks } from '@home/components/widget/widget-config.component.models';
import {
  EntityAliasDialogComponent,
  EntityAliasDialogData
} from '@home/components/alias/entity-alias-dialog.component';
import { catchError, mergeMap, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { EntityService } from '@core/http/entity.service';
import { JsonFormComponentData } from '@shared/components/json-form/json-form-component.models';
import { WidgetActionsData } from './action/manage-widget-actions.component.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { entityFields } from '@shared/models/entity.models';
import { Filter } from '@shared/models/query/query.models';
import { FilterDialogComponent, FilterDialogData } from '@home/components/filter/filter-dialog.component';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';

const emptySettingsSchema: JsonSchema = {
  type: 'object',
  properties: {}
};
const emptySettingsGroupInfoes: GroupInfo[] = [];
const defaultSettingsForm = [
  '*'
];

@Component({
  selector: 'tb-widget-config',
  templateUrl: './widget-config.component.html',
  styleUrls: ['./widget-config.component.scss', 'widget-config.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => WidgetConfigComponent),
      multi: true,
    }
  ]
})
export class WidgetConfigComponent extends PageComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  widgetTypes = widgetType;

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

  @Input() disabled: boolean;

  widgetType: widgetType;

  widgetConfigCallbacks: WidgetConfigCallbacks = {
    createEntityAlias: this.createEntityAlias.bind(this),
    createFilter: this.createFilter.bind(this),
    generateDataKey: this.generateDataKey.bind(this),
    fetchEntityKeys: this.fetchEntityKeys.bind(this),
    fetchDashboardStates: this.fetchDashboardStates.bind(this)
  };

  widgetEditMode = this.utils.widgetEditMode;

  modelValue: WidgetConfigComponentData;

  private propagateChange = null;

  headerOptions: ToggleHeaderOption[] = [];
  selectedOption: string;

  public dataSettings: UntypedFormGroup;
  public targetDeviceSettings: UntypedFormGroup;
  public widgetSettings: UntypedFormGroup;
  public layoutSettings: UntypedFormGroup;
  public advancedSettings: UntypedFormGroup;
  public actionsSettings: UntypedFormGroup;

  private dataSettingsChangesSubscription: Subscription;
  private targetDeviceSettingsSubscription: Subscription;
  private widgetSettingsSubscription: Subscription;
  private layoutSettingsSubscription: Subscription;
  private advancedSettingsSubscription: Subscription;
  private actionsSettingsSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private entityService: EntityService,
              private dialog: MatDialog,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.dataSettings = this.fb.group({});
    this.targetDeviceSettings = this.fb.group({});
    this.advancedSettings = this.fb.group({});
    this.widgetSettings = this.fb.group({
      title: [null, []],
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
      widgetStyle: [null, []],
      widgetCss: [null, []],
      titleStyle: [null, []],
      pageSize: [1024, [Validators.min(1), Validators.pattern(/^\d*$/)]],
      units: [null, []],
      decimals: [null, [Validators.min(0), Validators.max(15), Validators.pattern(/^\d*$/)]],
      noDataDisplayMessage: [null, []]
    });

    this.widgetSettings.get('showTitle').valueChanges.subscribe(() => {
      this.updateWidgetSettingsEnabledState();
    });
    this.widgetSettings.get('showTitleIcon').valueChanges.subscribe(() => {
      this.updateWidgetSettingsEnabledState();
    });

    this.layoutSettings = this.fb.group({
      mobileOrder: [null, [Validators.pattern(/^-?[0-9]+$/)]],
      mobileHeight: [null, [Validators.min(1), Validators.pattern(/^\d*$/)]],
      mobileHide: [false],
      desktopHide: [false]
    });
    this.actionsSettings = this.fb.group({
      actionsData: [null, []]
    });
  }

  ngOnDestroy(): void {
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
    if (this.widgetType !== widgetType.static) {
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
        name: this.translate.instant('widget-config.mobile'),
        value: 'mobile'
      }
    );
    this.selectedOption = this.headerOptions[0].value;
  }

  private buildForms() {
    this.dataSettings = this.fb.group({});
    this.targetDeviceSettings = this.fb.group({});
    this.advancedSettings = this.fb.group({});
    if (this.widgetType === widgetType.timeseries || this.widgetType === widgetType.alarm || this.widgetType === widgetType.latest) {
      this.dataSettings.addControl('useDashboardTimewindow', this.fb.control(true));
      this.dataSettings.addControl('displayTimewindow', this.fb.control({value: true, disabled: true}));
      this.dataSettings.addControl('timewindow', this.fb.control({value: null, disabled: true}));
      this.dataSettings.get('useDashboardTimewindow').valueChanges.subscribe(() => {
        this.updateDataSettingsEnabledState();
      });
      if (this.widgetType === widgetType.alarm) {
        this.dataSettings.addControl('alarmFilterConfig', this.fb.control(null));
      }
    }
    if (this.modelValue.isDataEnabled) {
      if (this.widgetType !== widgetType.rpc &&
        this.widgetType !== widgetType.alarm &&
        this.widgetType !== widgetType.static) {
        this.dataSettings.addControl('datasources', this.fb.control(null));
      } else if (this.widgetType === widgetType.rpc) {
        this.targetDeviceSettings.addControl('targetDeviceAliasId',
          this.fb.control(null,
            this.widgetEditMode ? [] : [Validators.required]));
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
    this.removeChangeSubscriptions();
    if (this.modelValue) {
      if (this.widgetType !== this.modelValue.widgetType) {
        this.widgetType = this.modelValue.widgetType;
        this.buildForms();
      }
      this.buildHeader();
      const config = this.modelValue.config;
      const layout = this.modelValue.layout;
      if (config) {
        const displayWidgetTitle = isDefined(config.showTitle) ? config.showTitle : false;
        this.widgetSettings.patchValue({
            title: config.title,
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
        const actionsData: WidgetActionsData = {
          actionsMap: config.actions || {},
          actionSources: this.modelValue.actionSources || {}
        };
        this.actionsSettings.patchValue(
          {
            actionsData
          },
          {emitEvent: false}
        );
        if (this.widgetType === widgetType.timeseries || this.widgetType === widgetType.alarm || this.widgetType === widgetType.latest) {
          const useDashboardTimewindow = isDefined(config.useDashboardTimewindow) ?
            config.useDashboardTimewindow : true;
          this.dataSettings.patchValue(
            { useDashboardTimewindow }, {emitEvent: false}
          );
          this.dataSettings.patchValue(
            { displayTimewindow: isDefined(config.displayTimewindow) ?
                config.displayTimewindow : true }, {emitEvent: false}
          );
          this.dataSettings.patchValue(
            { timewindow: config.timewindow }, {emitEvent: false}
          );
          this.updateDataSettingsEnabledState();
        }
        if (this.modelValue.isDataEnabled) {
          if (this.widgetType !== widgetType.rpc &&
            this.widgetType !== widgetType.alarm &&
            this.widgetType !== widgetType.static) {
            this.dataSettings.patchValue({ datasources: config.datasources},
              {emitEvent: false});
          } else if (this.widgetType === widgetType.rpc) {
            let targetDeviceAliasId: string;
            if (config.targetDeviceAliasIds && config.targetDeviceAliasIds.length > 0) {
              const aliasId = config.targetDeviceAliasIds[0];
              const entityAliases = this.aliasController.getEntityAliases();
              if (entityAliases[aliasId]) {
                targetDeviceAliasId = entityAliases[aliasId].id;
              } else {
                targetDeviceAliasId = null;
              }
            } else {
              targetDeviceAliasId = null;
            }
            this.targetDeviceSettings.patchValue({
              targetDeviceAliasId
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

        this.updateSchemaForm(config.settings);

        if (layout) {
          this.layoutSettings.patchValue(
            {
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
              mobileOrder: null,
              mobileHeight: null,
              mobileHide: false,
              desktopHide: false
            },
            {emitEvent: false}
          );
        }
      }
      this.createChangeSubscriptions();
    }
  }

  private updateWidgetSettingsEnabledState() {
    const showTitle: boolean = this.widgetSettings.get('showTitle').value;
    const showTitleIcon: boolean = this.widgetSettings.get('showTitleIcon').value;
    if (showTitle) {
      this.widgetSettings.get('title').enable({emitEvent: false});
      this.widgetSettings.get('titleTooltip').enable({emitEvent: false});
      this.widgetSettings.get('titleStyle').enable({emitEvent: false});
      this.widgetSettings.get('showTitleIcon').enable({emitEvent: false});
    } else {
      this.widgetSettings.get('title').disable({emitEvent: false});
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

  private updateDataSettingsEnabledState() {
    const useDashboardTimewindow: boolean = this.dataSettings.get('useDashboardTimewindow').value;
    if (useDashboardTimewindow) {
      this.dataSettings.get('displayTimewindow').disable({emitEvent: false});
      this.dataSettings.get('timewindow').disable({emitEvent: false});
    } else {
      this.dataSettings.get('displayTimewindow').enable({emitEvent: false});
      this.dataSettings.get('timewindow').enable({emitEvent: false});
    }
  }

  private updateSchemaForm(settings?: any) {
    const widgetSettingsFormData: JsonFormComponentData = {};
    if (this.modelValue.settingsSchema && this.modelValue.settingsSchema.schema) {
      widgetSettingsFormData.schema = this.modelValue.settingsSchema.schema;
      widgetSettingsFormData.form = this.modelValue.settingsSchema.form || deepClone(defaultSettingsForm);
      widgetSettingsFormData.groupInfoes = this.modelValue.settingsSchema.groupInfoes;
      widgetSettingsFormData.model = settings;
    } else {
      widgetSettingsFormData.schema = deepClone(emptySettingsSchema);
      widgetSettingsFormData.form = deepClone(defaultSettingsForm);
      widgetSettingsFormData.groupInfoes = deepClone(emptySettingsGroupInfoes);
      widgetSettingsFormData.model = settings || {};
    }
    widgetSettingsFormData.settingsDirective = this.modelValue.settingsDirective;
    this.advancedSettings.patchValue({ settings: widgetSettingsFormData }, {emitEvent: false});
  }

  private updateDataSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        Object.assign(this.modelValue.config, this.dataSettings.value);
      }
      this.propagateChange(this.modelValue);
    }
  }

  private updateTargetDeviceSettings() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        const targetDeviceAliasId: string = this.targetDeviceSettings.get('targetDeviceAliasId').value;
        if (targetDeviceAliasId) {
          this.modelValue.config.targetDeviceAliasIds = [targetDeviceAliasId];
        } else {
          this.modelValue.config.targetDeviceAliasIds = [];
        }
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
        this.modelValue.config.actions = (this.actionsSettings.get('actionsData').value as WidgetActionsData).actionsMap;
      }
      this.propagateChange(this.modelValue);
    }
  }

  public get displayAppearance(): boolean {
    return this.displayAppearanceDataSettings || this.displayAdvancedAppearance;
  }

  public get displayAdvancedAppearance(): boolean {
    return !!this.modelValue && (!!this.modelValue.settingsSchema && !!this.modelValue.settingsSchema.schema ||
        !!this.modelValue.settingsDirective && !!this.modelValue.settingsDirective.length);
  }

  public get displayTimewindowConfig(): boolean {
    if (this.widgetType === widgetType.timeseries || this.widgetType === widgetType.alarm) {
      return true;
    } else if (this.widgetType === widgetType.latest) {
      const datasources = this.dataSettings.get('datasources').value;
      return datasourcesHasAggregation(datasources);
    }
  }

  public get displayLimits(): boolean {
    return this.widgetType !== widgetType.rpc && this.widgetType !== widgetType.alarm &&
      this.modelValue?.isDataEnabled && !this.modelValue?.typeParameters?.singleEntity;
  }

  public get displayAppearanceDataSettings(): boolean {
    return this.displayUnitsConfig || this.displayNoDataDisplayMessageConfig;
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

  public generateDataKey(chip: any, type: DataKeyType, datakeySettingsSchema: JsonSettingsSchema): DataKey {
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
      if (datakeySettingsSchema && isDefined(datakeySettingsSchema.schema)) {
        result.settings = this.utils.generateObjectFromJsonSchema(datakeySettingsSchema.schema);
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

  private fetchEntityKeys(entityAliasId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    return this.aliasController.getAliasInfo(entityAliasId).pipe(
      mergeMap((aliasInfo) => this.entityService.getEntityKeysByEntityFilter(
          aliasInfo.entityFilter,
          dataKeyTypes,
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

  private createFilterForDashboardState(query: string): (stateId: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return stateId => stateId.toLowerCase().indexOf(lowercaseQuery) === 0;
  }

  public validate(c: UntypedFormControl) {
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
    } else if (!this.advancedSettings.valid || (this.displayAdvancedAppearance && !this.modelValue.config.settings)) {
      return {
        advancedSettings: {
          valid: false
        }
      };
    } else if (this.modelValue) {
      const config = this.modelValue.config;
      if (this.widgetType === widgetType.rpc && this.modelValue.isDataEnabled) {
        if (!this.widgetEditMode && (!config.targetDeviceAliasIds || !config.targetDeviceAliasIds.length)) {
          return {
            targetDeviceAliasIds: {
              valid: false
            }
          };
        }
      }
    }
    return null;
  }
}
