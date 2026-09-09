// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { EdgeEvent } from '@shared/models/edge.models';
import { EdgeDownlinkTableConfig } from '@home/components/edge/edge-downlink-table-config';

@Component({
    selector: 'tb-edge-downlink-table-header',
    templateUrl: './edge-downlink-table-header.component.html',
    styleUrls: ['./edge-downlink-table-header.component.scss'],
    standalone: false
})
export class EdgeDownlinkTableHeaderComponent extends EntityTableHeaderComponent<EdgeEvent> {

  get eventTableConfig(): EdgeDownlinkTableConfig {
    return this.entitiesTableConfig as EdgeDownlinkTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
