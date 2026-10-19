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
    selector: 'tb-radial-gauge-basic-config',
    templateUrl: './analog-gauge-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class RadialGaugeBasicConfigComponent extends GaugeBasicConfigComponent {

  gaugeType = 'radial';

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    super.onConfigSet(configData);
  }
}
