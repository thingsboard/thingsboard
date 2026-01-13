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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ResourceReferences } from '@shared/models/resource.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { getEntityDetailsPageURL } from '@core/utils';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Authority } from '@shared/models/authority.enum';
import { Observable } from 'rxjs';
import { EntityService } from '@core/http/entity.service';
import { BaseData, HasId } from '@shared/models/base-data';
import { HasTenantId } from '@shared/models/entity.models';
import { map } from 'rxjs/operators';
import { TbPopoverComponent } from '@shared/components/popover.component';

interface ReferencedEntityInfo {
  entity: BaseData<HasId> & HasTenantId;
  typeName: string;
  detailsUrl: string;
}

interface TenantReferencedEntities {
  tenantName?: string;
  tenantDetailsUrl?: string;
  entities: ReferencedEntityInfo[];
}

type ReferencedEntities = {[tenantId: string]: TenantReferencedEntities};
type ReferencedEntitiesEntry = [string, TenantReferencedEntities];

@Component({
  selector: 'tb-image-references',
  templateUrl: './image-references.component.html',
  styleUrls: ['./image-references.component.scss']
})
export class ImageReferencesComponent implements OnInit {

  @Input()
  references: ResourceReferences;

  popoverComponent: TbPopoverComponent<ImageReferencesComponent>;

  contentReady = false;

  authUser = getCurrentAuthUser(this.store);

  simpleList = true;

  referencedEntitiesList: ReferencedEntityInfo[];

  referencedEntitiesEntries: ReferencedEntitiesEntry[];

  constructor(protected store: Store<AppState>,
              private entityService: EntityService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    if (this.authUser.authority === Authority.SYS_ADMIN && this.hasNonSystemEntities(this.references)) {
      this.simpleList = false;
      this.toReferencedEntitiesEntries(this.references).subscribe(
        (entries) => {
          this.referencedEntitiesEntries = entries;
          this.contentReady = true;
          this.cd.detectChanges();
          if (this.popoverComponent) {
            Promise.resolve().then(() => {
              this.popoverComponent.updatePosition();
            });
          }
        }
      );
    } else {
      this.referencedEntitiesList = this.toReferencedEntitiesList(this.references);
      this.contentReady = true;
    }
  }

  isSystem(tenantId: string): boolean {
    return tenantId === NULL_UUID;
  }

  private hasNonSystemEntities(references: ResourceReferences): boolean {
    for (const entityTypeStr of Object.keys(references)) {
      const entities = this.references[entityTypeStr];
      if (entities.some(e => e.tenantId && e.tenantId.id && e.tenantId.id !== NULL_UUID)) {
        return true;
      }
    }
    return false;
  }

  private toReferencedEntitiesList(references: ResourceReferences): ReferencedEntityInfo[] {
    const result: ReferencedEntityInfo[] = [];
    for (const entityTypeStr of Object.keys(references)) {
      const entityType = entityTypeStr as EntityType;
      const entityTypeName = this.translate.instant(entityTypeTranslations.get(entityType).type);
      const entities = references[entityTypeStr];
      for (const entity of entities) {
        const detailsUrl = getEntityDetailsPageURL(entity.id.id, entityType);
        result.push({
          entity,
          typeName: entityTypeName,
          detailsUrl
        });
      }
    }
    return result;
  }

  private toReferencedEntitiesEntries(references: ResourceReferences): Observable<ReferencedEntitiesEntry[]> {
    let referencedEntities: ReferencedEntities = {};
    const referencedEntitiesList = this.toReferencedEntitiesList(references);
    for (const referencedEntityInfo of referencedEntitiesList) {
      const tenantId = referencedEntityInfo.entity.tenantId?.id || NULL_UUID;
      let tenantEntitiesInfo = referencedEntities[tenantId];
      if (!tenantEntitiesInfo) {
        tenantEntitiesInfo = {
          entities: []
        };
        referencedEntities[tenantId] = tenantEntitiesInfo;
      }
      tenantEntitiesInfo.entities.push(referencedEntityInfo);
    }
    referencedEntities = Object.keys(referencedEntities).sort((tenantId1, tenantId2) => {
      if (tenantId1 === NULL_UUID) {
        return -1;
      } else if (tenantId2 === NULL_UUID) {
        return 1;
      }
      return 0;
    }).reduce(
      (obj, key) => {
        obj[key] = referencedEntities[key];
        return obj;
      },
      {}
    );
    const tenantIds = Object.keys(referencedEntities).filter(id => id !== NULL_UUID);
    return this.entityService.getEntities(EntityType.TENANT, tenantIds).pipe(
      map((tenants) => {
        for (const tenant of tenants) {
          const tenantEntitiesInfo = referencedEntities[tenant.id.id];
          tenantEntitiesInfo.tenantName = tenant.name;
          tenantEntitiesInfo.tenantDetailsUrl = getEntityDetailsPageURL(tenant.id.id, EntityType.TENANT);
        }
        return Object.entries(referencedEntities);
      })
    );
  }

}
