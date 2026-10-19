// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { Resource } from '@shared/models/resource.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { NULL_UUID } from '@shared/models/id/has-uuid';

@Component({
    selector: 'tb-resource-library-tabs',
    templateUrl: './resource-library-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class ResourceLibraryTabsComponent extends EntityTabsComponent<Resource> {

  readonly NULL_UUID = NULL_UUID;

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
