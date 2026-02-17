///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { Datasource, TargetDevice, widgetTitleAutocompleteValues, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
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
  SegmentedButtonWidgetSettings,
  WidgetButtonToggleState,
  widgetButtonToggleStatesTranslations
} from '@home/components/widget/lib/button/segmented-button-widget.models';

@Component({
    selector: 'tb-segmented-button-basic-config',
    templateUrl: './segmented-button-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class SegmentedButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    const datasources: Datasource[] = this.segmentedButtonWidgetConfigForm.get('datasources').value;
    return getTargetDeviceFromDatasources(datasources);
  }
  predefinedValues = widgetTitleAutocompleteValues;
  segmentedButtonAppearanceType: SegmentedButtonAppearanceType = 'first';
  segmentedButtonColorStylesType: SegmentedButtonColorStylesType = 'selected';

  widgetButtonToggleStates = Object.keys(WidgetButtonToggleState) as WidgetButtonToggleState[];
  widgetButtonToggleStatesTranslationsMap = widgetButtonToggleStatesTranslations;

  segmentedButtonLayouts = segmentedButtonLayouts;
  segmentedButtonLayoutTranslationMap = segmentedButtonLayoutTranslations;
  segmentedButtonLayoutImageMap = segmentedButtonLayoutImages;
  segmentedButtonLayoutBorderMap = segmentedButtonLayoutBorder;

  valueType = ValueType;

  segmentedButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.segmentedButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: SegmentedButtonWidgetSettings = {...segmentedButtonDefaultSettings, ...(configData.config.settings || {})};

    this.segmentedButtonWidgetConfigForm = this.fb.group({
      datasources: [configData.config.datasources, []],

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

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.datasources = config.datasources;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.initialState = config.initialState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;
    this.widgetConfig.config.settings.leftButtonClick = config.leftButtonClick;
    this.widgetConfig.config.settings.rightButtonClick = config.rightButtonClick;
    this.widgetConfig.config.settings.appearance = config.appearance;
    this.widgetConfig.config.borderRadius = this.segmentedButtonLayoutBorderMap.get(config.appearance.layout);
    return this.widgetConfig;
  }


  protected validatorTriggers(): string[] {
    return ['appearance.leftAppearance.showLabel', 'appearance.leftAppearance.showIcon', 'appearance.rightAppearance.showLabel', 'appearance.rightAppearance.showIcon',];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLeftLabel: boolean = this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.showLabel').value;
    const showRightLabel: boolean = this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.showLabel').value;

    const showLeftIcon: boolean = this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.showIcon').value;
    const showRightIcon: boolean = this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.showIcon').value;

    if (showLeftLabel) {
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.label').enable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.labelFont').enable({emitEvent});
    } else {
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.label').disable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.labelFont').disable({emitEvent});
    }

    if (showRightLabel) {
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.label').enable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.labelFont').enable({emitEvent});
    } else {
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.label').disable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.labelFont').disable({emitEvent});
    }

    if (showLeftIcon) {
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.icon').enable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.iconSize').enable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.iconSizeUnit').enable({emitEvent});
    } else {
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.icon').disable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.iconSize').disable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.leftAppearance.iconSizeUnit').disable({emitEvent});
    }

    if (showRightIcon) {
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.icon').enable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.iconSize').enable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.iconSizeUnit').enable({emitEvent});
    } else {
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.icon').disable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.iconSize').disable({emitEvent});
      this.segmentedButtonWidgetConfigForm.get('appearance.rightAppearance.iconSizeUnit').disable({emitEvent});
    }
  }
}
