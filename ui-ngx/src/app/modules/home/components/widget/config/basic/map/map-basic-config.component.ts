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

import { Component, Injector } from '@angular/core';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isUndefined, mergeDeepIgnoreArray } from '@core/utils';
import { mapWidgetDefaultSettings, MapWidgetSettings } from '@home/components/widget/lib/maps/map-widget.models';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  datasourcesHasOnlyComparisonAggregation,
  WidgetConfig,
  widgetTitleAutocompleteValues
} from '@shared/models/widget.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { findWidgetModelDefinition } from '@shared/models/widget/widget-model.definition';

@Component({
  selector: 'tb-map-basic-config',
  templateUrl: './map-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class MapBasicConfigComponent extends BasicWidgetConfigComponent {

  mapWidgetConfigForm: UntypedFormGroup;

  trip = false;

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.mapWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.trip)) {
      this.trip = params.trip === true;
    }
    super.setupConfig(widgetConfig);
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    const settings = configData.config.settings as MapWidgetSettings;
    if (settings?.markers?.length) {
      settings.markers = [];
    }
    if (settings?.polygons?.length) {
      settings.polygons = [];
    }
    if (settings?.circles?.length) {
      settings.circles = [];
    }
    if (this.trip) {
      if (settings?.trips?.length) {
        settings.trips = [];
      }
    }
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: MapWidgetSettings = mergeDeepIgnoreArray<MapWidgetSettings>({} as MapWidgetSettings,
      mapWidgetDefaultSettings, configData.config.settings as MapWidgetSettings);
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.mapWidgetConfigForm = this.fb.group({
      mapSettings: [settings, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
    this.mapWidgetConfigForm.addControl('timewindowConfig', this.fb.control(getTimewindowConfig(configData.config)));
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.settings = config.mapSettings || {};

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;

    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.mapWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.mapWidgetConfigForm.get('showIcon').value;

    if (showTitle) {
      this.mapWidgetConfigForm.get('title').enable();
      this.mapWidgetConfigForm.get('titleFont').enable();
      this.mapWidgetConfigForm.get('titleColor').enable();
      this.mapWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.mapWidgetConfigForm.get('iconSize').enable();
        this.mapWidgetConfigForm.get('iconSizeUnit').enable();
        this.mapWidgetConfigForm.get('icon').enable();
        this.mapWidgetConfigForm.get('iconColor').enable();
      } else {
        this.mapWidgetConfigForm.get('iconSize').disable();
        this.mapWidgetConfigForm.get('iconSizeUnit').disable();
        this.mapWidgetConfigForm.get('icon').disable();
        this.mapWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.mapWidgetConfigForm.get('title').disable();
      this.mapWidgetConfigForm.get('titleFont').disable();
      this.mapWidgetConfigForm.get('titleColor').disable();
      this.mapWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.mapWidgetConfigForm.get('iconSize').disable();
      this.mapWidgetConfigForm.get('iconSizeUnit').disable();
      this.mapWidgetConfigForm.get('icon').disable();
      this.mapWidgetConfigForm.get('iconColor').disable();
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }

  public get displayTimewindowConfig(): boolean {
    if (this.trip) {
      return true;
    } else {
      const widgetDefinition = findWidgetModelDefinition(this.widget);
      if (widgetDefinition) {
        return widgetDefinition.hasTimewindow(this.widget);
      } else {
        return false;
      }
    }
  }

  public get onlyHistoryTimewindow(): boolean {
    if (this.trip) {
      return false;
    } else {
      const widgetDefinition = findWidgetModelDefinition(this.widget);
      if (widgetDefinition) {
        return widgetDefinition.datasourcesHasOnlyComparisonAggregation(this.widget);
      } else {
        return false;
      }
    }
  }

}
