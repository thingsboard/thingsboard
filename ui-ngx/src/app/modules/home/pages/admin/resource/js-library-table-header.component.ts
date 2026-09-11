// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Resource, ResourceInfo, ResourceSubType, ResourceSubTypeTranslationMap } from '@shared/models/resource.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
    selector: 'tb-js-library-table-header',
    templateUrl: './js-library-table-header.component.html',
    styleUrls: [],
    standalone: false
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
