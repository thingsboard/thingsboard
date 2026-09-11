// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Injector } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  statusWidgetDefaultSettings,
  statusWidgetLayoutImages,
  statusWidgetLayouts,
  statusWidgetLayoutTranslations
} from '@home/components/widget/lib/indicator/status-widget.models';
import { ValueType } from '@shared/models/constants';

@Component({
    selector: 'tb-status-widget-settings',
    templateUrl: './status-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class StatusWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  statusWidgetLayouts = statusWidgetLayouts;

  statusWidgetLayoutTranslationMap = statusWidgetLayoutTranslations;
  statusWidgetLayoutImageMap = statusWidgetLayoutImages;

  valueType = ValueType;

  statusWidgetSettingsForm: UntypedFormGroup;

  cardStyleMode = 'on';

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.statusWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return statusWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.statusWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      disabledState: [settings.disabledState, []],
      layout: [settings.layout, []],
      onState: [settings.onState, []],
      offState: [settings.offState, []],
      padding: [settings.padding, []]
    });
  }
}
