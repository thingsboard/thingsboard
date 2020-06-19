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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { AlarmInfo, AlarmSearchStatus, alarmSearchStatusTranslations } from '@shared/models/alarm.models';
import { AlarmTableConfig } from './alarm-table-config';

@Component({
  selector: 'tb-alarm-table-header',
  templateUrl: './alarm-table-header.component.html',
  styleUrls: ['./alarm-table-header.component.scss']
})
export class AlarmTableHeaderComponent extends EntityTableHeaderComponent<AlarmInfo> {

  alarmSearchStatusTranslationsMap = alarmSearchStatusTranslations;

  alarmSearchStatusTypes = Object.keys(AlarmSearchStatus);
  alarmSearchStatusEnum = AlarmSearchStatus;

  get alarmTableConfig(): AlarmTableConfig {
    return this.entitiesTableConfig as AlarmTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  searchStatusChanged(searchStatus: AlarmSearchStatus) {
    this.alarmTableConfig.searchStatus = searchStatus;
    this.alarmTableConfig.table.resetSortAndFilter(true, true);
  }
}
