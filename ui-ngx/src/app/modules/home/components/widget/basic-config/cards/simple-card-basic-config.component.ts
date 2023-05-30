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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-simple-card-basic-config',
  templateUrl: './simple-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss', '../../widget-config.scss']
})
export class SimpleCardBasicConfigComponent extends BasicWidgetConfigComponent {

  simpleCardWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected configForm(): UntypedFormGroup {
    return this.simpleCardWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.simpleCardWidgetConfigForm = this.fb.group({
      datasources: [configData.config.datasources, []],
      labelPosition: [configData.config.settings?.labelPosition, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.datasources = this.simpleCardWidgetConfigForm.value.datasources;
    this.widgetConfig.config.actions = this.simpleCardWidgetConfigForm.value.actions;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.labelPosition = this.simpleCardWidgetConfigForm.value.labelPosition;
    return this.widgetConfig;
  }

}
