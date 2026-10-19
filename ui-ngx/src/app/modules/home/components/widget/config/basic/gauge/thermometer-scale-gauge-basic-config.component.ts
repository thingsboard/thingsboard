// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  GaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/analog-gauge-basic-config.component';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
    selector: 'tb-thermometer-scale-gauge-basic-config',
    templateUrl: './analog-gauge-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class ThermometerScaleGaugeBasicConfigComponent extends GaugeBasicConfigComponent {

  gaugeType = 'linear';

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    super.onConfigSet(configData);
    this.radialGaugeWidgetConfigForm.addControl('colorBarStroke',
      this.fb.control(configData.config.settings?.colorBarStroke, []));
    this.radialGaugeWidgetConfigForm.addControl('colorBarProgress',
      this.fb.control(configData.config.settings?.colorBarProgress, []));
    this.radialGaugeWidgetConfigForm.addControl('colorBarProgressEnd',
      this.fb.control(configData.config.settings?.colorBarProgressEnd, []));
  }

  protected prepareOutputConfig(config): WidgetConfigComponentData {
    const outputConfig = super.prepareOutputConfig(config);
    this.widgetConfig.config.settings.colorBarStroke = config.colorBarStroke;
    this.widgetConfig.config.settings.colorBarProgress = config.colorBarProgress;
    this.widgetConfig.config.settings.colorBarProgressEnd = config.colorBarProgressEnd;
    return outputConfig;
  }
}
