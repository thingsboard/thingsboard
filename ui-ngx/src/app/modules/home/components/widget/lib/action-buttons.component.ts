///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, Input, OnInit } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetActionDescriptor } from '@shared/models/widget.models';
import { PageComponent } from '@shared/components/page.component';
import { ThemePalette } from '@angular/material/core';


interface ActionButtonsWidgetSettings {
  buttonsType: string,
  buttonsClass: ThemePalette,
  alignment: string
}

@Component({
  selector: 'tb-action-buttons-widget',
  templateUrl: './action-buttons.component.html',
  styleUrls: ['./action-buttons.component.scss']
})
export class ActionButtonsComponent  implements OnInit {

  @Input()
  ctx: WidgetContext;

  settings: ActionButtonsWidgetSettings;

  constructor() {
  }

  ngOnInit(): void {
    this.settings = this.ctx.settings;
  }

  actionButtonClick($event: MouseEvent, actionDescriptor: WidgetActionDescriptor) {
    let entityId, entityName, entityLabel;
    if (this.ctx.datasources) {
      entityId = this.ctx.datasources[0].entity.id;
      entityName = this.ctx.datasources[0].entityName;
      entityLabel = this.ctx.datasources[0].entityLabel;
    }
    this.ctx.actionsApi.handleWidgetAction($event, actionDescriptor, entityId, entityName, null,  entityLabel);
  }

}
