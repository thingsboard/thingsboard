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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { buildPageStepSizeValues } from '@home/components/widget/lib/table-widget.models';
import { Direction } from '@shared/models/page/sort-order';
import { entityFields } from '@shared/models/entity.models';

@Component({
  selector: 'tb-timeseries-table-widget-settings',
  templateUrl: './timeseries-table-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeseriesTableWidgetSettingsComponent extends WidgetSettingsComponent {

  entityFields = entityFields;
  Direction = Direction;

  timeseriesTableWidgetSettingsForm: UntypedFormGroup;
  pageStepSizeValues = [];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeseriesTableWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      enableSearch: true,
      enableSelectColumnDisplay: true,
      enableStickyHeader: true,
      enableStickyAction: true,
      showCellActionsMenu: true,
      reserveSpaceForHiddenAction: 'true',
      showTimestamp: true,
      dateFormat: {format: 'yyyy-MM-dd HH:mm:ss'},
      displayPagination: true,
      useEntityLabel: false,
      defaultPageSize: 10,
      pageStepIncrement: null,
      pageStepCount: 3,
      hideEmptyLines: false,
      disableStickyHeader: false,
      useRowStyleFunction: false,
      rowStyleFunction: '',
      sortOrder: {
        property: this.entityFields.createdTime.keyName,
        direction: Direction.DESC
      }
    };
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    settings.pageStepIncrement = settings.pageStepIncrement ?? settings.defaultPageSize;
    settings.sortOrder = {
      property: settings.sortOrder?.property || this.entityFields.createdTime.keyName,
      direction: settings.sortOrder?.direction || Direction.DESC
    };
    this.pageStepSizeValues = buildPageStepSizeValues(settings.pageStepCount, settings.pageStepIncrement);
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    // For backward compatibility
    const dateFormat = settings.dateFormat;
    if (settings?.showMilliseconds) {
      dateFormat.format = 'yyyy-MM-dd HH:mm:ss.SSS';
    }

    this.timeseriesTableWidgetSettingsForm = this.fb.group({
      enableSearch: [settings.enableSearch, []],
      enableSelectColumnDisplay: [settings.enableSelectColumnDisplay, []],
      enableStickyHeader: [settings.enableStickyHeader, []],
      enableStickyAction: [settings.enableStickyAction, []],
      showCellActionsMenu: [settings.showCellActionsMenu, []],
      reserveSpaceForHiddenAction: [settings.reserveSpaceForHiddenAction, []],
      showTimestamp: [settings.showTimestamp, []],
      dateFormat: [dateFormat, []],
      displayPagination: [settings.displayPagination, []],
      useEntityLabel: [settings.useEntityLabel, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      pageStepCount: [settings.pageStepCount ?? 3, [Validators.min(1), Validators.max(100),
        Validators.required, Validators.pattern(/^\d*$/)]],
      pageStepIncrement: [settings.pageStepIncrement, [Validators.min(1), Validators.required, Validators.pattern(/^\d*$/)]],
      hideEmptyLines: [settings.hideEmptyLines, []],
      disableStickyHeader: [settings.disableStickyHeader, []],
      useRowStyleFunction: [settings.useRowStyleFunction, []],
      rowStyleFunction: [settings.rowStyleFunction, [Validators.required]],
      sortOrder: this.fb.group({
        property: [
          settings.sortOrder.property,
          Validators.required
        ],
        direction: [
          settings.sortOrder.direction,
          Validators.required
        ]
      })
    });
  }

  protected validatorTriggers(): string[] {
    return ['useRowStyleFunction', 'displayPagination', 'pageStepCount', 'pageStepIncrement'];
  }

  protected prepareOutputSettings(settings: WidgetSettings): WidgetSettings {
    settings.sortOrder = {
      property: settings.sortOrder?.property || this.entityFields.createdTime.keyName,
      direction: settings.sortOrder?.direction || Direction.DESC
    };
    return settings;
  }

  protected updateValidators(emitEvent: boolean, trigger: string) {
    if (trigger === 'pageStepCount' || trigger === 'pageStepIncrement') {
      this.timeseriesTableWidgetSettingsForm.get('defaultPageSize').reset();
      this.pageStepSizeValues = buildPageStepSizeValues(this.timeseriesTableWidgetSettingsForm.get('pageStepCount').value,
        this.timeseriesTableWidgetSettingsForm.get('pageStepIncrement').value);
      return;
    }
    const useRowStyleFunction: boolean = this.timeseriesTableWidgetSettingsForm.get('useRowStyleFunction').value;
    const displayPagination: boolean = this.timeseriesTableWidgetSettingsForm.get('displayPagination').value;
    if (useRowStyleFunction) {
      this.timeseriesTableWidgetSettingsForm.get('rowStyleFunction').enable({emitEvent});
    } else {
      this.timeseriesTableWidgetSettingsForm.get('rowStyleFunction').disable({emitEvent});
    }
    if (displayPagination) {
      this.timeseriesTableWidgetSettingsForm.get('defaultPageSize').enable({emitEvent});
      this.timeseriesTableWidgetSettingsForm.get('pageStepCount').enable({emitEvent: false});
      this.timeseriesTableWidgetSettingsForm.get('pageStepIncrement').enable({emitEvent: false});
    } else {
      this.timeseriesTableWidgetSettingsForm.get('defaultPageSize').disable({emitEvent});
      this.timeseriesTableWidgetSettingsForm.get('pageStepCount').disable({emitEvent: false});
      this.timeseriesTableWidgetSettingsForm.get('pageStepIncrement').disable({emitEvent: false});
    }
  }

}
