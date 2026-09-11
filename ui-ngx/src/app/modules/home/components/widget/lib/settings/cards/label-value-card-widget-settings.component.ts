// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { labelValueCardWidgetDefaultSettings } from '@home/components/widget/lib/cards/label-value-card-widget.models';
import { formatValue } from '@core/utils';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-label-value-card-widget-settings',
    templateUrl: './label-value-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class LabelValueCardWidgetSettingsComponent extends WidgetSettingsComponent {

  labelValueCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.labelValueCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return labelValueCardWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.labelValueCardWidgetSettingsForm = this.fb.group({
      autoScale: [settings.autoScale, []],

      showLabel: [settings.showLabel, []],
      label: [settings.label, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      showIcon: [settings.showIcon, []],
      iconSize: [settings.iconSize, [Validators.min(0)]],
      iconSizeUnit: [settings.iconSizeUnit, []],
      icon: [settings.icon, []],
      iconColor: [settings.iconColor, []],

      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLabel: boolean = this.labelValueCardWidgetSettingsForm.get('showLabel').value;
    const showIcon: boolean = this.labelValueCardWidgetSettingsForm.get('showIcon').value;

    if (showLabel) {
      this.labelValueCardWidgetSettingsForm.get('label').enable();
      this.labelValueCardWidgetSettingsForm.get('labelFont').enable();
      this.labelValueCardWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.labelValueCardWidgetSettingsForm.get('label').disable();
      this.labelValueCardWidgetSettingsForm.get('labelFont').disable();
      this.labelValueCardWidgetSettingsForm.get('labelColor').disable();
    }

    if (showIcon) {
      this.labelValueCardWidgetSettingsForm.get('iconSize').enable();
      this.labelValueCardWidgetSettingsForm.get('iconSizeUnit').enable();
      this.labelValueCardWidgetSettingsForm.get('icon').enable();
      this.labelValueCardWidgetSettingsForm.get('iconColor').enable();
    } else {
      this.labelValueCardWidgetSettingsForm.get('iconSize').disable();
      this.labelValueCardWidgetSettingsForm.get('iconSizeUnit').disable();
      this.labelValueCardWidgetSettingsForm.get('icon').disable();
      this.labelValueCardWidgetSettingsForm.get('iconColor').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }
}
