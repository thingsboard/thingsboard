// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { TenantInfo } from '@shared/models/tenant.model';

@Component({
    selector: 'tb-tenant-tabs',
    templateUrl: './tenant-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class TenantTabsComponent extends EntityTabsComponent<TenantInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
