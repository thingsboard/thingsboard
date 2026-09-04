// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { AlarmInfo, AlarmSearchStatus, alarmSearchStatusTranslations } from '@shared/models/alarm.models';
import { AlarmTableConfig } from './alarm-table-config';
import { AlarmFilterConfig } from '@shared/models/query/query.models';

@Component({
    selector: 'tb-alarm-table-header',
    templateUrl: './alarm-table-header.component.html',
    styleUrls: ['./alarm-table-header.component.scss'],
    standalone: false
})
export class AlarmTableHeaderComponent extends EntityTableHeaderComponent<AlarmInfo> {

  get alarmTableConfig(): AlarmTableConfig {
    return this.entitiesTableConfig as AlarmTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  alarmFilterChanged(alarmFilterConfig: AlarmFilterConfig) {
    this.alarmTableConfig.alarmFilterConfig = alarmFilterConfig;
    this.alarmTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
