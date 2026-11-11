// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { PageLink } from '@shared/models/page/page-link';
import { ShortEntityView } from '@shared/models/entity-group.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-group-entity-table-header',
    templateUrl: './group-entity-table-header.component.html',
    styleUrls: ['./group-entity-table-header.component.scss'],
    standalone: false
})
export class GroupEntityTableHeaderComponent<T extends BaseData<HasId>>
  extends EntityTableHeaderComponent<T, PageLink, ShortEntityView, GroupEntityTableConfig<T>> {

  tableTitle: string;
  entitiesTitle: string;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private translate: TranslateService) {
    super(store);
  }

  protected setEntitiesTableConfig(entitiesTableConfig: GroupEntityTableConfig<T>) {
    super.setEntitiesTableConfig(entitiesTableConfig);
    const settings = entitiesTableConfig.settings;
    const entityGroup = entitiesTableConfig.entityGroup;
    const entityType = entityGroup.type;
    if (settings.groupTableTitle && settings.groupTableTitle.length) {
      this.tableTitle = settings.groupTableTitle;
      this.entitiesTitle = '';
    } else {
      this.tableTitle = this.utils.customTranslation(entityGroup.name, entityGroup.name);
      this.entitiesTitle = `: ${this.translate.instant(entityTypeTranslations.get(entityType).typePlural)}`;
    }
  }

  toggleGroupDetails() {
    this.entitiesTableConfig.onToggleEntityGroupDetails();
  }

}
