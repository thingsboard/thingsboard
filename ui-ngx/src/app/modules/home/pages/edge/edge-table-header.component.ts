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
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EdgeInfo } from '@shared/models/edge.models';

@Component({
    selector: 'tb-edge-table-header',
    templateUrl: './edge-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class EdgeTableHeaderComponent extends EntityTableHeaderComponent<EdgeInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  edgeTypeChanged(edgeType: string) {
    this.entitiesTableConfig.componentsData.edgeType = edgeType;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

}
