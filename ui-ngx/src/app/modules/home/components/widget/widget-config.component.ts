///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  Datasource,
  LegendConfig,
  WidgetActionDescriptor,
  WidgetActionSource, WidgetConfigSettings,
  widgetType,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { deepClone, isDefined } from '@app/core/utils';
import { Timewindow } from '@shared/models/time/time.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityAlias } from '@shared/models/alias.models';

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
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => WidgetConfigComponent),
      multi: true,
    }
  ]
})
export class WidgetConfigComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  widgetTypes = widgetType;

  @Input()
  forceExpandDatasources: boolean;

  @Input()
  isDataEnabled: boolean;

  @Input()
  widgetType: widgetType;

  @Input()
  typeParameters: WidgetTypeParameters;

  @Input()
  actionSources: {[key: string]: WidgetActionSource};

  @Input()
  aliasController: IAliasController;

  @Input()
  widgetSettingsSchema: any;

  @Input()
  dataKeySettingsSchema: any;

  @Input()
  functionsOnly: boolean;

  @Input() disabled: boolean;

  selectedTab: number;
  title: string;
  showTitleIcon: boolean;
  titleIcon: string;
  iconColor: string;
  iconSize: string;
  showTitle: boolean;
  dropShadow: boolean;
  enableFullscreen: boolean;
  backgroundColor: string;
  color: string;
  padding: string;
  margin: string;
  widgetStyle: string;
  titleStyle: string;
  units: string;
  decimals: number;
  useDashboardTimewindow: boolean;
  displayTimewindow: boolean;
  timewindow: Timewindow;
  showLegend: boolean;
  legendConfig: LegendConfig;
  actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  datasources: Array<Datasource>;
  targetDeviceAlias: EntityAlias;
  alarmSource: Datasource;
  alarmSearchStatus: AlarmSearchStatus;
  alarmsPollingInterval: number;
  settings: WidgetConfigSettings;
  mobileOrder: number;
  mobileHeight: number;

  emptySettingsSchema = {
    type: 'object',
    properties: {}
  };

  emptySettingsGroupInfoes = [];

  defaultSettingsForm = [
    '*'
  ];

  currentSettingsSchema = deepClone(this.emptySettingsSchema);

  currentSettings: WidgetConfigSettings = {};
  currentSettingsGroupInfoes = deepClone(this.emptySettingsGroupInfoes);

  currentSettingsForm: any;

  private modelValue: WidgetConfigComponentData;

  private propagateChange = null;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
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
    if (this.modelValue) {
      const config = this.modelValue.config;
      const layout = this.modelValue.layout;
      if (config) {
        this.selectedTab = 0;
        this.title = config.title;
        this.showTitleIcon = isDefined(config.showTitleIcon) ? config.showTitleIcon : false;
        this.titleIcon = isDefined(config.titleIcon) ? config.titleIcon : '';
        this.iconColor = isDefined(config.iconColor) ? config.iconColor : 'rgba(0, 0, 0, 0.87)';
        this.iconSize = isDefined(config.iconSize) ? config.iconSize : '24px';
        this.showTitle = config.showTitle;
        this.dropShadow = isDefined(config.dropShadow) ? config.dropShadow : true;
        this.enableFullscreen = isDefined(config.enableFullscreen) ? config.enableFullscreen : true;
        this.backgroundColor = config.backgroundColor;
        this.color = config.color;
        this.padding = config.padding;
        this.margin = config.margin;
        this.widgetStyle =
          JSON.stringify(isDefined(config.widgetStyle) ? config.widgetStyle : {}, null, 2);
        this.titleStyle =
          JSON.stringify(isDefined(config.titleStyle) ? config.titleStyle : {
            fontSize: '16px',
            fontWeight: 400
          }, null, 2);
        this.units = config.units;
        this.decimals = config.decimals;
        this.useDashboardTimewindow = isDefined(config.useDashboardTimewindow) ?
          config.useDashboardTimewindow : true;
        this.displayTimewindow = isDefined(config.displayTimewindow) ?
          config.displayTimewindow : true;
        this.timewindow = config.timewindow;
        this.actions = config.actions;
        if (!this.actions) {
          this.actions = {};
        }
        if (this.isDataEnabled) {
          if (this.widgetType !== widgetType.rpc &&
            this.widgetType !== widgetType.alarm &&
            this.widgetType !== widgetType.static) {
            if (config.datasources) {
              this.datasources = config.datasources;
            } else {
              this.datasources = [];
            }
          } else if (this.widgetType === widgetType.rpc) {
            if (config.targetDeviceAliasIds && config.targetDeviceAliasIds.length > 0) {
              const aliasId = config.targetDeviceAliasIds[0];
              const entityAliases = this.aliasController.getEntityAliases();
              if (entityAliases[aliasId]) {
                this.targetDeviceAlias = entityAliases[aliasId];
              } else {
                this.targetDeviceAlias = null;
              }
            } else {
              this.targetDeviceAlias = null;
            }
          } else if (this.widgetType === widgetType.alarm) {
            this.alarmSearchStatus = isDefined(config.alarmSearchStatus) ?
              config.alarmSearchStatus : AlarmSearchStatus.ANY;
            this.alarmsPollingInterval = isDefined(config.alarmsPollingInterval) ?
              config.alarmsPollingInterval : 5;
            if (config.alarmSource) {
              this.alarmSource = config.alarmSource;
            } else {
              this.alarmSource = null;
            }
          }
        }
        this.settings = config.settings;

        this.updateSchemaForm();

        if (layout) {
          this.mobileOrder = layout.mobileOrder;
          this.mobileHeight = layout.mobileHeight;
        } else {
          this.mobileOrder = undefined;
          this.mobileHeight = undefined;
        }
      }
    }
  }

  private updateSchemaForm() {
    if (this.widgetSettingsSchema && this.widgetSettingsSchema.schema) {
      this.currentSettingsSchema = this.widgetSettingsSchema.schema;
      this.currentSettingsForm = this.widgetSettingsSchema.form || deepClone(this.defaultSettingsForm);
      this.currentSettingsGroupInfoes = this.widgetSettingsSchema.groupInfoes;
      this.currentSettings = this.settings;
    } else {
      this.currentSettingsForm = deepClone(this.defaultSettingsForm);
      this.currentSettingsSchema = deepClone(this.emptySettingsSchema);
      this.currentSettingsGroupInfoes = deepClone(this.emptySettingsGroupInfoes);
      this.currentSettings = {};
    }
  }

  public updateModel() {
    if (this.modelValue) {
      if (this.modelValue.config) {
        const config = this.modelValue.config;
        config.useDashboardTimewindow = this.useDashboardTimewindow;
        config.displayTimewindow = this.displayTimewindow;
        config.timewindow = this.timewindow;
      }
      this.propagateChange(this.modelValue);
    }
  }

  public displayAdvanced(): boolean {
    return this.widgetSettingsSchema && this.widgetSettingsSchema.schema;
  }

  public validate(c: FormControl) {
    return null; /*{
      targetDeviceAliasIds: {
        valid: false,
      },
    };*/
  }

}
