///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { TargetDevice, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { ScadaTestWidgetSettings } from '@home/components/widget/lib/scada/scada-test-widget.models';

@Component({
  selector: 'tb-scada-test-basic-config',
  templateUrl: './scada-test-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ScadaTestBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.scadaTestWidgetConfigForm.get('targetDevice').value;
  }

  scadaTestWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.scadaTestWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ScadaTestWidgetSettings = {...(configData.config.settings as ScadaTestWidgetSettings || { scadaObject: {}})};
    this.scadaTestWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],
      scadaObject: [settings.scadaObject, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.targetDevice = config.targetDevice;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.scadaObject = config.scadaObject;
    return this.widgetConfig;
  }
}
