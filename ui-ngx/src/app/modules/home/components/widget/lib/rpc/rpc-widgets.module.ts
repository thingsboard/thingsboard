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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { LedIndicatorComponent } from '@home/components/widget/lib/rpc/led-indicator.component';
import { RoundSwitchComponent } from '@home/components/widget/lib/rpc/round-switch.component';
import { SwitchComponent } from '@home/components/widget/lib/rpc/switch.component';
import { KnobComponent } from '@home/components/widget/lib/rpc/knob.component';
import { PersistentTableComponent } from '@home/components/widget/lib/rpc/persistent-table.component';
import { PersistentDetailsDialogComponent } from '@home/components/widget/lib/rpc/persistent-details-dialog.component';
import { PersistentFilterPanelComponent } from '@home/components/widget/lib/rpc/persistent-filter-panel.component';
import { PersistentAddDialogComponent } from '@home/components/widget/lib/rpc/persistent-add-dialog.component';

@NgModule({
  declarations:
    [
      LedIndicatorComponent,
      RoundSwitchComponent,
      SwitchComponent,
      KnobComponent,
      PersistentTableComponent,
      PersistentDetailsDialogComponent,
      PersistentAddDialogComponent,
      PersistentFilterPanelComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    LedIndicatorComponent,
    RoundSwitchComponent,
    SwitchComponent,
    KnobComponent,
    PersistentTableComponent,
    PersistentDetailsDialogComponent,
    PersistentAddDialogComponent
  ]
})
export class RpcWidgetsModule { }
