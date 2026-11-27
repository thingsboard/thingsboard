// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
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

  private translate = inject(TranslateService);

  readonly resourceTypeLabel = (resourceType: ResourceType): string =>
    this.translate.instant(this.resourceTypesTranslationMap.get(resourceType) ?? resourceType);

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  resourceTypeChanged(resourceType: ResourceType) {
    this.entitiesTableConfig.componentsData.resourceType = resourceType;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }
}
