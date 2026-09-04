// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { User } from '@app/shared/models/user.model';

@Component({
    selector: 'tb-user-tabs',
    templateUrl: './user-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class UserTabsComponent extends EntityTabsComponent<User> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
