// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-update-boolean-attribute-widget-settings',
    templateUrl: './update-boolean-attribute-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class UpdateBooleanAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateBooleanAttributeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateBooleanAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      trueValue: '',
      falseValue: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateBooleanAttributeWidgetSettingsForm = this.fb.group({
      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],
      trueValue: [settings.trueValue, []],
      falseValue: [settings.falseValue, []]
    });
  }
}
