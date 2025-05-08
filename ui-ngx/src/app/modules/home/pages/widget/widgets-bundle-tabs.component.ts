///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { NULL_UUID } from '@shared/models/id/has-uuid';

@Component({
  selector: 'tb-widgets-bundle-tabs',
  templateUrl: './widgets-bundle-tabs.component.html',
  styleUrls: []
})
export class WidgetsBundleTabsComponent extends EntityTabsComponent<WidgetsBundle> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  isTenantWidgetsBundle() {
    return this.entity && this.entity.tenantId.id !== NULL_UUID;
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
