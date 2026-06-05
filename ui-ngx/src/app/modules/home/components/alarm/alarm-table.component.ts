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

import { ChangeDetectorRef, Component, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DialogService } from '@core/services/dialog.service';
import { AlarmTableConfig } from './alarm-table-config';
import { AlarmSearchStatus, AlarmSeverity, AlarmsMode } from '@shared/models/alarm.models';
import { AlarmService } from '@app/core/http/alarm.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { ActivatedRoute, Router } from '@angular/router';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { EntityService } from '@core/http/entity.service';
import { PageQueryParam } from '@shared/models/page/page-link';
import { AlarmFilterConfig } from '@shared/models/query/query.models';

interface AlarmPageQueryParams extends PageQueryParam {
  typeList?: Array<string>;
  statusList?: Array<AlarmSearchStatus>;
  severityList?: Array<AlarmSeverity>;
  assignedToMe?: boolean;
}

@Component({
    selector: 'tb-alarm-table',
    templateUrl: './alarm-table.component.html',
    styleUrls: ['./alarm-table.component.scss'],
    standalone: false
})
export class AlarmTableComponent implements OnInit {

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;
  alarmsMode = AlarmsMode.ENTITY;
  detailsMode = true;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    this.entityIdValue = entityId;
    if (this.alarmTableConfig && this.alarmTableConfig.entityId !== entityId) {
      this.alarmTableConfig.alarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]};
      this.alarmTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  alarmTableConfig: AlarmTableConfig;

  constructor(private alarmService: AlarmService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private utilsService: UtilsService,
              private route: ActivatedRoute,
              private router: Router) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    const pageMode = !!this.route.snapshot.data.isPage;
    if (pageMode) {
      this.detailsMode = false;
    }
    if (isDefinedAndNotNull(this.route.snapshot.data.alarmsMode)) {
      this.alarmsMode = this.route.snapshot.data.alarmsMode;
    }
    const defaultAlarmFilterConfig: AlarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]};
    if (pageMode) {
      const routerQueryParams: AlarmPageQueryParams = this.route.snapshot.queryParams;
      if (routerQueryParams) {
        const queryParams = deepClone(routerQueryParams);
        let replaceUrl = false;
        if (routerQueryParams?.typeList) {
          defaultAlarmFilterConfig.typeList = routerQueryParams?.typeList;
          delete queryParams.typeList;
          replaceUrl = true;
        }
        if (routerQueryParams?.statusList) {
          defaultAlarmFilterConfig.statusList = routerQueryParams?.statusList;
          delete queryParams.statusList;
          replaceUrl = true;
        }
        if (routerQueryParams?.severityList) {
          defaultAlarmFilterConfig.severityList = routerQueryParams?.severityList;
          delete queryParams.severityList;
          replaceUrl = true;
        }
        if (routerQueryParams?.assignedToMe) {
          defaultAlarmFilterConfig.assignedToCurrentUser = routerQueryParams?.assignedToMe;
          delete queryParams.assignedToMe;
          replaceUrl = true;
        }
        if (replaceUrl) {
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams,
            queryParamsHandling: '',
            replaceUrl: true
          });
        }
      }
    }
    this.alarmTableConfig = new AlarmTableConfig(
      this.alarmService,
      this.entityService,
      this.dialogService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.alarmsMode,
      this.entityIdValue,
      defaultAlarmFilterConfig,
      this.store,
      this.viewContainerRef,
      this.overlay,
      this.cd,
      this.utilsService,
      pageMode
    );
  }

}
