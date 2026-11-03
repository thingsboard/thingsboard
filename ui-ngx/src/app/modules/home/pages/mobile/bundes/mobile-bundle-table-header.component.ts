// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { MobileAppBundleInfo } from '@shared/models/mobile-app.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-mobile-bundle-table-header',
    templateUrl: './mobile-bundle-table-header.component.html',
    styleUrls: ['./mobile-bundle-table-header.component.scss'],
    standalone: false
})
export class MobileBundleTableHeaderComponent extends EntityTableHeaderComponent<MobileAppBundleInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
