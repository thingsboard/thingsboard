///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  Input,
  OnInit,
  ViewChild,
} from '@angular/core';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CalculatedFieldsTableConfig } from '@home/components/calculated-fields/calculated-fields-table-config';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';

@Component({
  selector: 'tb-calculated-fields-table',
  templateUrl: './calculated-fields-table.component.html',
  styleUrls: ['./calculated-fields-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalculatedFieldsTableComponent implements OnInit {

  @Input()
  set entityId(entityId: EntityId) {
    if (this.entityIdValue !== entityId) {
      this.entityIdValue = entityId;
      if (!this.activeValue) {
        this.hasInitialized = true;
      }
    }
  }

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.hasInitialized) {
        this.hasInitialized = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  calculatedFieldsTableConfig: CalculatedFieldsTableConfig;

  private activeValue = false;
  private hasInitialized = false;
  private entityIdValue: EntityId;

  constructor(private calculatedFieldsService: CalculatedFieldsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private durationLeft: DurationLeftPipe,
              private popoverService: TbPopoverService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.hasInitialized = !this.activeValue;

    this.calculatedFieldsTableConfig = new CalculatedFieldsTableConfig(
      this.calculatedFieldsService,
      this.translate,
      this.dialog,
      this.entityIdValue,
      this.store,
      this.durationLeft,
      this.popoverService,
      this.destroyRef,
    );
  }
}
