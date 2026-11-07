// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Asset, AssetInfo } from '@shared/models/asset.models';
import { AssetProfileId } from '@shared/models/id/asset-profile-id';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { map } from 'rxjs/operators';

@Component({
    selector: 'tb-asset-table-header',
    templateUrl: './asset-table-header.component.html',
    styleUrls: ['asset-table-header.component.scss'],
    standalone: false
})
export class AssetTableHeaderComponent extends EntityTableHeaderComponent<AssetInfo | Asset> implements OnInit {

  entityType = EntityType;

  includeCustomersLabel: string;

  private assetProfileService = inject(AssetProfileService);

  readonly assetProfileLabel = (assetProfileId: AssetProfileId) =>
    this.assetProfileService.getAssetProfileInfo(assetProfileId.id, {ignoreLoading: true, ignoreErrors: true})
      .pipe(map(assetProfileInfo => assetProfileInfo.name));

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    this.includeCustomersLabel = (getCurrentAuthUser(this.store).authority === Authority.CUSTOMER_USER ||
      this.entitiesTableConfig.customerId) ? 'entity.include-sub-customer-entities' : 'entity.include-customer-entities';
  }

  assetProfileChanged(assetProfileId: AssetProfileId) {
    this.entitiesTableConfig.componentsData.assetProfileId = assetProfileId;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

  includeCustomersChanged(includeCustomers: boolean) {
    this.entitiesTableConfig.componentsData.includeCustomersChanged(includeCustomers);
  }

}
