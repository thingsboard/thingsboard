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
import { Resource, ResourceInfo, ResourceSubType, ResourceSubTypeTranslationMap } from '@shared/models/resource.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-js-library-table-header',
  templateUrl: './js-library-table-header.component.html',
  styleUrls: []
})
export class JsLibraryTableHeaderComponent extends EntityTableHeaderComponent<Resource, PageLink, ResourceInfo> {

  readonly jsResourceSubTypes: ResourceSubType[] = [ResourceSubType.EXTENSION, ResourceSubType.MODULE];
  readonly resourceSubTypesTranslationMap = ResourceSubTypeTranslationMap;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  jsResourceSubTypeChanged(resourceSubType: ResourceSubType) {
    this.entitiesTableConfig.componentsData.resourceSubType = resourceSubType;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }
}
