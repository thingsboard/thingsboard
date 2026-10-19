// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TargetDevice, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import { ValueType } from '@shared/models/constants';
import {
  statusWidgetDefaultSettings,
  statusWidgetLayoutImages,
  statusWidgetLayouts,
  statusWidgetLayoutTranslations,
  StatusWidgetSettings
} from '@home/components/widget/lib/indicator/status-widget.models';

@Component({
    selector: 'tb-status-widget-basic-config',
    templateUrl: './status-widget-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class StatusWidgetBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.statusWidgetConfigForm.get('targetDevice').value;
  }

  statusWidgetLayouts = statusWidgetLayouts;

  statusWidgetLayoutTranslationMap = statusWidgetLayoutTranslations;
  statusWidgetLayoutImageMap = statusWidgetLayoutImages;

  valueType = ValueType;

  statusWidgetConfigForm: UntypedFormGroup;

  cardStyleMode = 'on';

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.statusWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: StatusWidgetSettings = {...statusWidgetDefaultSettings, ...(configData.config.settings || {})};
    this.statusWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],

      initialState: [settings.initialState, []],
      disabledState: [settings.disabledState, []],

      layout: [settings.layout, []],

      onState: [settings.onState, []],
      offState: [settings.offState, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.initialState = config.initialState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.layout = config.layout;

    this.widgetConfig.config.settings.onState = config.onState;
    this.widgetConfig.config.settings.offState = config.offState;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }

}
