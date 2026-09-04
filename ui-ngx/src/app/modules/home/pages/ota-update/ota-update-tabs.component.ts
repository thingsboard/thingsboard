// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { OtaPackage } from '@shared/models/ota-package.models';

@Component({
    selector: 'tb-ota-update-tabs',
    templateUrl: './ota-update-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class OtaUpdateTabsComponent extends EntityTabsComponent<OtaPackage> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  isTenantOtaUpdate() {
    return this.entity && this.entity.tenantId.id !== NULL_UUID;
  }

}
