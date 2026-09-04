// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { EntityViewInfo } from '@app/shared/models/entity-view.models';

@Component({
    selector: 'tb-entity-view-tabs',
    templateUrl: './entity-view-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class EntityViewTabsComponent extends EntityTabsComponent<EntityViewInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
