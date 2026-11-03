// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
