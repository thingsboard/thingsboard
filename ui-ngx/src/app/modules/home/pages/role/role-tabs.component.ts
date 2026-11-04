// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { Role } from '@shared/models/role.models';

@Component({
    selector: 'tb-role-tabs',
    templateUrl: './role-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class RoleTabsComponent extends EntityTabsComponent<Role> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
