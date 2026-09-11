// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityViewInfo } from '@app/shared/models/entity-view.models';

@Component({
    selector: 'tb-entity-view-table-header',
    templateUrl: './entity-view-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class EntityViewTableHeaderComponent extends EntityTableHeaderComponent<EntityViewInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  entityViewTypeChanged(entityViewType: string) {
    this.entitiesTableConfig.componentsData.entityViewType = entityViewType;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

}
