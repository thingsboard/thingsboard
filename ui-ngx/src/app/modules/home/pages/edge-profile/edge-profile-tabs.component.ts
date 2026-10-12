// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { EdgeProfile } from '@shared/models/edge-profile.models';
import { Authority } from '@shared/models/authority.enum';
import { Operation, Resource } from '@shared/models/security.models';

@Component({
  selector: 'tb-edge-profile-tabs',
  templateUrl: './edge-profile-tabs.component.html',
  styleUrls: [],
  standalone: false
})
export class EdgeProfileTabsComponent extends EntityTabsComponent<EdgeProfile> {

  resource = Resource;
  operation = Operation;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  // GET /api/edgeProfile/{id}/assignments is tenant-admin only, but a customer user with EDGE_PROFILE read can reach
  // this page — without this the tab renders for them and only fails once they click it
  isTenantAdmin(): boolean {
    return getCurrentAuthUser(this.store)?.authority === Authority.TENANT_ADMIN;
  }
}
