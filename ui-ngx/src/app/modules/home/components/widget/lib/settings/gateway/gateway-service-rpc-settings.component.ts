// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-gateway-service-rpc-settings',
    templateUrl: './gateway-service-rpc-settings.component.html',
    styleUrls: ['../widget-settings.scss'],
    standalone: false
})
export class GatewayServiceRPCSettingsComponent extends WidgetSettingsComponent {

  gatewayServiceRPCSettingForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayServiceRPCSettingForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      isConnector: false,
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayServiceRPCSettingForm = this.fb.group({
      isConnector: [false, []]
    });
  }
}
