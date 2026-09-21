// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ValueType } from '@shared/models/constants';
import { commandButtonDefaultSettings } from '@home/components/widget/lib/button/command-button-widget.models';

@Component({
    selector: 'tb-command-button-widget-settings',
    templateUrl: './command-button-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class CommandButtonWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }
  get borderRadius(): string {
    return this.widgetConfig?.config?.borderRadius;
  }

  valueType = ValueType;

  commandButtonWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.commandButtonWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return commandButtonDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.commandButtonWidgetSettingsForm = this.fb.group({
      onClickState: [settings.onClickState, []],
      disabledState: [settings.disabledState, []],

      appearance: [settings.appearance, []]
    });
  }
}
