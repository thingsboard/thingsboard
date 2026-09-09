// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { labelCardWidgetDefaultSettings } from '@home/components/widget/lib/cards/label-card-widget.models';

@Component({
    selector: 'tb-label-card-widget-settings',
    templateUrl: './label-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class LabelCardWidgetSettingsComponent extends WidgetSettingsComponent {

  labelCardWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.labelCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return labelCardWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.labelCardWidgetSettingsForm = this.fb.group({
      autoScale: [settings.autoScale, []],

      label: [settings.label, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      showIcon: [settings.showIcon, []],
      iconSize: [settings.iconSize, [Validators.min(0)]],
      iconSizeUnit: [settings.iconSizeUnit, []],
      icon: [settings.icon, []],
      iconColor: [settings.iconColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showIcon'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showIcon: boolean = this.labelCardWidgetSettingsForm.get('showIcon').value;
    if (showIcon) {
      this.labelCardWidgetSettingsForm.get('iconSize').enable();
      this.labelCardWidgetSettingsForm.get('iconSizeUnit').enable();
      this.labelCardWidgetSettingsForm.get('icon').enable();
      this.labelCardWidgetSettingsForm.get('iconColor').enable();
    } else {
      this.labelCardWidgetSettingsForm.get('iconSize').disable();
      this.labelCardWidgetSettingsForm.get('iconSizeUnit').disable();
      this.labelCardWidgetSettingsForm.get('icon').disable();
      this.labelCardWidgetSettingsForm.get('iconColor').disable();
    }
  }
}
