// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { EntityType } from '@shared/models/entity-type.models';
import { AssetInfo } from '@shared/models/asset.models';
import { AssetProfileId } from '@shared/models/id/asset-profile-id';

@Component({
    selector: 'tb-asset-table-header',
    templateUrl: './asset-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class AssetTableHeaderComponent extends EntityTableHeaderComponent<AssetInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  assetProfileChanged(assetProfileId: AssetProfileId) {
    this.entitiesTableConfig.componentsData.assetProfileId = assetProfileId;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

}
