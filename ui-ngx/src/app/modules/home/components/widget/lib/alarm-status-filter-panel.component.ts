///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Inject, InjectionToken } from '@angular/core';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { AlarmSearchStatus, alarmSearchStatusTranslations } from '@shared/models/alarm.models';

export const ALARM_STATUS_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmStatusFilterPanelData');

export interface AlarmStatusFilterPanelData {
  subscription: IWidgetSubscription;
}

@Component({
  selector: 'tb-alarm-status-filter-panel',
  templateUrl: './alarm-status-filter-panel.component.html',
  styleUrls: ['./alarm-status-filter-panel.component.scss']
})
export class AlarmStatusFilterPanelComponent {

  subscription: IWidgetSubscription;

  alarmSearchStatuses = Object.keys(AlarmSearchStatus);
  alarmSearchStatusTranslationMap = alarmSearchStatusTranslations;
  alarmSearchStatusEnum = AlarmSearchStatus;

  constructor(@Inject(ALARM_STATUS_FILTER_PANEL_DATA) public data: AlarmStatusFilterPanelData) {
    this.subscription = this.data.subscription;
  }
}
