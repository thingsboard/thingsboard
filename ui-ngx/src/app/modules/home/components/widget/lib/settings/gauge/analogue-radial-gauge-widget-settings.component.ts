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
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AnalogueGaugeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/analogue-gauge-widget-settings.component';

@Component({
  selector: 'tb-analogue-radial-gauge-widget-settings',
  templateUrl: './analogue-gauge-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class AnalogueRadialGaugeWidgetSettingsComponent extends AnalogueGaugeWidgetSettingsComponent {

  gaugeType = 'radial';

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultSettings(): WidgetSettings {
    const settings = super.defaultSettings();
    settings.startAngle = 45;
    settings.ticksAngle = 270;
    settings.needleCircleSize = 10;
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    super.onSettingsSet(settings);
    this.analogueGaugeWidgetSettingsForm.addControl('startAngle',
      this.fb.control(settings.startAngle, [Validators.min(0), Validators.max(360)]));
    this.analogueGaugeWidgetSettingsForm.addControl('ticksAngle',
      this.fb.control(settings.ticksAngle, [Validators.min(0), Validators.max(360)]));
    this.analogueGaugeWidgetSettingsForm.addControl('needleCircleSize',
      this.fb.control(settings.needleCircleSize, [Validators.min(0)]));
  }
}
