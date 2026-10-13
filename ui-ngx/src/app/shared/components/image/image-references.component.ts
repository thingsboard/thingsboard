// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { forkJoin, Observable, of } from 'rxjs';
import { EntityService } from '@core/http/entity.service';
import { BaseData, HasId } from '@shared/models/base-data';
import { map } from 'rxjs/operators';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { pageByWhiteLabelingType, WhiteLabeling, WhiteLabelingType } from '@shared/models/white-labeling.models';
import { EntityId } from '@shared/models/id/entity-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@app/shared/models/id/customer-id';

interface ReferencedEntityInfo {
  entity: BaseData<HasId> | WhiteLabeling;
  typeName: string;
  detailsUrl: string;
  queryParams?: {[key: string]: string};
  isWl: boolean;
}

// system, tenant or customer
interface HolderReferencedEntities {
  name?: string;
  detailsUrl?: string;
  currentHolder: boolean;
  entities: ReferencedEntityInfo[];
}

type ReferencedEntities = {[type: string]: {[id: string]: HolderReferencedEntities}};
type ReferencedEntitiesEntry = [EntityId, HolderReferencedEntities];

@Component({
    selector: 'tb-image-references',
    templateUrl: './image-references.component.html',
    styleUrls: ['./image-references.component.scss'],
    standalone: false
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
    if (this.hasNotSameAuthLevelEntities(this.references)) {
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

  isSystem(id: EntityId): boolean {
    return id.entityType === EntityType.TENANT && id.id === NULL_UUID;
  }

  holderName(id: EntityId): string {
    return this.translate.instant(id.entityType === EntityType.TENANT ? 'tenant.tenant' : 'customer.customer');
  }

  private hasNotSameAuthLevelEntities(references: ResourceReferences): boolean {
    const authority = this.authUser.authority;
    if (authority === Authority.SYS_ADMIN &&
      references.some(e => e.tenantId && e.tenantId.id && e.tenantId.id !== NULL_UUID)) {
      return true;
    }
    if (authority === Authority.TENANT_ADMIN &&
      references.some(e => e.customerId && e.customerId.id && e.customerId.id !== NULL_UUID)) {
      return true;
    }
    if (authority === Authority.CUSTOMER_USER &&
      references.some(e => this.authUser.customerId !== e.customerId?.id)) {
      return true;
    }
    return false;
  }
  private getAdminSettingsPageURL(entity: BaseData<EntityId>) {
    let url = '/settings/';
    switch (entity.name) {
      case 'mail':
        return url + 'outgoing-mail';
      case 'entitiesVersionControl':
        return url + 'repository';
      case 'sms':
      case 'notifications':
        return url + 'notifications';
      default:
        return url + 'general';
    }
  }

  getAdminSettingsName(entity: BaseData<EntityId>) {
    if (entity.id.entityType === EntityType.ADMIN_SETTINGS) {
      let name: string;
      switch (entity.name) {
        case 'mail':
          name = 'admin.outgoing-mail-settings';
          break;
        case 'entitiesVersionControl':
          name = 'admin.repository-settings';
          break;
        case 'sms':
        case 'notifications':
          name = 'admin.notifications-settings';
          break;
        default:
          name = 'admin.general-settings';
      }
      return this.translate.instant(name);
    }
    return entity.name;
  }

  private toReferencedEntitiesList(references: ResourceReferences): ReferencedEntityInfo[] {
    const result: ReferencedEntityInfo[] = [];
    for (const reference of references) {
      if ((reference as BaseData<HasId>).id) {
        const entity = reference as BaseData<EntityId>;
        const entityType = entity.id.entityType as EntityType;
        const entityTypeName = this.translate.instant(entityTypeTranslations.get(entityType).type);
        let detailsUrl: string;
        let queryParams: {[key: string]: string};
        switch (entityType) {
          case EntityType.ADMIN_SETTINGS:
            detailsUrl = this.getAdminSettingsPageURL(entity);
            break;
          case EntityType.AI_MODEL:
            detailsUrl = '/settings/ai-models';
            queryParams = {textSearch: encodeURI(entity.name)}
            break;
          default:
            detailsUrl = getEntityDetailsPageURL(entity.id.id, entityType);
        }
        result.push({
          entity,
          typeName: entityTypeName,
          detailsUrl,
          queryParams,
          isWl: false
        });
      } else {
        const whiteLabeling = reference as WhiteLabeling;
        const typeName = this.translate.instant(whiteLabeling.type === WhiteLabelingType.GENERAL
          ? 'white-labeling.white-labeling' : 'white-labeling.login-white-labeling');
        const detailsUrl = pageByWhiteLabelingType.get(whiteLabeling.type);
        result.push({
          entity: whiteLabeling,
          typeName,
          detailsUrl,
          isWl: true
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
      const customerId = referencedEntityInfo.entity.customerId?.id || NULL_UUID;
      let type = 'system';
      let id = NULL_UUID;
      let currentHolder = this.authUser.authority === Authority.SYS_ADMIN;
      if (tenantId !== NULL_UUID) {
        type = 'tenant';
        id = tenantId;
        currentHolder = id === this.authUser.tenantId && this.authUser.authority === Authority.TENANT_ADMIN;
        if (customerId !== NULL_UUID) {
          type = 'customer';
          id = customerId;
          currentHolder = id === this.authUser.customerId && this.authUser.authority === Authority.CUSTOMER_USER;
          if (id !== this.authUser.customerId) {
            type = 'subCustomer';
          }
        }
      }
      let holderEntitiesByType = referencedEntities[type];
      if (!holderEntitiesByType) {
        holderEntitiesByType = {};
        referencedEntities[type] = holderEntitiesByType;
      }
      let holderEntitiesInfo = holderEntitiesByType[id];
      if (!holderEntitiesInfo) {
        holderEntitiesInfo = {
          entities: [],
          currentHolder
        };
        holderEntitiesByType[id] = holderEntitiesInfo;
      }
      holderEntitiesInfo.entities.push(referencedEntityInfo);
    }
    referencedEntities = Object.keys(referencedEntities).sort((type1, type2) => {
      if (type1 === type2) {
        return 0;
      } else if (type1 === 'system') {
        return -1;
      } else if (type1 === 'tenant') {
        if (type2 === 'system') {
          return 1;
        } else {
          return -1;
        }
      } else if (type1 === 'customer') {
        if (type2 === 'system' || type2 === 'tenant') {
          return 1;
        } else {
          return -1;
        }
      } else {
        return 1;
      }
    }).reduce(
      (obj, key) => {
        obj[key] = referencedEntities[key];
        return obj;
      },
      {}
    );
    const tasks: Observable<any>[] = [];
    for (const type of Object.keys(referencedEntities)) {
      if (type === 'tenant') {
        if (this.authUser.authority === Authority.SYS_ADMIN) {
          const tenantEntities = referencedEntities[type];
          const tenantIds = Object.keys(tenantEntities);
          tasks.push(this.entityService.getEntities(EntityType.TENANT, tenantIds).pipe(
            map((tenants) => {
              for (const tenant of tenants) {
                const holderEntitiesInfo = tenantEntities[tenant.id.id];
                holderEntitiesInfo.name = tenant.name;
                holderEntitiesInfo.detailsUrl = getEntityDetailsPageURL(tenant.id.id, EntityType.TENANT);
              }
            })
          ));
        }
      } else if (type === 'customer' || type === 'subCustomer') {
        if (this.authUser.authority === Authority.TENANT_ADMIN || this.authUser.authority === Authority.CUSTOMER_USER) {
          const customerEntities = referencedEntities[type];
          const customerIds = Object.keys(customerEntities);
          tasks.push(this.entityService.getEntities(EntityType.CUSTOMER, customerIds).pipe(
            map((customers) => {
              for (const customer of customers) {
                const holderEntitiesInfo = customerEntities[customer.id.id];
                holderEntitiesInfo.name = customer.name;
                holderEntitiesInfo.detailsUrl = getEntityDetailsPageURL(customer.id.id, EntityType.CUSTOMER);
              }
            })
          ));
        }
      }
    }
    return (tasks.length ? forkJoin(tasks) : of(null)).pipe(
      map(() => {
        const result: ReferencedEntitiesEntry[] = [];
        for (const type of Object.keys(referencedEntities)) {
          const entries = Object.entries(referencedEntities[type]);
          for (const entry of entries) {
            let entityId: EntityId;
            if (type === 'system' || type === 'tenant') {
              entityId = new TenantId(entry[0]);
            } else {
              entityId = new CustomerId(entry[0]);
            }
            result.push([entityId, entry[1]]);
          }
        }
        return result;
      })
    );
  }

}
