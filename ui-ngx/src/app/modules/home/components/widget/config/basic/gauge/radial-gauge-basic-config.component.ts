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
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AnalogueGaugeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/analogue-gauge-widget-settings.component';
import {
  GaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/analog-gauge-basic-config.component';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-radial-gauge-basic-config',
  templateUrl: './analog-gauge-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class RadialGaugeBasicConfigComponent extends GaugeBasicConfigComponent {

  gaugeType = 'radial';

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    return super.setupDefaults(configData, [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }]);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    super.onConfigSet(configData);
  }
}
