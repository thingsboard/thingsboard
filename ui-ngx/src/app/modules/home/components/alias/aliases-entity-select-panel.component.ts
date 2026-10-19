// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, InjectionToken } from '@angular/core';
import { AliasInfo, IAliasController } from '@core/api/widget-api.models';
import { EntityInfo } from '@shared/models/entity.models';

export const ALIASES_ENTITY_SELECT_PANEL_DATA = new InjectionToken<any>('AliasesEntitySelectPanelData');

export interface AliasesEntitySelectPanelData {
  aliasController: IAliasController;
  entityAliasesInfo: {[aliasId: string]: AliasInfo};
}

@Component({
    selector: 'tb-aliases-entity-select-panel',
    templateUrl: './aliases-entity-select-panel.component.html',
    styleUrls: ['./aliases-entity-select-panel.component.scss'],
    standalone: false
})
export class AliasesEntitySelectPanelComponent {

  entityAliasesInfo: {[aliasId: string]: AliasInfo};

  constructor(@Inject(ALIASES_ENTITY_SELECT_PANEL_DATA) public data: AliasesEntitySelectPanelData) {
    this.entityAliasesInfo = this.data.entityAliasesInfo;
  }

  public currentAliasEntityChanged(aliasId: string, selected: EntityInfo | null) {
    if (selected) {
      this.data.aliasController.updateCurrentAliasEntity(aliasId, selected);
    }
  }
}
