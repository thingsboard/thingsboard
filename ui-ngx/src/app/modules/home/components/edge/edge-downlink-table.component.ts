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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EdgeDownlinkTableConfig } from './edge-downlink-table-config';
import { DialogService } from '@core/services/dialog.service';
import { AttributeService } from '@core/http/attribute.service';
import { EdgeService } from '@core/http/edge.service';
import { EntityService } from "@core/http/entity.service";

@Component({
  selector: 'tb-edge-downlink-table',
  templateUrl: './edge-downlink-table.component.html',
  styleUrls: ['./edge-downlink-table.component.scss']
})
export class EdgeDownlinkTableComponent implements OnInit {

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

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
    if (this.edgeDownlinkTableConfig && this.edgeDownlinkTableConfig.entityId !== entityId) {
      this.edgeDownlinkTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  edgeDownlinkTableConfig: EdgeDownlinkTableConfig;

  constructor(private edgeService: EdgeService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.edgeDownlinkTableConfig = new EdgeDownlinkTableConfig(
      this.edgeService,
      this.entityService,
      this.dialogService,
      this.translate,
      this.attributeService,
      this.datePipe,
      this.dialog,
      this.entityIdValue
    );
  }

}
