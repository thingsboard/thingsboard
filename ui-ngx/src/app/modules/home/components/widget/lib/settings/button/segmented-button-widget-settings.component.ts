///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import {
  TargetDevice,
  WidgetSettings,
  WidgetSettingsComponent,
  widgetTitleAutocompleteValues,
  widgetType
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ValueType } from '@shared/models/constants';
import { getTargetDeviceFromDatasources } from '@shared/models/widget-settings.models';
import {
  SegmentedButtonAppearanceType,
  SegmentedButtonColorStylesType,
  segmentedButtonDefaultSettings,
  segmentedButtonLayoutBorder,
  segmentedButtonLayoutImages,
  segmentedButtonLayouts,
  segmentedButtonLayoutTranslations,
  WidgetButtonToggleState,
  widgetButtonToggleStatesTranslations
} from '@home/components/widget/lib/button/segmented-button-widget.models';

@Component({
  selector: 'tb-segmented-button-widget-settings',
  templateUrl: './segmented-button-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class SegmentedButtonWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    const datasources = this.widgetConfig?.config?.datasources;
    return getTargetDeviceFromDatasources(datasources);
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }
  get borderRadius(): string {
    return this.segmentedButtonLayoutBorderMap.get(this.segmentedButtonWidgetSettingsForm.get('appearance.layout').value);
  }

  segmentedButtonAppearanceType: SegmentedButtonAppearanceType = 'first';
  segmentedButtonColorStylesType: SegmentedButtonColorStylesType = 'selected';

  widgetButtonToggleStates = Object.keys(WidgetButtonToggleState) as WidgetButtonToggleState[];
  widgetButtonToggleStatesTranslationsMap = widgetButtonToggleStatesTranslations;

  segmentedButtonLayouts = segmentedButtonLayouts;
  segmentedButtonLayoutTranslationMap = segmentedButtonLayoutTranslations;
  segmentedButtonLayoutImageMap = segmentedButtonLayoutImages;
  segmentedButtonLayoutBorderMap = segmentedButtonLayoutBorder;

  valueType = ValueType;

  segmentedButtonWidgetSettingsForm: UntypedFormGroup;

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.segmentedButtonWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return segmentedButtonDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.segmentedButtonWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      leftButtonClick: [settings.leftButtonClick, []],
      rightButtonClick: [settings.rightButtonClick, []],
      disabledState: [settings.disabledState, []],

      appearance: this.fb.group({
        layout: [settings.appearance.layout, []],
        autoScale: [settings.appearance.autoScale, []],
        cardBorder: [settings.appearance.cardBorder, []],
        cardBorderColor: [settings.appearance.cardBorderColor, []],
        leftAppearance: this.fb.group({
          showLabel: [settings.appearance.leftAppearance.showLabel, []],
          label: [settings.appearance.leftAppearance.label, []],
          labelFont: [settings.appearance.leftAppearance.labelFont, []],
          showIcon: [settings.appearance.leftAppearance.showIcon, []],
          icon: [settings.appearance.leftAppearance.icon, []],
          iconSize: [settings.appearance.leftAppearance.iconSize, []],
          iconSizeUnit: [settings.appearance.leftAppearance.iconSizeUnit, []],
        }),
        rightAppearance: this.fb.group({
          showLabel: [settings.appearance.rightAppearance.showLabel, []],
          label: [settings.appearance.rightAppearance.label, []],
          labelFont: [settings.appearance.rightAppearance.labelFont, []],
          showIcon: [settings.appearance.rightAppearance.showIcon, []],
          icon: [settings.appearance.rightAppearance.icon, []],
          iconSize: [settings.appearance.rightAppearance.iconSize, []],
          iconSizeUnit: [settings.appearance.rightAppearance.iconSizeUnit, []],
        }),
        selectedStyle: this.fb.group({
          mainColor: [settings.appearance.selectedStyle.mainColor, []],
          backgroundColor: [settings.appearance.selectedStyle.backgroundColor, []],
          customStyle: this.fb.group({
            enabled: [settings.appearance.selectedStyle.customStyle.enabled, []],
            hovered: [settings.appearance.selectedStyle.customStyle.hovered, []],
            disabled: [settings.appearance.selectedStyle.customStyle.disabled, []],
          })
        }),
        unselectedStyle: this.fb.group({
          mainColor: [settings.appearance.unselectedStyle.mainColor, []],
          backgroundColor: [settings.appearance.unselectedStyle.backgroundColor, []],
          customStyle: this.fb.group({
            enabled: [settings.appearance.unselectedStyle.customStyle.enabled, []],
            hovered: [settings.appearance.unselectedStyle.customStyle.hovered, []],
            disabled: [settings.appearance.unselectedStyle.customStyle.disabled, []],
          })
        }),
      })
    });
  }
}
