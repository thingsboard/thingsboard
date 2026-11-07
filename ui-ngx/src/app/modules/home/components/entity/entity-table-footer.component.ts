// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, HasId } from '@shared/models/base-data';
import { PageComponent } from '@shared/components/page.component';
import { Directive, Input } from '@angular/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';

/**
 * Base class for a component rendered under the table, above the paginator. Counterpart of
 * {@link EntityTableHeaderComponent}: set `footerComponent` on the table config to use it.
 */
@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class EntityTableFooterComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends PageComponent {

  entitiesTableConfigValue: C;

  @Input()
  set entitiesTableConfig(entitiesTableConfig: C) {
    this.setEntitiesTableConfig(entitiesTableConfig);
  }

  get entitiesTableConfig(): C {
    return this.entitiesTableConfigValue;
  }

  protected constructor(...args: unknown[]) {
    super();
  }

  protected setEntitiesTableConfig(entitiesTableConfig: C) {
    this.entitiesTableConfigValue = entitiesTableConfig;
  }
}
