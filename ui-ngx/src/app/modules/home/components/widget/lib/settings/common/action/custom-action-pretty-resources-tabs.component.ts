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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { CustomActionDescriptor } from '@shared/models/widget.models';
import {
  CustomPrettyActionEditorCompleter
} from '@home/components/widget/lib/settings/common/action/custom-action.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-custom-action-pretty-resources-tabs',
    templateUrl: './custom-action-pretty-resources-tabs.component.html',
    standalone: false
})
export class CustomActionPrettyResourcesTabsComponent extends PageComponent {

  @Input()
  action: CustomActionDescriptor;

  @Input()
  @coerceBoolean()
  fullscreen = false;

  @Output()
  actionUpdated: EventEmitter<CustomActionDescriptor> = new EventEmitter<CustomActionDescriptor>();

  customPrettyActionEditorCompleter = CustomPrettyActionEditorCompleter;

  constructor() {
    super();
  }

  public notifyActionUpdated() {
    this.actionUpdated.emit(this.validate() ? this.action : null);
  }

  private validate(): boolean {
    if (this.action.customResources) {
      for (const resource of this.action.customResources) {
        if (!resource.url) {
          return false;
        }
      }
    }
    return true;
  }

  public addResource() {
    if (!this.action.customResources) {
      this.action.customResources = [];
    }
    this.action.customResources.push({url: ''});
    this.notifyActionUpdated();
  }

  public removeResource(index: number) {
    if (index > -1) {
      if (this.action.customResources.splice(index, 1).length > 0) {
        this.notifyActionUpdated();
      }
    }
  }

}
