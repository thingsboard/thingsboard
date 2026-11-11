// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Edge } from '@shared/models/edge.models';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';

@Component({
    selector: 'tb-edge-tabs',
    templateUrl: './edge-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class EdgeTabsComponent extends EntityTabsComponent<Edge> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
