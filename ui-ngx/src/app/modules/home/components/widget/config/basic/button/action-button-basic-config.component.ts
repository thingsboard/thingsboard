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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  actionDescriptorToAction,
  Datasource,
  defaultWidgetAction,
  TargetDevice,
  WidgetAction,
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { guid } from '@core/utils';
import { ValueType } from '@shared/models/constants';
import { getTargetDeviceFromDatasources } from '@shared/models/widget-settings.models';
import {
  actionButtonDefaultSettings,
  ActionButtonWidgetSettings
} from '@home/components/widget/lib/button/action-button-widget.models';

@Component({
    selector: 'tb-action-button-basic-config',
    templateUrl: './action-button-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class ActionButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    const datasources: Datasource[] = this.actionButtonWidgetConfigForm.get('datasources').value;
    return getTargetDeviceFromDatasources(datasources);
  }

  valueType = ValueType;

  actionButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.actionButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ActionButtonWidgetSettings = {...actionButtonDefaultSettings, ...(configData.config.settings || {})};
    const onClickAction = this.getOnClickAction(configData.config);
    this.actionButtonWidgetConfigForm = this.fb.group({
      datasources: [configData.config.datasources, []],

      onClickAction: [onClickAction, []],
      activatedState: [settings.activatedState, []],
      disabledState: [settings.disabledState, []],

      appearance: [settings.appearance, []],

      borderRadius: [configData.config.borderRadius, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {

    this.widgetConfig.config.datasources = config.datasources;
    this.setOnClickAction(this.widgetConfig.config, config.onClickAction);

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.activatedState = config.activatedState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.appearance = config.appearance;

    this.widgetConfig.config.borderRadius = config.borderRadius;

    return this.widgetConfig;
  }

  private getOnClickAction(config: WidgetConfig): WidgetAction {
    let clickAction: WidgetAction;
    const actions = config.actions;
    if (actions && actions.click) {
      const descriptors = actions.click;
      if (descriptors?.length) {
        const descriptor = descriptors[0];
        clickAction = actionDescriptorToAction(descriptor);
      }
    }
    if (!clickAction) {
      clickAction = defaultWidgetAction();
    }
    return clickAction;
  }

  private setOnClickAction(config: WidgetConfig, clickAction: WidgetAction): void {
    let actions = config.actions;
    if (!actions) {
      actions = {};
      config.actions = actions;
    }
    let descriptors = actions.click;
    if (!descriptors) {
      descriptors = [];
      actions.click = descriptors;
    }
    let descriptor = descriptors[0];
    if (!descriptor) {
      descriptor = {
        id: guid(),
        name: 'onClick',
        icon: 'more_horiz',
        ...clickAction
      };
      descriptors[0] = descriptor;
    } else {
      descriptors[0] = {...descriptor, ...clickAction};
    }
  }
}
