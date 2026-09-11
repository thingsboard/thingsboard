// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TargetDevice, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { ValueType } from '@shared/models/constants';
import {
  commandButtonDefaultSettings,
  CommandButtonWidgetSettings
} from '@home/components/widget/lib/button/command-button-widget.models';

@Component({
    selector: 'tb-command-button-basic-config',
    templateUrl: './command-button-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class CommandButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.commandButtonWidgetConfigForm.get('targetDevice').value;
  }

  valueType = ValueType;

  commandButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.commandButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: CommandButtonWidgetSettings = {...commandButtonDefaultSettings, ...(configData.config.settings || {})};
    this.commandButtonWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],

      onClickState: [settings.onClickState, []],
      disabledState: [settings.disabledState, []],

      appearance: [settings.appearance, []],

      borderRadius: [configData.config.borderRadius, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {

    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.onClickState = config.onClickState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.appearance = config.appearance;

    this.widgetConfig.config.borderRadius = config.borderRadius;

    return this.widgetConfig;
  }
}
