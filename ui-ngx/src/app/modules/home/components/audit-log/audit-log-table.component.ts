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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { AuditLogService } from '@core/http/audit-log.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { AuditLogMode } from '@shared/models/audit-log.models';
import { EntityId } from '@shared/models/id/entity-id';
import { UserId } from '@shared/models/id/user-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { AuditLogTableConfig } from '@home/components/audit-log/audit-log-table-config';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-audit-log-table',
  templateUrl: './audit-log-table.component.html',
  styleUrls: ['./audit-log-table.component.scss']
})
export class AuditLogTableComponent implements OnInit {

  @Input()
  auditLogMode: AuditLogMode;

  @Input()
  detailsMode: boolean;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;
  userIdValue: UserId;
  customerIdValue: CustomerId;

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
    if (this.auditLogTableConfig && this.auditLogTableConfig.entityId !== entityId) {
      this.auditLogTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  set userId(userId: UserId) {
    this.userIdValue = userId;
    if (this.auditLogTableConfig && this.auditLogTableConfig.userId !== userId) {
      this.auditLogTableConfig.userId = userId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  set customerId(customerId: CustomerId) {
    this.customerIdValue = customerId;
    if (this.auditLogTableConfig && this.auditLogTableConfig.customerId !== customerId) {
      this.auditLogTableConfig.customerId = customerId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  auditLogTableConfig: AuditLogTableConfig;

  constructor(private auditLogService: AuditLogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private route: ActivatedRoute) {
  }

  ngOnInit() {
    let updateOnInit = this.activeValue;
    this.dirtyValue = !this.activeValue;
    if (!this.auditLogMode) {
      const authUser = getCurrentAuthUser(this.store);
      if (authUser.authority === Authority.TENANT_ADMIN) {
        this.auditLogMode = AuditLogMode.TENANT;
      }
      updateOnInit = true;
    }
    const pageMode = !!this.route.snapshot.data.isPage;
    this.auditLogTableConfig = new AuditLogTableConfig(
      this.auditLogService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.auditLogMode,
      this.entityIdValue,
      this.userIdValue,
      this.customerIdValue,
      updateOnInit,
      pageMode
    );
  }

}
