// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EdgeInfo } from '@shared/models/edge.models';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';

@Component({
    selector: 'tb-edge-tabs',
    templateUrl: './edge-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class EdgeTabsComponent extends EntityTabsComponent<EdgeInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
