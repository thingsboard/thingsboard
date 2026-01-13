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
  styleUrls: ['./aliases-entity-select-panel.component.scss']
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
