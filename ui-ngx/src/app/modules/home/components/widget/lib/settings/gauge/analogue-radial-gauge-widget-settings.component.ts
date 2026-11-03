// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
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
