///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  actionStatusTranslations,
  actionTypeTranslations,
  AuditLog,
  AuditLogMode
} from '@shared/models/audit-log.models';
import { EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AuditLogService } from '@core/http/audit-log.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { TimePageLink } from '@shared/models/page/page-link';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { UserId } from '@shared/models/id/user-id';
import { CustomerId } from '@shared/models/id/customer-id';
import {
  AuditLogDetailsDialogComponent,
  AuditLogDetailsDialogData
} from '@home/components/audit-log/audit-log-details-dialog.component';

export class AuditLogTableConfig extends EntityTableConfig<AuditLog, TimePageLink> {

  constructor(private auditLogService: AuditLogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private auditLogMode: AuditLogMode = AuditLogMode.TENANT,
              public entityId: EntityId = null,
              public userId: UserId = null,
              public customerId: CustomerId = null,
              updateOnInit = true) {
    super();
    this.loadDataOnInit = updateOnInit;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.actionsColumnTitle = 'audit-log.details';
    this.entityTranslations = {
      noEntities: 'audit-log.no-audit-logs-prompt',
      search: 'audit-log.search'
    };
    this.entityResources = {
    } as EntityTypeResource<AuditLog>;

    this.entitiesFetchFunction = pageLink => this.fetchAuditLogs(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<AuditLog>('createdTime', 'audit-log.timestamp', this.datePipe, '150px'));

    if (this.auditLogMode !== AuditLogMode.ENTITY) {
      this.columns.push(
        new EntityTableColumn<AuditLog>('entityType', 'audit-log.entity-type', '20%',
          (entity) => translate.instant(entityTypeTranslations.get(entity.entityId.entityType).type)),
        new EntityTableColumn<AuditLog>('entityName', 'audit-log.entity-name', '20%'),
      );
    }

    if (this.auditLogMode !== AuditLogMode.USER) {
      this.columns.push(
        new EntityTableColumn<AuditLog>('userName', 'audit-log.user', '33%')
      );
    }

    this.columns.push(
      new EntityTableColumn<AuditLog>('actionType', 'audit-log.type', '33%',
        (entity) => translate.instant(actionTypeTranslations.get(entity.actionType))),
      new EntityTableColumn<AuditLog>('actionStatus', 'audit-log.status', '33%',
        (entity) => translate.instant(actionStatusTranslations.get(entity.actionStatus)))
    );

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('audit-log.details'),
        icon: 'more_horiz',
        isEnabled: () => true,
        onAction: ($event, entity) => this.showAuditLogDetails(entity)
      }
    );
  }

  fetchAuditLogs(pageLink: TimePageLink): Observable<PageData<AuditLog>> {
    switch (this.auditLogMode) {
      case AuditLogMode.TENANT:
        return this.auditLogService.getAuditLogs(pageLink);
      case AuditLogMode.ENTITY:
        return this.auditLogService.getAuditLogsByEntityId(this.entityId, pageLink);
      case AuditLogMode.USER:
        return this.auditLogService.getAuditLogsByUserId(this.userId.id, pageLink);
      case AuditLogMode.CUSTOMER:
        return this.auditLogService.getAuditLogsByCustomerId(this.customerId.id, pageLink);
    }
  }

  showAuditLogDetails(entity: AuditLog) {
    this.dialog.open<AuditLogDetailsDialogComponent, AuditLogDetailsDialogData>(AuditLogDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        auditLog: entity
      }
    });
  }

}
