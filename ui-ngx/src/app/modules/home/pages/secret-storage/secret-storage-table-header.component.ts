// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { SecretStorage } from '@shared/models/secret-storage.models';

@Component({
    selector: 'tb-secret-storage-table-header',
    templateUrl: './secret-storage-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class SecretStorageTableHeaderComponent extends EntityTableHeaderComponent<SecretStorage> {

  @HostBinding('style.width') width = '100%';

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
