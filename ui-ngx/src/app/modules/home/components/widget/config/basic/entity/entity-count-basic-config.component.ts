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
import { DatasourceType, WidgetConfig, widgetTitleAutocompleteValues, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isUndefined } from '@core/utils';
import { UtilsService } from '@core/services/utils.service';
import { countDefaultSettings, CountWidgetSettings } from '@home/components/widget/lib/count/count-widget.models';

@Component({
  selector: 'tb-entity-count-basic-config',
  templateUrl: './entity-count-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class EntityCountBasicConfigComponent extends BasicWidgetConfigComponent {

  entityCountWidgetConfigForm: UntypedFormGroup;

  predefinedValues = widgetTitleAutocompleteValues;
  
  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.entityCountWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    let datasources = configData.config.datasources;
    if (!datasources || !datasources.length) {
      datasources = [{}];
      configData.config.datasources = datasources;
    }
    datasources[0].type = DatasourceType.entityCount;
    datasources[0].dataKeys = [this.utils.createKey({name: 'count'}, DataKeyType.count)];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: CountWidgetSettings = {...countDefaultSettings(false), ...(configData.config.settings || {})};
    this.entityCountWidgetConfigForm = this.fb.group({
      datasources: [configData.config.datasources, []],
      settings: [settings, []],

      backgroundColor: [configData.config.backgroundColor, []],
      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.datasources = config.datasources;

    this.widgetConfig.config.settings = {...(this.widgetConfig.config.settings || {}), ...config.settings};

    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
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
}
