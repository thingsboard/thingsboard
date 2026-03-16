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

@Component({
    selector: 'tb-alarms-table-widget-settings',
    templateUrl: './alarms-table-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class AlarmsTableWidgetSettingsComponent extends WidgetSettingsComponent {

  alarmsTableWidgetSettingsForm: UntypedFormGroup;
  pageStepSizeValues = [];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.alarmsTableWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      alarmsTitle: '',
      enableSelection: true,
      enableSearch: true,
      enableSelectColumnDisplay: true,
      enableFilter: true,
      enableStickyHeader: true,
      enableStickyAction: true,
      showCellActionsMenu: true,
      reserveSpaceForHiddenAction: 'true',
      displayDetails: true,
      allowAcknowledgment: true,
      allowClear: true,
      allowAssign: true,
      displayActivity: true,
      displayPagination: true,
      defaultPageSize: 10,
      pageStepIncrement: null,
      pageStepCount: 3,
      defaultSortOrder: '-createdTime',
      useRowStyleFunction: false,
      rowStyleFunction: ''
    };
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    settings.pageStepIncrement = settings.pageStepIncrement ?? settings.defaultPageSize;
    this.pageStepSizeValues = buildPageStepSizeValues(settings.pageStepCount, settings.pageStepIncrement);
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.alarmsTableWidgetSettingsForm = this.fb.group({
      alarmsTitle: [settings.alarmsTitle, []],
      enableSelection: [settings.enableSelection, []],
      enableSearch: [settings.enableSearch, []],
      enableSelectColumnDisplay: [settings.enableSelectColumnDisplay, []],
      enableFilter: [settings.enableFilter, []],
      enableStickyHeader: [settings.enableStickyHeader, []],
      enableStickyAction: [settings.enableStickyAction, []],
      showCellActionsMenu: [settings.showCellActionsMenu, []],
      reserveSpaceForHiddenAction: [settings.reserveSpaceForHiddenAction, []],
      displayDetails: [settings.displayDetails, []],
      allowAcknowledgment: [settings.allowAcknowledgment, []],
      allowClear: [settings.allowClear, []],
      allowAssign: [settings.allowAssign, []],
      displayActivity: [settings.displayActivity, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      pageStepCount: [settings.pageStepCount ?? 3, [Validators.min(1), Validators.max(100),
        Validators.required, Validators.pattern(/^\d*$/)]],
      pageStepIncrement: [settings.pageStepIncrement, [Validators.min(1), Validators.required, Validators.pattern(/^\d*$/)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      useRowStyleFunction: [settings.useRowStyleFunction, []],
      rowStyleFunction: [settings.rowStyleFunction, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useRowStyleFunction', 'displayPagination', 'pageStepCount', 'pageStepIncrement'];
  }

  protected updateValidators(emitEvent: boolean, trigger: string) {
    if (trigger === 'pageStepCount' || trigger === 'pageStepIncrement') {
      this.alarmsTableWidgetSettingsForm.get('defaultPageSize').reset();
      this.pageStepSizeValues = buildPageStepSizeValues(this.alarmsTableWidgetSettingsForm.get('pageStepCount').value,
        this.alarmsTableWidgetSettingsForm.get('pageStepIncrement').value);
      return;
    }
    const useRowStyleFunction: boolean = this.alarmsTableWidgetSettingsForm.get('useRowStyleFunction').value;
    const displayPagination: boolean = this.alarmsTableWidgetSettingsForm.get('displayPagination').value;
    if (useRowStyleFunction) {
      this.alarmsTableWidgetSettingsForm.get('rowStyleFunction').enable({emitEvent});
    } else {
      this.alarmsTableWidgetSettingsForm.get('rowStyleFunction').disable({emitEvent});
    }
    if (displayPagination) {
      this.alarmsTableWidgetSettingsForm.get('defaultPageSize').enable({emitEvent});
      this.alarmsTableWidgetSettingsForm.get('pageStepCount').enable({emitEvent: false});
      this.alarmsTableWidgetSettingsForm.get('pageStepIncrement').enable({emitEvent: false});
    } else {
      this.alarmsTableWidgetSettingsForm.get('defaultPageSize').disable({emitEvent});
      this.alarmsTableWidgetSettingsForm.get('pageStepCount').disable({emitEvent: false});
      this.alarmsTableWidgetSettingsForm.get('pageStepIncrement').disable({emitEvent: false});
    }
  }

}
