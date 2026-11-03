// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
