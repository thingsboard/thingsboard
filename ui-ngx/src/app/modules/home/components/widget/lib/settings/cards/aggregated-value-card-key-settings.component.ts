// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  aggregatedValueCardDefaultKeySettings,
  AggregatedValueCardKeyPosition,
  aggregatedValueCardKeyPositionTranslations
} from '@home/components/widget/lib/cards/aggregated-value-card.models';

@Component({
    selector: 'tb-aggregated-value-card-key-settings',
    templateUrl: './aggregated-value-card-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class AggregatedValueCardKeySettingsComponent extends WidgetSettingsComponent {

  aggregatedValueCardKeyPositions: AggregatedValueCardKeyPosition[] =
    Object.keys(AggregatedValueCardKeyPosition).map(value => AggregatedValueCardKeyPosition[value]);

  aggregatedValueCardKeyPositionTranslationMap = aggregatedValueCardKeyPositionTranslations;

  aggregatedValueCardKeySettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.aggregatedValueCardKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return aggregatedValueCardDefaultKeySettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.aggregatedValueCardKeySettingsForm = this.fb.group({
      position: [settings.position, []],
      font: [settings.font, []],
      color: [settings.color, []],
      showArrow: [settings.showArrow, []]
    });
  }
}
