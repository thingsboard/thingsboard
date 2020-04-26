///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { BaseData, HasId } from '@shared/models/base-data';
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, ContentChildren, EventEmitter, Input, OnInit, Output, QueryList, ViewChildren, Directive } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatTab } from '@angular/material/tabs';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { BehaviorSubject } from 'rxjs';
import { Authority } from '@app/shared/models/authority.enum';
import { selectAuthUser, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { EntityType } from '@shared/models/entity-type.models';
import { AuditLogMode } from '@shared/models/audit-log.models';
import { DebugEventType, EventType } from '@shared/models/event.models';
import { AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { NgForm } from '@angular/forms';
import { PageLink } from '@shared/models/page/page-link';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class EntityTabsComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends PageComponent implements OnInit, AfterViewInit {

  attributeScopes = AttributeScope;
  latestTelemetryTypes = LatestTelemetry;

  authorities = Authority;

  entityTypes = EntityType;

  auditLogModes = AuditLogMode;

  eventTypes = EventType;

  debugEventTypes = DebugEventType;

  authUser: AuthUser;

  nullUid = NULL_UUID;

  entityValue: T;

  entitiesTableConfigValue: C;

  @ViewChildren(MatTab) entityTabs: QueryList<MatTab>;

  isEditValue: boolean;

  @Input()
  set isEdit(isEdit: boolean) {
    this.isEditValue = isEdit;
  }

  get isEdit() {
    return this.isEditValue;
  }

  @Input()
  set entity(entity: T) {
    this.setEntity(entity);
  }

  get entity(): T {
    return this.entityValue;
  }

  @Input()
  set entitiesTableConfig(entitiesTableConfig: C) {
    this.setEntitiesTableConfig(entitiesTableConfig);
  }

  get entitiesTableConfig(): C {
    return this.entitiesTableConfigValue;
  }

  @Input()
  detailsForm: NgForm;

  private entityTabsSubject = new BehaviorSubject<Array<MatTab>>(null);

  entityTabsChanged = this.entityTabsSubject.asObservable();

  protected constructor(protected store: Store<AppState>) {
    super(store);
    this.authUser = getCurrentAuthUser(store);
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    this.entityTabsSubject.next(this.entityTabs.toArray());
    this.entityTabs.changes.subscribe(
      () => {
        this.entityTabsSubject.next(this.entityTabs.toArray());
      }
    );
  }

  protected setEntity(entity: T) {
    this.entityValue = entity;
  }

  protected setEntitiesTableConfig(entitiesTableConfig: C) {
    this.entitiesTableConfigValue = entitiesTableConfig;
  }

}
