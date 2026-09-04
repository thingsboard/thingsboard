// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  singleSwitchDefaultSettings,
  singleSwitchLayoutImages,
  singleSwitchLayouts,
  singleSwitchLayoutTranslations
} from '@home/components/widget/lib/rpc/single-switch-widget.models';
import { ValueType } from '@shared/models/constants';

@Component({
    selector: 'tb-single-switch-widget-settings',
    templateUrl: './single-switch-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class SingleSwitchWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  singleSwitchLayouts = singleSwitchLayouts;

  singleSwitchLayoutTranslationMap = singleSwitchLayoutTranslations;
  singleSwitchLayoutImageMap = singleSwitchLayoutImages;

  valueType = ValueType;

  singleSwitchWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.singleSwitchWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return singleSwitchDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.singleSwitchWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      onUpdateState: [settings.onUpdateState, []],
      offUpdateState: [settings.offUpdateState, []],
      disabledState: [settings.disabledState, []],
      layout: [settings.layout, []],
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

      switchColorOn: [settings.switchColorOn, []],
      switchColorOff: [settings.switchColorOff, []],
      switchColorDisabled: [settings.switchColorDisabled, []],

      tumblerColorOn: [settings.tumblerColorOn, []],
      tumblerColorOff: [settings.tumblerColorOff, []],
      tumblerColorDisabled: [settings.tumblerColorDisabled, []],

      showOnLabel: [settings.showOnLabel, []],
      onLabel: [settings.onLabel, []],
      onLabelFont: [settings.onLabelFont, []],
      onLabelColor: [settings.onLabelColor, []],

      showOffLabel: [settings.showOffLabel, []],
      offLabel: [settings.offLabel, []],
      offLabelFont: [settings.offLabelFont, []],
      offLabelColor: [settings.offLabelColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showIcon', 'showOnLabel', 'showOffLabel'];
  }

  protected updateValidators(_emitEvent: boolean): void {
    const showLabel: boolean = this.singleSwitchWidgetSettingsForm.get('showLabel').value;
    const showIcon: boolean = this.singleSwitchWidgetSettingsForm.get('showIcon').value;
    const showOnLabel: boolean = this.singleSwitchWidgetSettingsForm.get('showOnLabel').value;
    const showOffLabel: boolean = this.singleSwitchWidgetSettingsForm.get('showOffLabel').value;

    if (showLabel) {
      this.singleSwitchWidgetSettingsForm.get('label').enable();
      this.singleSwitchWidgetSettingsForm.get('labelFont').enable();
      this.singleSwitchWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.singleSwitchWidgetSettingsForm.get('label').disable();
      this.singleSwitchWidgetSettingsForm.get('labelFont').disable();
      this.singleSwitchWidgetSettingsForm.get('labelColor').disable();
    }

    if (showIcon) {
      this.singleSwitchWidgetSettingsForm.get('iconSize').enable();
      this.singleSwitchWidgetSettingsForm.get('iconSizeUnit').enable();
      this.singleSwitchWidgetSettingsForm.get('icon').enable();
      this.singleSwitchWidgetSettingsForm.get('iconColor').enable();
    } else {
      this.singleSwitchWidgetSettingsForm.get('iconSize').disable();
      this.singleSwitchWidgetSettingsForm.get('iconSizeUnit').disable();
      this.singleSwitchWidgetSettingsForm.get('icon').disable();
      this.singleSwitchWidgetSettingsForm.get('iconColor').disable();
    }

    if (showOnLabel) {
      this.singleSwitchWidgetSettingsForm.get('onLabel').enable();
      this.singleSwitchWidgetSettingsForm.get('onLabelFont').enable();
      this.singleSwitchWidgetSettingsForm.get('onLabelColor').enable();
    } else {
      this.singleSwitchWidgetSettingsForm.get('onLabel').disable();
      this.singleSwitchWidgetSettingsForm.get('onLabelFont').disable();
      this.singleSwitchWidgetSettingsForm.get('onLabelColor').disable();
    }

    if (showOffLabel) {
      this.singleSwitchWidgetSettingsForm.get('offLabel').enable();
      this.singleSwitchWidgetSettingsForm.get('offLabelFont').enable();
      this.singleSwitchWidgetSettingsForm.get('offLabelColor').enable();
    } else {
      this.singleSwitchWidgetSettingsForm.get('offLabel').disable();
      this.singleSwitchWidgetSettingsForm.get('offLabelFont').disable();
      this.singleSwitchWidgetSettingsForm.get('offLabelColor').disable();
    }
  }
}
