// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive, OnInit } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { BaseData, HasId } from '@shared/models/base-data';
import { PageLink } from '@shared/models/page/page-link';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class IncludeCustomersTableHeaderComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends EntityTableHeaderComponent<T, P, L, C> implements OnInit {

  includeCustomersLabel: string;

  ngOnInit() {
    super.ngOnInit();
    this.includeCustomersLabel = getCurrentAuthUser(this.store).authority === Authority.CUSTOMER_USER
      ? 'entity.include-sub-customer-entities' : 'entity.include-customer-entities';
  }

  includeCustomersChanged(includeCustomers: boolean) {
    this.entitiesTableConfig.componentsData.includeCustomersChanged(includeCustomers);
  }
}
