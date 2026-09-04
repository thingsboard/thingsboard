// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, HostBinding } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { OAuth2Client, OAuth2ClientInfo } from '@shared/models/oauth2.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
    selector: 'tb-client-table-header',
    templateUrl: './client-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class ClientTableHeaderComponent extends EntityTableHeaderComponent<OAuth2Client, PageLink, OAuth2ClientInfo> {

  @HostBinding('style.width') width = '100%';

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
