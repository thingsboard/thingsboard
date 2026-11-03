// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-gateway-logs-settings',
    templateUrl: './gateway-logs-settings.component.html',
    styleUrls: ['../widget-settings.scss'],
    standalone: false
})
export class GatewayLogsSettingsComponent extends WidgetSettingsComponent {

  gatewayLogSettingForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayLogSettingForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      isConnectorLog: false,
      connectorLogState: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayLogSettingForm = this.fb.group({
      isConnectorLog: [false, []],
      connectorLogState: ['', Validators.required]
    });
  }

  protected validatorTriggers(): string[] {
    return ['isConnectorLog'];
  }

  protected updateValidators(emitEvent: boolean) {
    const isConnectorLog: boolean = this.gatewayLogSettingForm.get('isConnectorLog').value;
    if (isConnectorLog) {
      this.gatewayLogSettingForm.get('connectorLogState').enable({emitEvent});
    } else {
      this.gatewayLogSettingForm.get('connectorLogState').disable({emitEvent});
    }
  }
}
