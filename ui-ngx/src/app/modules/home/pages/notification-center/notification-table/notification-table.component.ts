///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { NotificationService } from '@core/http/notification.service';
import { TargetsTableConfig } from '@home/pages/notification-center/notification-table/targets-table-config';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { InboxTableConfig } from '@home/pages/notification-center/notification-table/inbox-table-config';
import { DatePipe } from '@angular/common';
import { TemplateTableConfig } from '@home/pages/notification-center/notification-table/template-table-config';
import { RequestTableConfig } from '@home/pages/notification-center/notification-table/request-table-config';

@Component({
  selector: 'tb-notification-table',
  templateUrl: './notification-table.component.html'
})
export class NotificationTableComponent implements OnInit {

  @Input()
  notificationType = EntityType.NOTIFICATION;

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  entityTableConfig: EntityTableConfig<any>;

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.entityTableConfig = this.getTableConfig();
    this.entityTableConfig.pageMode = false;
    this.entityTableConfig.detailsPanelEnabled = false;
    this.entityTableConfig.selectionEnabled = false;
    this.entityTableConfig.addEnabled = false;
  }

  updateData() {
    this.entitiesTable.updateData();
  }

  private getTableConfig(): EntityTableConfig<any> {
    switch (this.notificationType) {
      case EntityType.NOTIFICATION_TARGET:
        return new TargetsTableConfig(
          this.notificationService,
          this.translate,
          this.dialog
        );
      case EntityType.NOTIFICATION:
        return new InboxTableConfig(
          this.notificationService,
          this.translate,
          this.datePipe
        );
      case EntityType.NOTIFICATION_TEMPLATE:
        return new TemplateTableConfig(
          this.notificationService,
          this.translate,
          this.dialog
        );
      case EntityType.NOTIFICATION_REQUEST:
        return new RequestTableConfig(
          this.notificationService,
          this.translate,
          this.dialog,
          this.datePipe
        );
    }
  }
}
