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
    selector: 'tb-analogue-linear-gauge-widget-settings',
    templateUrl: './analogue-gauge-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class AnalogueLinearGaugeWidgetSettingsComponent extends AnalogueGaugeWidgetSettingsComponent {

  gaugeType = 'linear';

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultSettings(): WidgetSettings {
    const settings = super.defaultSettings();
    settings.barStrokeWidth = 2.5;
    settings.colorBarStroke = null;
    settings.colorBar = '#fff';
    settings.colorBarEnd = '#ddd';
    settings.colorBarProgress = null;
    settings.colorBarProgressEnd = null;
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    super.onSettingsSet(settings);
    this.analogueGaugeWidgetSettingsForm.addControl('barStrokeWidth',
      this.fb.control(settings.barStrokeWidth, [Validators.min(0)]));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarStroke',
      this.fb.control(settings.colorBarStroke, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBar',
      this.fb.control(settings.colorBar, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarEnd',
      this.fb.control(settings.colorBarEnd, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarProgress',
      this.fb.control(settings.colorBarProgress, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarProgressEnd',
      this.fb.control(settings.colorBarProgressEnd, []));
  }
}
