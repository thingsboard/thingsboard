///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Resource, ResourceInfo, ResourceType, ResourceTypeTranslationMap } from '@shared/models/resource.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
    selector: 'tb-resources-table-header',
    templateUrl: './resources-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class ResourcesTableHeaderComponent extends EntityTableHeaderComponent<Resource, PageLink, ResourceInfo> {

  readonly resourceTypes = [ResourceType.LWM2M_MODEL, ResourceType.PKCS_12, ResourceType.JKS, ResourceType.GENERAL];
  readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  resourceTypeChanged(resourceType: ResourceType) {
    this.entitiesTableConfig.componentsData.resourceType = resourceType;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }
}
