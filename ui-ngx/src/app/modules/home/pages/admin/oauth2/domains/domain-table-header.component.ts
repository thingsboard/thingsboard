// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, HostBinding } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DomainInfo } from '@shared/models/oauth2.models';

@Component({
    selector: 'tb-domain-table-header',
    templateUrl: './domain-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class DomainTableHeaderComponent extends EntityTableHeaderComponent<DomainInfo> {

  @HostBinding('style.width') width = '100%';

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
